package com.group18.dewecs.controller;

import com.group18.dewecs.domain.RescueRequestStatus;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.dto.RescueRequestFormRequest;
import com.group18.dewecs.exception.RescueRequestValidationException;
import com.group18.dewecs.mapper.RescueRequestMapper;
import com.group18.dewecs.service.RescueRequestService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rescue-requests")
public class RescueRequestController {

    private final RescueRequestService rescueRequestService;
    private final RescueRequestMapper rescueRequestMapper;

    public RescueRequestController(RescueRequestService rescueRequestService,
                                    RescueRequestMapper rescueRequestMapper) {
        this.rescueRequestService = rescueRequestService;
        this.rescueRequestMapper = rescueRequestMapper;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                        @RequestParam(required = false) String priority,
                        @RequestParam(required = false) Long districtId,
                        Model model) {
        RescueRequestStatus statusFilter = parseEnum(RescueRequestStatus.class, status);
        Severity priorityFilter = parseEnum(Severity.class, priority);

        model.addAttribute("requests", rescueRequestMapper.toResponseList(
                rescueRequestService.list(statusFilter, priorityFilter, districtId)));
        model.addAttribute("statuses", RescueRequestStatus.values());
        model.addAttribute("priorities", Severity.values());
        model.addAttribute("districts", rescueRequestService.listDistricts());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("selectedDistrictId", districtId);
        return "rescue-requests/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new RescueRequestFormRequest());
        addFormReferenceData(model);
        return "rescue-requests/form";
    }

    @PostMapping
    // TODO: restrict to DMC-officer/rescue-coordinator role once auth lands
    public String create(@Valid @ModelAttribute("form") RescueRequestFormRequest form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model);
            return "rescue-requests/form";
        }
        try {
            var request = rescueRequestService.submit(form.getDistrictId(), form.getRequesterName(),
                    form.getRequesterPhone(), form.getGpsLat(), form.getGpsLng(), form.getDescription(),
                    form.getPriority());
            redirectAttributes.addFlashAttribute("message", "Rescue request submitted.");
            return "redirect:/rescue-requests/" + request.getId();
        } catch (RescueRequestValidationException ex) {
            bindingResult.reject(null, ex.getMessage());
            addFormReferenceData(model);
            return "rescue-requests/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("request", rescueRequestMapper.toResponse(rescueRequestService.getById(id)));
        model.addAttribute("availableTeams", rescueRequestService.listTeamsForSelection());
        return "rescue-requests/detail";
    }

    @PostMapping("/{id}/assign")
    // TODO: restrict to DMC-officer/rescue-coordinator role once auth lands
    public String assign(@PathVariable Long id, @RequestParam Long teamId, RedirectAttributes redirectAttributes) {
        try {
            rescueRequestService.assign(id, teamId);
            redirectAttributes.addFlashAttribute("message", "Rescue team assigned.");
        } catch (RescueRequestValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/rescue-requests/" + id;
    }

    @PostMapping("/{id}/complete")
    // TODO: restrict to DMC-officer/rescue-coordinator role once auth lands
    public String complete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rescueRequestService.complete(id);
            redirectAttributes.addFlashAttribute("message", "Rescue request completed.");
        } catch (RescueRequestValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/rescue-requests/" + id;
    }

    @PostMapping("/{id}/cancel")
    // TODO: restrict to DMC-officer/rescue-coordinator role once auth lands
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rescueRequestService.cancel(id);
            redirectAttributes.addFlashAttribute("message", "Rescue request cancelled.");
        } catch (RescueRequestValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/rescue-requests/" + id;
    }

    private void addFormReferenceData(Model model) {
        model.addAttribute("districts", rescueRequestService.listDistricts());
        model.addAttribute("priorities", Severity.values());
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
