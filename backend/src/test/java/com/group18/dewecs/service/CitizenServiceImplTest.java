package com.group18.dewecs.service;

import com.group18.dewecs.domain.Citizen;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.exception.CitizenValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.repository.CitizenRepository;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.service.CitizenService.IdentifyResult;
import com.group18.dewecs.service.impl.CitizenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenServiceImplTest {

    @Mock
    private CitizenRepository citizenRepository;
    @Mock
    private DistrictRepository districtRepository;

    private CitizenServiceImpl service;
    private District district;

    @BeforeEach
    void setUp() {
        service = new CitizenServiceImpl(citizenRepository, districtRepository);
        district = new District();
        district.setName("Colombo");
    }

    private void districtExists() {
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
    }

    @Test
    void identify_newNic_createsCitizenWithNormalisedValues() {
        districtExists();
        when(citizenRepository.findByNicIgnoreCase("901234567V")).thenReturn(Optional.empty());
        when(citizenRepository.save(any(Citizen.class))).thenAnswer(inv -> inv.getArgument(0));

        IdentifyResult result = service.identify("  901234567v ", "  Nimal Perera ", "0771234567", 1L);

        assertThat(result.created()).isTrue();
        ArgumentCaptor<Citizen> saved = ArgumentCaptor.forClass(Citizen.class);
        verify(citizenRepository).save(saved.capture());
        assertThat(saved.getValue().getNic()).isEqualTo("901234567V");
        assertThat(saved.getValue().getFullName()).isEqualTo("Nimal Perera");
        assertThat(saved.getValue().getDistrict()).isSameAs(district);
    }

    @Test
    void identify_twelveDigitNicIsAccepted() {
        districtExists();
        when(citizenRepository.findByNicIgnoreCase("199012345678")).thenReturn(Optional.empty());
        when(citizenRepository.save(any(Citizen.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.identify("199012345678", "A", "+94 77 123 4567", 1L).created()).isTrue();
    }

    @Test
    void identify_existingNic_returnsStoredCitizenAndNeverOverwrites() {
        districtExists();
        Citizen stored = new Citizen();
        stored.setNic("199012345678");
        stored.setFullName("Stored Name");
        when(citizenRepository.findByNicIgnoreCase("199012345678")).thenReturn(Optional.of(stored));

        IdentifyResult result = service.identify("199012345678", "Other Name", "0779999999", 1L);

        assertThat(result.created()).isFalse();
        assertThat(result.citizen().getFullName()).isEqualTo("Stored Name");
        verify(citizenRepository, never()).save(any());
    }

    @Test
    void identify_badNicFormats_areRejected() {
        for (String nic : new String[] {null, "", "  ", "12345", "19901234567", "1990123456789", "90123456AV",
                "901234567Z", "9012345678V", "ABCDEFGHIJKL"}) {
            assertThatThrownBy(() -> service.identify(nic, "A", "0771234567", 1L))
                    .as("nic=%s", nic).isInstanceOf(CitizenValidationException.class);
        }
    }

    @Test
    void identify_badNameOrPhone_areRejected() {
        assertThatThrownBy(() -> service.identify("199012345678", "   ", "0771234567", 1L))
                .isInstanceOf(CitizenValidationException.class);
        assertThatThrownBy(() -> service.identify("199012345678", "x".repeat(121), "0771234567", 1L))
                .isInstanceOf(CitizenValidationException.class);
        for (String phone : new String[] {null, "", "077", "abcdefghij", "0771234567890123", "+", "--12345678"}) {
            assertThatThrownBy(() -> service.identify("199012345678", "A", phone, 1L))
                    .as("phone=%s", phone).isInstanceOf(CitizenValidationException.class);
        }
    }

    @Test
    void identify_unknownDistrict_isNotFound() {
        when(districtRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.identify("199012345678", "A", "0771234567", 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void identify_raceOnUniqueNic_rereadsTheWinner() {
        districtExists();
        Citizen winner = new Citizen();
        winner.setNic("199012345678");
        when(citizenRepository.findByNicIgnoreCase("199012345678"))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(citizenRepository.save(any(Citizen.class))).thenThrow(new DataIntegrityViolationException("unique"));

        IdentifyResult result = service.identify("199012345678", "A", "0771234567", 1L);

        assertThat(result.created()).isFalse();
        assertThat(result.citizen()).isSameAs(winner);
    }
}
