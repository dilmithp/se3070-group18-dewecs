package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.RiverBasin;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.exception.WarningValidationException;
import com.group18.dewecs.repository.RiverBasinRepository;
import com.group18.dewecs.service.HazardEventService;
import com.group18.dewecs.service.WarningIssuanceService;
import com.group18.dewecs.service.WarningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class WarningIssuanceServiceImpl implements WarningIssuanceService {

    private final HazardEventService hazardEventService;
    private final WarningService warningService;
    private final RiverBasinRepository riverBasinRepository;

    public WarningIssuanceServiceImpl(HazardEventService hazardEventService,
                                      WarningService warningService,
                                      RiverBasinRepository riverBasinRepository) {
        this.hazardEventService = hazardEventService;
        this.warningService = warningService;
        this.riverBasinRepository = riverBasinRepository;
    }

    @Override
    @Transactional
    public Warning issue(String hazardType, String severity, Long districtId, Long riverBasinId, String title,
                         String message, Set<String> broadcastChannels, Long issuedByUserId) {
        if (message == null || message.isBlank()) {
            throw new WarningValidationException("A warning cannot be published without a message.");
        }
        Long eventDistrictId;
        if (riverBasinId != null) {
            RiverBasin basin = riverBasinRepository.findById(riverBasinId)
                    .orElseThrow(() -> new ResourceNotFoundException("River basin not found: " + riverBasinId));
            eventDistrictId = basin.getDistricts().stream()
                    .min(Comparator.comparing(District::getName))
                    .map(District::getId)
                    .orElseThrow(() -> new WarningValidationException("This river basin has no districts."));
        } else if (districtId != null) {
            eventDistrictId = districtId;
        } else {
            throw new WarningValidationException("Select the affected district or a river basin.");
        }

        String text = title == null || title.isBlank() ? message.trim() : title.trim() + "\n" + message.trim();
        HazardEvent event = hazardEventService.create(hazardType, severity, eventDistrictId, null);
        Warning draft = warningService.createDraft(event.getId(), severity, text, null, broadcastChannels,
                issuedByUserId, riverBasinId);
        return warningService.publish(draft.getId());
    }
}
