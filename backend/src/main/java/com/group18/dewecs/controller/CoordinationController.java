package com.group18.dewecs.controller;

import com.group18.dewecs.domain.TeamCapability;
import com.group18.dewecs.dto.CoordinationViews.DispatchOptions;
import com.group18.dewecs.dto.CoordinationViews.OccupancyResult;
import com.group18.dewecs.dto.CoordinationViews.SyncResult;
import com.group18.dewecs.dto.CoordinationViews.TeamUpdateResult;
import com.group18.dewecs.exception.CoordinationValidationException;
import com.group18.dewecs.exception.RescueRequestValidationException;
import com.group18.dewecs.exception.ReliefValidationException;
import com.group18.dewecs.service.DistrictCoordinationService;
import com.group18.dewecs.service.DistrictCoordinationService.QueuedOccupancy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** District coordination (UC-03): one operational picture of shelters, rescue teams and incidents. */
@Controller
@RequestMapping("/coordination")
public class CoordinationController {

    private final DistrictCoordinationService service;

    public CoordinationController(DistrictCoordinationService service) {
        this.service = service;
    }

    @GetMapping
    public String dashboard(@RequestParam(required = false) Long districtId, Model model) {
        model.addAttribute("districts", service.listDistricts());
        model.addAttribute("organizations", service.listOrganizations());
        model.addAttribute("capabilities", TeamCapability.values());
        if (districtId != null) {
            model.addAttribute("picture", service.picture(districtId));
            model.addAttribute("supplies", service.listSupplies());
        }
        model.addAttribute("districtId", districtId);
        return "coordination/dashboard";
    }

    @PostMapping("/shelters/{id}/occupancy")
    // TODO: restrict to the district officer of this district once auth lands
    public String occupancy(@PathVariable Long id,
                            @RequestParam Long districtId,
                            @RequestParam(required = false) Integer occupancy,
                            @RequestParam(required = false) Integer expectedOccupancy,
                            @RequestParam(required = false) Long organizationId,
                            @RequestParam(required = false) String officerName,
                            RedirectAttributes redirect) {
        try {
            OccupancyResult r = service.updateOccupancy(id, occupancy, expectedOccupancy, officerName, organizationId, null);
            String text = r.shelterName + ": " + r.occupancy + " of " + r.capacity + " occupied.";
            if (r.conflict) {
                text += " Another update was made at the same time; both are logged and the latest value is applied.";
            }
            if (r.level.equals("FULL")) {
                text += " The shelter is full: check-in is blocked. Nearest with room: " + (r.alternates.isEmpty() ? "none" : String.join("; ", r.alternates)) + ".";
            } else if (r.level.equals("NEAR")) {
                text += " Near capacity. Alternates: " + (r.alternates.isEmpty() ? "none" : String.join("; ", r.alternates)) + ".";
            }
            redirect.addFlashAttribute("message", text);
        } catch (CoordinationValidationException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return back(districtId);
    }

    @GetMapping("/incidents/{id}")
    public String dispatchPage(@PathVariable Long id,
                               @RequestParam(required = false) String capability,
                               @RequestParam(required = false) Long organizationId,
                               Model model) {
        DispatchOptions options = service.dispatchOptions(id, parseCapability(capability), organizationId);
        model.addAttribute("options", options);
        model.addAttribute("organizations", service.listOrganizations());
        model.addAttribute("capabilities", TeamCapability.values());
        model.addAttribute("selectedCapability", capability);
        model.addAttribute("selectedOrganizationId", organizationId);
        return "coordination/dispatch";
    }

    @PostMapping("/incidents/{id}/dispatch")
    // TODO: restrict to the district officer of this district once auth lands
    public String dispatch(@PathVariable Long id,
                           @RequestParam(required = false) Long teamId,
                           @RequestParam(required = false) String notes,
                           @RequestParam(required = false) Long organizationId,
                           @RequestParam(required = false, defaultValue = "false") boolean confirmCrossOrganization,
                           @RequestParam(required = false) String officerName,
                           RedirectAttributes redirect) {
        Long districtId = service.dispatchOptions(id, null, null).districtId;
        if (teamId == null) {
            redirect.addFlashAttribute("error", "Choose a rescue team.");
            return "redirect:/coordination/incidents/" + id;
        }
        try {
            service.dispatch(id, teamId, notes, organizationId, confirmCrossOrganization, officerName);
            redirect.addFlashAttribute("message", "Team dispatched. The responding organization was notified.");
            return back(districtId);
        } catch (CoordinationValidationException | RescueRequestValidationException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/coordination/incidents/" + id;
        }
    }

    @PostMapping("/incidents/{id}/escalate")
    // TODO: restrict to the district officer of this district once auth lands
    public String escalate(@PathVariable Long id, @RequestParam(required = false) String officerName,
                           RedirectAttributes redirect) {
        Long districtId = service.dispatchOptions(id, null, null).districtId;
        service.escalateUnassigned(id, officerName);
        redirect.addFlashAttribute("message", "The incident is marked unassigned and escalated to the DMC.");
        return back(districtId);
    }

    @PostMapping("/teams/{id}/status")
    // TODO: the team reports through its own device or organization channel once auth lands
    public String teamStatus(@PathVariable Long id,
                             @RequestParam Long districtId,
                             @RequestParam String stage,
                             @RequestParam(required = false) String note,
                             @RequestParam(required = false) String officerName,
                             RedirectAttributes redirect) {
        try {
            TeamUpdateResult r = service.updateTeamStage(id, stage, note, officerName);
            String text = r.teamName + " reported: " + r.stage.toLowerCase(Locale.ROOT).replace('_', ' ') + ".";
            if (r.stage.equals("NEEDS_SUPPORT")) {
                text += " Backup teams nearby: " + (r.backups.isEmpty() ? "none available" : String.join(", ", r.backups)) + ".";
            }
            redirect.addFlashAttribute("message", text);
        } catch (CoordinationValidationException | RescueRequestValidationException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return back(districtId);
    }

    @PostMapping("/teams/{id}/profile")
    // TODO: restrict to the owning organization once auth lands
    public String profile(@PathVariable Long id,
                          @RequestParam Long districtId,
                          @RequestParam(required = false) String capability,
                          @RequestParam(required = false) BigDecimal baseLat,
                          @RequestParam(required = false) BigDecimal baseLng,
                          RedirectAttributes redirect) {
        try {
            service.saveProfile(id, parseCapability(capability), baseLat, baseLng);
            redirect.addFlashAttribute("message", "Team profile saved.");
        } catch (CoordinationValidationException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return back(districtId);
    }

    @PostMapping("/supplies")
    // TODO: restrict to the district officer of this district once auth lands
    public String supplies(@RequestParam Long districtId,
                           @RequestParam(required = false) Long shelterId,
                           @RequestParam(required = false) Long resourceId,
                           @RequestParam(required = false) Integer quantity,
                           @RequestParam(required = false) Long organizationId,
                           @RequestParam(required = false) String officerName,
                           RedirectAttributes redirect) {
        if (shelterId == null || resourceId == null) {
            redirect.addFlashAttribute("error", "Choose the shelter and the supply.");
            return back(districtId);
        }
        try {
            service.logSupply(shelterId, resourceId, quantity, officerName, organizationId);
            redirect.addFlashAttribute("message", "Supply distribution logged against the owning organization's stock.");
        } catch (CoordinationValidationException | ReliefValidationException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return back(districtId);
    }

    /**
     * Replays updates queued by the browser while it was offline. {@code actions} holds one update per line:
     * shelterId,occupancy,time (the time the officer made it, ISO local date-time).
     */
    @PostMapping("/sync")
    @ResponseBody
    public Map<String, Object> sync(@RequestParam String actions, @RequestParam(required = false) String officerName) {
        List<QueuedOccupancy> queued = new ArrayList<>();
        int unreadable = 0;
        for (String line : actions.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            try {
                String[] p = line.trim().split(",");
                LocalDateTime time = p.length > 2 && !p[2].isBlank() ? LocalDateTime.parse(p[2].trim()) : null;
                queued.add(new QueuedOccupancy(Long.valueOf(p[0].trim()), Integer.valueOf(p[1].trim()), time));
            } catch (RuntimeException ex) {
                unreadable++;
            }
        }
        SyncResult r = service.sync(queued, officerName);
        return Map.of("applied", r.applied, "conflicts", r.conflicts, "rejected", r.rejected + unreadable);
    }

    private TeamCapability parseCapability(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TeamCapability.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String back(Long districtId) {
        return "redirect:/coordination" + (districtId == null ? "" : "?districtId=" + districtId);
    }
}
