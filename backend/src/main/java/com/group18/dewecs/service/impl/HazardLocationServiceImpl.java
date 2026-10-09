package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.HazardEventLocation;
import com.group18.dewecs.exception.HazardEventValidationException;
import com.group18.dewecs.repository.HazardEventLocationRepository;
import com.group18.dewecs.service.HazardEventService;
import com.group18.dewecs.service.HazardLocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class HazardLocationServiceImpl implements HazardLocationService {

    private final HazardEventLocationRepository repository;
    private final HazardEventService hazardEventService;

    public HazardLocationServiceImpl(HazardEventLocationRepository repository, HazardEventService hazardEventService) {
        this.repository = repository;
        this.hazardEventService = hazardEventService;
    }

    @Override
    @Transactional
    public void save(Long hazardEventId, BigDecimal lat, BigDecimal lng) {
        if (lat == null && lng == null) {
            return;
        }
        if (lat == null || lng == null) {
            throw new HazardEventValidationException("Pick a point on the map, or leave the location empty.");
        }
        if (lat.abs().doubleValue() > 90 || lng.abs().doubleValue() > 180) {
            throw new HazardEventValidationException("The location is out of range.");
        }
        HazardEventLocation location = repository.findByHazardEvent_Id(hazardEventId).orElseGet(HazardEventLocation::new);
        location.setHazardEvent(hazardEventService.getById(hazardEventId));
        location.setLat(lat);
        location.setLng(lng);
        repository.save(location);
    }

    @Override
    public Optional<HazardEventLocation> find(Long hazardEventId) {
        return repository.findByHazardEvent_Id(hazardEventId);
    }
}
