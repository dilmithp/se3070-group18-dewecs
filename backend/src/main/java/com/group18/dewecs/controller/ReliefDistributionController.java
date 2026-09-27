package com.group18.dewecs.controller;

import com.group18.dewecs.domain.ConsignmentStatus;
import com.group18.dewecs.dto.ReliefDistributionFormRequest;
import com.group18.dewecs.exception.ReliefValidationException;
import com.group18.dewecs.mapper.ReliefDistributionMapper;
import com.group18.dewecs.service.ReliefDistributionService;
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
@RequestMapping("/relief-distributions")
public class ReliefDistributionController {

    private final ReliefDistributionService reliefDistributionService;
    private final ReliefDistributionMapper reliefDistributionMapper;

    public ReliefDistributionController(ReliefDistributionService reliefDistributionService,
                                         ReliefDistributionMapper reliefDistributionMapper) {
        this.reliefDistributionService = reliefDistributionService;
        this.reliefDistributionMapper = reliefDistributionMapper;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                        @RequestParam(required = false) Long shelterId,
                        @RequestParam(required = false) Long resourceId,
                        Model model) {
        ConsignmentStatus statusFilter = parseStatus(status);

        model.addAttribute("distributions", reliefDistributionMapper.toResponseList(
                reliefDistributionService.list(statusFilter, shelterId, resourceId)));
        model.addAttribute("statuses", ConsignmentStatus.values());
        model.addAttribute("shelters", reliefDistributionService.listSheltersForSelection());
        model.addAttribute("supplies", reliefDistributionService.listSuppliesForSelection());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedShelterId", shelterId);
        model.addAttribute("selectedResourceId", resourceId);
        return "relief-distributions/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ReliefDistributionFormRequest());
        addFormReferenceData(model);
        return "relief-distributions/form";
    }

    @PostMapping
    // TODO: restrict to DMC-officer/logistics-coordinator role once auth lands
    public String create(@Valid @ModelAttribute("form") ReliefDistributionFormRequest form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model);
            return "relief-distributions/form";
        }
        try {
            var consignment = reliefDistributionService.create(form.getResourceId(), form.getShelterId(),
                    form.getQuantity());
            redirectAttributes.addFlashAttribute("message", "Relief distribution dispatched.");
            return "redirect:/relief-distributions/" + consignment.getId();
        } catch (ReliefValidationException ex) {
            bindingResult.reject(null, ex.getMessage());
            addFormReferenceData(model);
            return "relief-distributions/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("distribution", reliefDistributionMapper.toResponse(
                reliefDistributionService.getById(id)));
        return "relief-distributions/detail";
    }

    @PostMapping("/{id}/deliver")
    // TODO: restrict to DMC-officer/logistics-coordinator role once auth lands
    public String deliver(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reliefDistributionService.deliver(id);
            redirectAttributes.addFlashAttribute("message", "Distribution marked delivered.");
        } catch (ReliefValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/relief-distributions/" + id;
    }

    @PostMapping("/{id}/cancel")
    // TODO: restrict to DMC-officer/logistics-coordinator role once auth lands
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reliefDistributionService.cancel(id);
            redirectAttributes.addFlashAttribute("message", "Distribution cancelled; stock restored.");
        } catch (ReliefValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/relief-distributions/" + id;
    }

    private void addFormReferenceData(Model model) {
        model.addAttribute("supplies", reliefDistributionService.listSuppliesForSelection());
        model.addAttribute("shelters", reliefDistributionService.listSheltersForSelection());
    }

    private ConsignmentStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ConsignmentStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
