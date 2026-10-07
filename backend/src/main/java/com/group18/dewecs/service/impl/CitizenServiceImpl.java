package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.Citizen;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.exception.CitizenValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.repository.CitizenRepository;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.service.CitizenService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Deliberately NOT @Transactional: each repository call runs in its own transaction, so when two requests race on
 * a new NIC the unique-constraint failure is caught here, outside any transaction, and the winner's row is re-read.
 * A surrounding transaction would be rollback-only after the failure and the re-read would break.
 */
@Service
public class CitizenServiceImpl implements CitizenService {

    private static final Pattern NIC = Pattern.compile("^(\\d{9}[VX]|\\d{12})$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 -]{7,14}$");
    private static final int MAX_NAME_LENGTH = 120;

    private final CitizenRepository citizenRepository;
    private final DistrictRepository districtRepository;

    public CitizenServiceImpl(CitizenRepository citizenRepository, DistrictRepository districtRepository) {
        this.citizenRepository = citizenRepository;
        this.districtRepository = districtRepository;
    }

    @Override
    public IdentifyResult identify(String nic, String fullName, String phone, Long districtId) {
        String normalisedNic = normaliseNic(nic);
        String name = cleanName(fullName);
        if (phone == null || !PHONE.matcher(phone).matches()) {
            throw new CitizenValidationException("Phone number is not valid.");
        }
        if (districtId == null) {
            throw new CitizenValidationException("District is required.");
        }
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District not found: " + districtId));

        Optional<Citizen> existing = citizenRepository.findByNicIgnoreCase(normalisedNic);
        if (existing.isPresent()) {
            return new IdentifyResult(existing.get(), false);
        }

        Citizen citizen = new Citizen();
        citizen.setNic(normalisedNic);
        citizen.setFullName(name);
        citizen.setPhone(phone);
        citizen.setDistrict(district);
        try {
            return new IdentifyResult(citizenRepository.save(citizen), true);
        } catch (DataIntegrityViolationException race) {
            Citizen winner = citizenRepository.findByNicIgnoreCase(normalisedNic).orElseThrow(() -> race);
            return new IdentifyResult(winner, false);
        }
    }

    private String normaliseNic(String raw) {
        String nic = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!NIC.matcher(nic).matches()) {
            throw new CitizenValidationException("NIC must be 9 digits followed by V or X, or 12 digits.");
        }
        return nic;
    }

    private String cleanName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new CitizenValidationException("Full name must be 1 to " + MAX_NAME_LENGTH + " characters.");
        }
        return name;
    }
}
