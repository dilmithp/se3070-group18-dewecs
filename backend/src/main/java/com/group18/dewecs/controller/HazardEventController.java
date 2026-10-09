package com.group18.dewecs.controller;

import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.dto.HazardEventFormRequest;
import com.group18.dewecs.exception.HazardEventValidationException;
import com.group18.dewecs.mapper.HazardEventMapper;
import com.group18.dewecs.service.HazardEventService;
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

import java.util.Arrays;
import java.util.List;

/** Hazard events: register an incident, read what is known about it, then issue a warning for it. */
@Controller
@RequestMapping("/hazard-events")
public class HazardEventController {

    private final HazardEventService hazardEventService;
    private final HazardEventMapper hazardEventMapper;

    private final com.group18.dewecs.service.HazardLocationService locationService;

    public HazardEventController(HazardEventService hazardEventService, HazardEventMapper hazardEventMapper,
                                 com.group18.dewecs.service.HazardLocationService locationService) {
        this.locationService = locationService;
        this.hazardEventService = hazardEventService;
        this.hazardEventMapper = hazardEventMapper;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("events", hazardEventMapper.toResponseList(hazardEventService.list()));
        return "hazard-events/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new HazardEventFormRequest());
        addFormReferenceData(model);
        return "hazard-events/form";
    }

    @PostMapping
    // TODO: restrict to DMC-officer role once auth lands
    public String create(@Valid @ModelAttribute("form") HazardEventFormRequest form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model);
            return "hazard-events/form";
        }
        try {
            HazardEvent event = hazardEventService.create(form.getHazardType(), form.getSeverity(),
                    form.getDistrictId(), form.getOccurredAt());
            locationService.save(event.getId(), form.getGpsLat(), form.getGpsLng());
            redirectAttributes.addFlashAttribute("message", "Hazard event registered.");
            return "redirect:/hazard-events/" + event.getId();
        } catch (HazardEventValidationException ex) {
            bindingResult.reject(null, ex.getMessage());
            addFormReferenceData(model);
            return "hazard-events/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var review = hazardEventMapper.toReview(hazardEventService.review(id));
        model.addAttribute("review", review);
        model.addAttribute("location", locationService.find(id).orElse(null));
        model.addAttribute("nextStatuses", statusesAfter(review.getEvent().getStatus()));
        return "hazard-events/detail";
    }

    @PostMapping("/{id}/status")
    // TODO: restrict to DMC-officer role once auth lands
    public String updateStatus(@PathVariable Long id, @RequestParam String status,
                                RedirectAttributes redirectAttributes) {
        try {
            HazardEvent event = hazardEventService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("message", "Hazard event is now " + event.getStatus().name() + ".");
        } catch (HazardEventValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/hazard-events/" + id;
    }

    private void addFormReferenceData(Model model) {
        model.addAttribute("hazardTypes", HazardType.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("districts", hazardEventService.listDistricts());
    }

    private List<String> statusesAfter(String current) {
        HazardEventStatus now = HazardEventStatus.valueOf(current);
        return Arrays.stream(HazardEventStatus.values()).filter(s -> s.ordinal() > now.ordinal()).map(Enum::name).toList();
    }
}
