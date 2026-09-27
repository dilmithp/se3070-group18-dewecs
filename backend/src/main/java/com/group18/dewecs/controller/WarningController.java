package com.group18.dewecs.controller;

import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.dto.WarningFormRequest;
import com.group18.dewecs.exception.WarningValidationException;
import com.group18.dewecs.mapper.WarningMapper;
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

@Controller
@RequestMapping("/warnings")
public class WarningController {

    private final WarningService warningService;
    private final WarningMapper warningMapper;

    public WarningController(WarningService warningService, WarningMapper warningMapper) {
        this.warningService = warningService;
        this.warningMapper = warningMapper;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                        @RequestParam(required = false) Long districtId,
                        Model model) {
        WarningStatus statusFilter = parseStatus(status);

        model.addAttribute("warnings", warningMapper.toResponseList(warningService.list(statusFilter, districtId)));
        model.addAttribute("statuses", WarningStatus.values());
        model.addAttribute("districts", warningService.listDistricts());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedDistrictId", districtId);
        return "warnings/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new WarningFormRequest());
        addFormReferenceData(model);
        return "warnings/form";
    }

    @PostMapping
    // TODO: restrict to DMC-officer role once auth lands
    public String create(@Valid @ModelAttribute("form") WarningFormRequest form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model);
            return "warnings/form";
        }

        try {
            Warning draft = warningService.createDraft(form.getHazardEventId(), form.getSeverity(),
                    form.getMessage(), form.getExpiresAt(), form.getBroadcastChannels(), form.getIssuedByUserId());
            redirectAttributes.addFlashAttribute("message", "Draft warning created.");
            return "redirect:/warnings/" + draft.getId();
        } catch (WarningValidationException ex) {
            bindingResult.reject(null, ex.getMessage());
            addFormReferenceData(model);
            return "warnings/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("warning", warningMapper.toResponse(warningService.getById(id)));
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
        addFormReferenceData(model);
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
            addFormReferenceData(model);
            return "warnings/form";
        }
        try {
            warningService.updateDraft(id, form.getSeverity(), form.getMessage(), form.getExpiresAt(),
                    form.getBroadcastChannels());
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

    private void addFormReferenceData(Model model) {
        model.addAttribute("hazardEvents", warningService.listHazardEventsForSelection());
        model.addAttribute("users", warningService.listUsersForSelection());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("channels", BroadcastChannel.values());
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
