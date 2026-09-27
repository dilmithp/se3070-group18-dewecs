package com.group18.dewecs.controller;

import com.group18.dewecs.domain.ShelterStatus;
import com.group18.dewecs.dto.CheckInRequest;
import com.group18.dewecs.dto.ShelterFormRequest;
import com.group18.dewecs.exception.ShelterValidationException;
import com.group18.dewecs.mapper.ShelterMapper;
import com.group18.dewecs.service.ShelterService;
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
@RequestMapping("/shelters")
public class ShelterController {

    private final ShelterService shelterService;
    private final ShelterMapper shelterMapper;

    public ShelterController(ShelterService shelterService, ShelterMapper shelterMapper) {
        this.shelterService = shelterService;
        this.shelterMapper = shelterMapper;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                        @RequestParam(required = false) Long districtId,
                        Model model) {
        ShelterStatus statusFilter = parseStatus(status);

        model.addAttribute("shelters", shelterMapper.toResponseList(shelterService.list(statusFilter, districtId)));
        model.addAttribute("statuses", ShelterStatus.values());
        model.addAttribute("districts", shelterService.listDistricts());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedDistrictId", districtId);
        return "shelters/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ShelterFormRequest());
        addFormReferenceData(model);
        return "shelters/form";
    }

    @PostMapping
    // TODO: restrict to DMC-officer/rescue-coordinator role once auth lands
    public String create(@Valid @ModelAttribute("form") ShelterFormRequest form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model);
            return "shelters/form";
        }
        try {
            var shelter = shelterService.create(form.getDistrictId(), form.getOrganizationId(), form.getName(),
                    form.getCapacity());
            redirectAttributes.addFlashAttribute("message", "Shelter created.");
            return "redirect:/shelters/" + shelter.getId();
        } catch (ShelterValidationException ex) {
            bindingResult.reject(null, ex.getMessage());
            addFormReferenceData(model);
            return "shelters/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("shelter", shelterMapper.toResponse(shelterService.getById(id)));
        model.addAttribute("occupants", shelterMapper.toOccupantResponseList(shelterService.listCurrentOccupants(id)));
        model.addAttribute("checkInForm", new CheckInRequest());
        return "shelters/detail";
    }

    @GetMapping("/{id}/edit")
    // TODO: restrict to DMC-officer/rescue-coordinator role once auth lands
    public String editForm(@PathVariable Long id, Model model) {
        var shelter = shelterService.getById(id);
        ShelterFormRequest form = new ShelterFormRequest();
        form.setDistrictId(shelter.getDistrict().getId());
        form.setOrganizationId(shelter.getOrganization().getId());
        form.setName(shelter.getName());
        form.setCapacity(shelter.getCapacity());

        model.addAttribute("form", form);
        model.addAttribute("shelterId", id);
        addFormReferenceData(model);
        return "shelters/form";
    }

    @PostMapping("/{id}")
    // TODO: restrict to DMC-officer/rescue-coordinator role once auth lands
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("form") ShelterFormRequest form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("shelterId", id);
            addFormReferenceData(model);
            return "shelters/form";
        }
        try {
            shelterService.update(id, form.getName(), form.getCapacity());
            redirectAttributes.addFlashAttribute("message", "Shelter updated.");
            return "redirect:/shelters/" + id;
        } catch (ShelterValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/shelters/" + id;
        }
    }

    @PostMapping("/{id}/close")
    // TODO: restrict to DMC-officer/rescue-coordinator role once auth lands
    public String close(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        shelterService.close(id);
        redirectAttributes.addFlashAttribute("message", "Shelter closed.");
        return "redirect:/shelters/" + id;
    }

    @PostMapping("/{id}/reopen")
    // TODO: restrict to DMC-officer/rescue-coordinator role once auth lands
    public String reopen(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            shelterService.reopen(id);
            redirectAttributes.addFlashAttribute("message", "Shelter reopened.");
        } catch (ShelterValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/shelters/" + id;
    }

    @PostMapping("/{id}/check-in")
    // TODO: restrict to DMC-officer/rescue-coordinator role once auth lands
    public String checkIn(@PathVariable Long id,
                           @Valid @ModelAttribute("checkInForm") CheckInRequest checkInForm,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Enter both a name and NIC to check in an occupant.");
            return "redirect:/shelters/" + id;
        }
        try {
            shelterService.checkIn(id, checkInForm.getFullName(), checkInForm.getNic());
            redirectAttributes.addFlashAttribute("message", "Occupant checked in.");
        } catch (ShelterValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/shelters/" + id;
    }

    @PostMapping("/{id}/check-out/{occupantId}")
    // TODO: restrict to DMC-officer/rescue-coordinator role once auth lands
    public String checkOut(@PathVariable Long id, @PathVariable Long occupantId,
                            RedirectAttributes redirectAttributes) {
        try {
            shelterService.checkOut(id, occupantId);
            redirectAttributes.addFlashAttribute("message", "Occupant checked out.");
        } catch (ShelterValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/shelters/" + id;
    }

    private void addFormReferenceData(Model model) {
        model.addAttribute("districts", shelterService.listDistricts());
        model.addAttribute("organizations", shelterService.listOrganizations());
    }

    private ShelterStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ShelterStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
