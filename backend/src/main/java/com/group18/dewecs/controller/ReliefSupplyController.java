package com.group18.dewecs.controller;

import com.group18.dewecs.domain.ResourceType;
import com.group18.dewecs.dto.ReliefSupplyFormRequest;
import com.group18.dewecs.dto.RestockRequest;
import com.group18.dewecs.exception.ReliefValidationException;
import com.group18.dewecs.mapper.ReliefSupplyMapper;
import com.group18.dewecs.service.ReliefSupplyService;
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
@RequestMapping("/relief-supplies")
public class ReliefSupplyController {

    private final ReliefSupplyService reliefSupplyService;
    private final ReliefSupplyMapper reliefSupplyMapper;

    public ReliefSupplyController(ReliefSupplyService reliefSupplyService, ReliefSupplyMapper reliefSupplyMapper) {
        this.reliefSupplyService = reliefSupplyService;
        this.reliefSupplyMapper = reliefSupplyMapper;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String type,
                        @RequestParam(required = false) Long districtId,
                        @RequestParam(defaultValue = "false") boolean lowStockOnly,
                        Model model) {
        ResourceType typeFilter = parseType(type);
        var supplies = lowStockOnly ? reliefSupplyService.listLowStock()
                : reliefSupplyService.list(typeFilter, districtId);

        model.addAttribute("supplies", reliefSupplyMapper.toResponseList(supplies));
        model.addAttribute("types", ResourceType.values());
        model.addAttribute("districts", reliefSupplyService.listDistricts());
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedDistrictId", districtId);
        model.addAttribute("lowStockOnly", lowStockOnly);
        return "relief-supplies/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ReliefSupplyFormRequest());
        addFormReferenceData(model);
        return "relief-supplies/form";
    }

    @PostMapping
    // TODO: restrict to DMC-officer/logistics-coordinator role once auth lands
    public String create(@Valid @ModelAttribute("form") ReliefSupplyFormRequest form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model);
            return "relief-supplies/form";
        }
        try {
            var resource = reliefSupplyService.create(form.getDistrictId(), form.getOrganizationId(), form.getName(),
                    form.getType(), form.getUnit(), form.getQuantity());
            redirectAttributes.addFlashAttribute("message", "Relief supply created.");
            return "redirect:/relief-supplies/" + resource.getId();
        } catch (ReliefValidationException ex) {
            bindingResult.reject(null, ex.getMessage());
            addFormReferenceData(model);
            return "relief-supplies/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("supply", reliefSupplyMapper.toResponse(reliefSupplyService.getById(id)));
        model.addAttribute("restockForm", new RestockRequest());
        return "relief-supplies/detail";
    }

    @GetMapping("/{id}/edit")
    // TODO: restrict to DMC-officer/logistics-coordinator role once auth lands
    public String editForm(@PathVariable Long id, Model model) {
        var resource = reliefSupplyService.getById(id);
        ReliefSupplyFormRequest form = new ReliefSupplyFormRequest();
        form.setDistrictId(resource.getDistrict().getId());
        form.setOrganizationId(resource.getOrganization().getId());
        form.setName(resource.getName());
        form.setType(resource.getType().name());
        form.setUnit(resource.getUnit());
        form.setQuantity(resource.getQuantity());

        model.addAttribute("form", form);
        model.addAttribute("resourceId", id);
        addFormReferenceData(model);
        return "relief-supplies/form";
    }

    @PostMapping("/{id}")
    // TODO: restrict to DMC-officer/logistics-coordinator role once auth lands
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("form") ReliefSupplyFormRequest form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("resourceId", id);
            addFormReferenceData(model);
            return "relief-supplies/form";
        }
        try {
            reliefSupplyService.update(id, form.getName(), form.getType(), form.getUnit());
            redirectAttributes.addFlashAttribute("message", "Relief supply updated.");
            return "redirect:/relief-supplies/" + id;
        } catch (ReliefValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/relief-supplies/" + id;
        }
    }

    @PostMapping("/{id}/restock")
    // TODO: restrict to DMC-officer/logistics-coordinator role once auth lands
    public String restock(@PathVariable Long id,
                           @Valid @ModelAttribute("restockForm") RestockRequest restockForm,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Enter a positive quantity to restock.");
            return "redirect:/relief-supplies/" + id;
        }
        try {
            reliefSupplyService.restock(id, restockForm.getQuantity());
            redirectAttributes.addFlashAttribute("message", "Stock updated.");
        } catch (ReliefValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/relief-supplies/" + id;
    }

    private void addFormReferenceData(Model model) {
        model.addAttribute("districts", reliefSupplyService.listDistricts());
        model.addAttribute("organizations", reliefSupplyService.listOrganizations());
        model.addAttribute("types", ResourceType.values());
    }

    private ResourceType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ResourceType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
