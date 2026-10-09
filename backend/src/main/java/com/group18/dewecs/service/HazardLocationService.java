package com.group18.dewecs.service;

import com.group18.dewecs.domain.HazardEventLocation;

import java.math.BigDecimal;
import java.util.Optional;

/** The map position of a hazard event, picked on the registration form. */
public interface HazardLocationService {

    /** Saves the position; both values null means none was picked (nothing is stored). */
    void save(Long hazardEventId, BigDecimal lat, BigDecimal lng);

    Optional<HazardEventLocation> find(Long hazardEventId);
}
