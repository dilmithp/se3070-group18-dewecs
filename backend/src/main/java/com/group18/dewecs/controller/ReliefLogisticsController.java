package com.group18.dewecs.controller;

import com.group18.dewecs.domain.ResourceType;
import com.group18.dewecs.dto.DispatchFormRequest;
import com.group18.dewecs.dto.LogisticsViews.CommandCenterView;
import com.group18.dewecs.dto.NeedFormRequest;
import com.group18.dewecs.exception.ReliefValidationException;
import com.group18.dewecs.mapper.HazardEventMapper;
import com.group18.dewecs.service.ReliefLogisticsService;
import com.group18.dewecs.service.ReliefLogisticsService.DispatchOutcome;
import com.group18.dewecs.service.ReliefLogisticsService.DispatchRequest;
import com.group18.dewecs.service.ReliefLogisticsService.HandoverRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.stream.Collectors;

/** The relief command center (UC-02): inventory, shelter needs and dispatch, plus handover and re-route of a consignment. */
@Controller
public class ReliefLogisticsController {

    private static final String DEFAULT_OFFICER = "DMC officer";

    private final ReliefLogisticsService logisticsService;
    private final HazardEventMapper hazardEventMapper;

    public ReliefLogisticsController(ReliefLogisticsService logisticsService, HazardEventMapper hazardEventMapper) {
        this.logisticsService = logisticsService;
        this.hazardEventMapper = hazardEventMapper;
    }

    @GetMapping("/relief-logistics")
    public String commandCenter(@RequestParam(required = false) Long hazardEventId,
                                @RequestParam(required = false) Long districtId,
                                @RequestParam(required = false) Long needId,
                                Model model) {
        CommandCenterView view = logisticsService.commandCenter(hazardEventId, districtId);
        DispatchFormRequest form = new DispatchFormRequest();
        form.setHazardEventId(view.hazardEventId);
        form.setDistrictId(view.districtId);
        view.needs.stream().filter(n -> n.id.equals(needId)).findFirst().ifPresent(n -> {
            form.setNeedId(n.id);
            form.setShelterId(n.shelterId);
            form.setQuantity(n.remaining);
        });
        model.addAttribute("form", form);
        addPageData(model, view);
        return "relief-logistics/command-center";
    }

    @PostMapping("/relief-logistics/needs")
    // TODO: restrict to DMC-officer/logistics-coordinator role once auth lands
    public String recordNeed(@Valid @ModelAttribute("needForm") NeedFormRequest needForm,
                             BindingResult bindingResult,
                             @RequestParam(required = false) Long hazardEventId,
                             @RequestParam(required = false) Long districtId,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", bindingResult.getFieldErrors().stream()
                    .map(e -> e.getDefaultMessage()).collect(Collectors.joining(". ")) + ".");
        } else {
            try {
                logisticsService.recordNeed(needForm.getShelterId(), needForm.getResourceType(), needForm.getQuantity(),
                        needForm.isUrgent(), needForm.getNote());
                redirectAttributes.addFlashAttribute("message", "Shelter need recorded.");
            } catch (ReliefValidationException ex) {
                redirectAttributes.addFlashAttribute("error", ex.getMessage());
            }
        }
        return backToCenter(hazardEventId, districtId);
    }

    @PostMapping("/relief-logistics/needs/{id}/cancel")
    // TODO: restrict to DMC-officer/logistics-coordinator role once auth lands
    public String cancelNeed(@PathVariable Long id,
                             @RequestParam(required = false) Long hazardEventId,
                             @RequestParam(required = false) Long districtId,
                             RedirectAttributes redirectAttributes) {
        try {
            logisticsService.cancelNeed(id);
            redirectAttributes.addFlashAttribute("message", "Shelter need withdrawn.");
        } catch (ReliefValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return backToCenter(hazardEventId, districtId);
    }

    @PostMapping("/relief-logistics/dispatch")
    // TODO: restrict to DMC-officer/logistics-coordinator role once auth lands
    public String dispatch(@Valid @ModelAttribute("form") DispatchFormRequest form,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addPageData(model, logisticsService.commandCenter(form.getHazardEventId(), form.getDistrictId()));
            return "relief-logistics/command-center";
        }
        try {
            DispatchOutcome outcome = logisticsService.dispatch(new DispatchRequest(form.getHazardEventId(),
                    form.getResourceId(), form.getShelterId(), form.getQuantity(), form.getNeedId(),
                    form.getHandlingNotes(), form.getVehicle(), form.getDriverName(), form.getDriverPhone(),
                    form.getExpectedArrival(), form.getExpectedStock()), form.isAcceptSplit(), officer(form.getOfficerName()));
            if (outcome.needsDecision()) {
                addPageData(model, logisticsService.commandCenter(form.getHazardEventId(), form.getDistrictId()));
                model.addAttribute("proposal", outcome.proposal());
                return "relief-logistics/command-center";
            }
            if (outcome.created().size() == 1) {
                redirectAttributes.addFlashAttribute("message",
                        outcome.created().get(0).reference() + " dispatched. The convoy unit was notified.");
                return "redirect:/relief-distributions/" + outcome.created().get(0).getId();
            }
            redirectAttributes.addFlashAttribute("message", "Split allocation committed: " + outcome.created().stream()
                    .map(c -> c.reference()).collect(Collectors.joining(", ")) + ".");
            return backToCenter(form.getHazardEventId(), form.getDistrictId());
        } catch (ReliefValidationException ex) {
            bindingResult.reject(null, ex.getMessage());
            addPageData(model, logisticsService.commandCenter(form.getHazardEventId(), form.getDistrictId()));
            return "relief-logistics/command-center";
        }
    }

    @GetMapping("/relief-logistics/notifications")
    public String notifications(Model model) {
        model.addAttribute("notifications", logisticsService.notifications());
        return "relief-logistics/notifications";
    }

    @PostMapping("/relief-distributions/{id}/handover")
    // TODO: restrict to the responding organisation's field lead once auth lands
    public String handover(@PathVariable Long id,
                           @RequestParam(required = false) String receivedBy,
                           @RequestParam(required = false) Integer receivedQuantity,
                           @RequestParam(required = false) Integer damagedQuantity,
                           @RequestParam(required = false) String note,
                           @RequestParam(required = false) MultipartFile photo,
                           @RequestParam(required = false) String officerName,
                           RedirectAttributes redirectAttributes) {
        try {
            byte[] bytes = photo == null || photo.isEmpty() ? null : photo.getBytes();
            logisticsService.handover(id, new HandoverRequest(receivedBy, receivedQuantity, damagedQuantity, note, bytes),
                    officer(officerName));
            redirectAttributes.addFlashAttribute("message", "Handover recorded. The consignment is distributed.");
        } catch (ReliefValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("error", "The photo could not be read.");
        }
        return "redirect:/relief-distributions/" + id;
    }

    @PostMapping("/relief-distributions/{id}/reroute")
    // TODO: restrict to DMC-officer/logistics-coordinator role once auth lands
    public String reroute(@PathVariable Long id,
                          @RequestParam(required = false) Long shelterId,
                          @RequestParam(required = false) String reason,
                          @RequestParam(required = false) String officerName,
                          RedirectAttributes redirectAttributes) {
        if (shelterId == null) {
            redirectAttributes.addFlashAttribute("error", "Choose the alternate shelter.");
            return "redirect:/relief-distributions/" + id;
        }
        try {
            logisticsService.reroute(id, shelterId, reason, officer(officerName));
            redirectAttributes.addFlashAttribute("message", "Consignment re-routed. The convoy driver was notified.");
        } catch (ReliefValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/relief-distributions/" + id;
    }

    private void addPageData(Model model, CommandCenterView view) {
        model.addAttribute("view", view);
        model.addAttribute("incidents", hazardEventMapper.toResponseList(logisticsService.listIncidents()));
        model.addAttribute("districts", logisticsService.listDistricts());
        model.addAttribute("resourceTypes", ResourceType.values());
        if (!model.containsAttribute("needForm")) {
            model.addAttribute("needForm", new NeedFormRequest());
        }
    }

    private String backToCenter(Long hazardEventId, Long districtId) {
        StringBuilder url = new StringBuilder("redirect:/relief-logistics");
        String sep = "?";
        if (hazardEventId != null) {
            url.append(sep).append("hazardEventId=").append(hazardEventId);
            sep = "&";
        }
        if (districtId != null) {
            url.append(sep).append("districtId=").append(districtId);
        }
        return url.toString();
    }

    private String officer(String name) {
        return name == null || name.isBlank() ? DEFAULT_OFFICER : name.trim();
    }
}
