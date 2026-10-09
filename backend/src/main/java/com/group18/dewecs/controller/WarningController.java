package com.group18.dewecs.controller;

import com.group18.dewecs.domain.AlertDeliveryLog;
import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.DeliveryOutcome;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.dto.IssueWarningFormRequest;
import com.group18.dewecs.dto.WarningFormRequest;
import com.group18.dewecs.exception.HazardEventValidationException;
import com.group18.dewecs.exception.WarningValidationException;
import com.group18.dewecs.mapper.DeliveryLogMapper;
import com.group18.dewecs.mapper.HazardEventMapper;
import com.group18.dewecs.mapper.WarningMapMapper;
import com.group18.dewecs.mapper.WarningMapper;
import com.group18.dewecs.service.HazardEventService;
import com.group18.dewecs.service.WarningIssuanceService;
import com.group18.dewecs.service.WarningService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/warnings")
public class WarningController {

    private final WarningService warningService;
    private final WarningMapper warningMapper;
    private final WarningMapMapper warningMapMapper;
    private final DeliveryLogMapper deliveryLogMapper;
    private final HazardEventService hazardEventService;
    private final HazardEventMapper hazardEventMapper;
    private final WarningIssuanceService warningIssuanceService;

    public WarningController(WarningService warningService,
                              WarningMapper warningMapper,
                              WarningMapMapper warningMapMapper,
                              DeliveryLogMapper deliveryLogMapper,
                              HazardEventService hazardEventService,
                              HazardEventMapper hazardEventMapper,
                              WarningIssuanceService warningIssuanceService) {
        this.warningService = warningService;
        this.warningMapper = warningMapper;
        this.warningMapMapper = warningMapMapper;
        this.deliveryLogMapper = deliveryLogMapper;
        this.hazardEventService = hazardEventService;
        this.hazardEventMapper = hazardEventMapper;
        this.warningIssuanceService = warningIssuanceService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                        @RequestParam(required = false) Long districtId,
                        Model model) {
        WarningStatus statusFilter = parseStatus(status);

        model.addAttribute("warnings", warningMapper.toResponseList(warningService.list(statusFilter, districtId)));
        // The map shows the live warnings (of the chosen district) whatever status the table is filtered by.
        model.addAttribute("map", warningMapMapper.toView(warningService.list(null, districtId)));
        model.addAttribute("statuses", WarningStatus.values());
        model.addAttribute("districts", warningService.listDistricts());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedDistrictId", districtId);
        return "warnings/list";
    }

    /** The one-screen "Add New Hazard Event": form on the left, map and active warnings on the right. */
    @GetMapping("/issue")
    public String issueForm(Model model) {
        model.addAttribute("form", new IssueWarningFormRequest());
        addIssueReferenceData(model);
        return "warnings/issue";
    }

    @PostMapping("/issue")
    // TODO: restrict to DMC-officer role once auth lands
    public String issue(@Valid @ModelAttribute("form") IssueWarningFormRequest form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addIssueReferenceData(model);
            return "warnings/issue";
        }
        boolean byBasin = "BASIN".equalsIgnoreCase(form.getAreaMode());
        try {
            Warning issued = warningIssuanceService.issue(form.getHazardType(), form.getSeverity(),
                    byBasin ? null : form.getDistrictId(), byBasin ? form.getRiverBasinId() : null,
                    form.getTitle(), form.getMessage(), form.getBroadcastChannels(), form.getIssuedByUserId());
            redirectAttributes.addFlashAttribute("message", "Warning issued and broadcast.");
            return "redirect:/warnings/" + issued.getId();
        } catch (WarningValidationException | HazardEventValidationException ex) {
            bindingResult.reject(null, ex.getMessage());
            addIssueReferenceData(model);
            return "warnings/issue";
        }
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long hazardEventId, Model model) {
        WarningFormRequest form = new WarningFormRequest();
        form.setHazardEventId(hazardEventId);
        model.addAttribute("form", form);
        addFormReferenceData(model, hazardEventId);
        return "warnings/form";
    }

    @PostMapping
    // TODO: restrict to DMC-officer role once auth lands
    public String create(@Valid @ModelAttribute("form") WarningFormRequest form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, form.getHazardEventId());
            return "warnings/form";
        }

        try {
            Warning draft = warningService.createDraft(form.getHazardEventId(), form.getSeverity(),
                    form.getMessage(), form.getExpiresAt(), form.getBroadcastChannels(), form.getIssuedByUserId(),
                    form.getRiverBasinId());
            redirectAttributes.addFlashAttribute("message", "Draft warning created.");
            return "redirect:/warnings/" + draft.getId();
        } catch (WarningValidationException ex) {
            bindingResult.reject(null, ex.getMessage());
            addFormReferenceData(model, form.getHazardEventId());
            return "warnings/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Warning warning = warningService.getById(id);
        model.addAttribute("warning", warningMapper.toResponse(warning));
        model.addAttribute("deliveryLog", deliveryLogMapper.toResponseList(warningService.listDeliveryLog(id)));
        model.addAttribute("allDelivered", warningService.allChannelsDelivered(id));
        model.addAttribute("escalationOptions", severitiesAbove(warning.getSeverity()));
        return "warnings/detail";
    }

    @GetMapping("/{id}/edit")
    // TODO: restrict to DMC-officer role once auth lands
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Warning warning = warningService.getById(id);
        if (warning.getStatus() != WarningStatus.DRAFT) {
            redirectAttributes.addFlashAttribute("error", "Only draft warnings can be edited.");
            return "redirect:/warnings/" + id;
        }
        model.addAttribute("form", warningMapper.toFormRequest(warning));
        model.addAttribute("warningId", id);
        model.addAttribute("currentDistricts", warningMapper.toResponse(warning).getAffectedDistrictNames());
        addFormReferenceData(model, warning.getHazardEvent().getId());
        return "warnings/form";
    }

    @PostMapping("/{id}")
    // TODO: restrict to DMC-officer role once auth lands
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("form") WarningFormRequest form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("warningId", id);
            addFormReferenceData(model, form.getHazardEventId());
            return "warnings/form";
        }
        try {
            warningService.updateDraft(id, form.getSeverity(), form.getMessage(), form.getExpiresAt(),
                    form.getBroadcastChannels(), form.getRiverBasinId());
            redirectAttributes.addFlashAttribute("message", "Draft updated.");
            return "redirect:/warnings/" + id;
        } catch (WarningValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/warnings/" + id;
        }
    }

    @PostMapping("/{id}/publish")
    // TODO: restrict to DMC-officer role once auth lands
    public String publish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            warningService.publish(id);
            redirectAttributes.addFlashAttribute("message", "Warning published.");
        } catch (WarningValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/warnings/" + id;
    }

    @PostMapping("/{id}/retract")
    // TODO: restrict to DMC-officer role once auth lands
    public String retract(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            warningService.retract(id);
            redirectAttributes.addFlashAttribute("message", "Warning retracted.");
        } catch (WarningValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/warnings/" + id;
    }

    @PostMapping("/{id}/escalate")
    // TODO: restrict to DMC-officer role once auth lands
    public String escalate(@PathVariable Long id,
                            @RequestParam(required = false) String severity,
                            @RequestParam(required = false) String note,
                            @RequestParam(required = false) LocalDateTime expiresAt,
                            RedirectAttributes redirectAttributes) {
        if (severity == null || severity.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Choose the higher severity to escalate to.");
            return "redirect:/warnings/" + id;
        }
        try {
            Warning escalated = warningService.escalate(id, severity, note, expiresAt);
            redirectAttributes.addFlashAttribute("message",
                    "Warning escalated to " + escalated.getSeverity().name() + " and sent again.");
        } catch (WarningValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/warnings/" + id;
    }

    @PostMapping("/{id}/rebroadcast")
    // TODO: restrict to DMC-officer role once auth lands
    public String rebroadcast(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            List<AlertDeliveryLog> rows = warningService.rebroadcast(id);
            if (rows.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "Every channel is already delivered. Nothing to send again.");
            } else {
                long sent = rows.stream().filter(r -> r.getOutcome() == DeliveryOutcome.SENT).count();
                redirectAttributes.addFlashAttribute("message",
                        "Sent again: " + sent + " delivered, " + (rows.size() - sent) + " failed attempt(s).");
            }
        } catch (WarningValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/warnings/" + id;
    }

    private void addIssueReferenceData(Model model) {
        model.addAttribute("hazardTypes", HazardType.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("districts", warningService.listDistricts());
        model.addAttribute("riverBasins", warningService.listRiverBasins());
        model.addAttribute("channels", BroadcastChannel.values());
        model.addAttribute("users", warningService.listUsersForSelection());
        List<Warning> active = warningService.listActive();
        model.addAttribute("activeWarnings", warningMapper.toActiveCards(active));
        model.addAttribute("map", warningMapMapper.toView(active));
    }

    private void addFormReferenceData(Model model, Long hazardEventId) {
        model.addAttribute("hazardEvents", warningService.listHazardEventsForSelection());
        model.addAttribute("users", warningService.listUsersForSelection());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("channels", BroadcastChannel.values());
        model.addAttribute("riverBasins", warningService.listRiverBasins());
        if (hazardEventId != null) {
            try {
                model.addAttribute("review", hazardEventMapper.toReview(hazardEventService.review(hazardEventId)));
            } catch (RuntimeException ex) {
                // An unknown event id simply shows no evidence panel; the form validation reports the real problem.
            }
        }
    }

    /** The severities above the current one, lowest first: what the warning can be escalated to. */
    private List<String> severitiesAbove(Severity current) {
        return Arrays.stream(Severity.values()).filter(s -> s.ordinal() > current.ordinal()).map(Enum::name).toList();
    }

    private WarningStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return WarningStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
