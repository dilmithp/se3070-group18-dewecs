package com.group18.dewecs.service;

import com.group18.dewecs.domain.Citizen;

public interface CitizenService {

    /**
     * Finds the citizen with this NIC or registers a new one. An existing citizen's stored name, phone and
     * district are never overwritten.
     */
    IdentifyResult identify(String nic, String fullName, String phone, Long districtId);

    record IdentifyResult(Citizen citizen, boolean created) {
    }
}
