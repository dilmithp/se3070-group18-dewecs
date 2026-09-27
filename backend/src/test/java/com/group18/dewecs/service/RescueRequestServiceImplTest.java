package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.OrganizationType;
import com.group18.dewecs.domain.RescueRequest;
import com.group18.dewecs.domain.RescueRequestStatus;
import com.group18.dewecs.domain.RescueTeam;
import com.group18.dewecs.domain.RescueTeamStatus;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.exception.RescueRequestValidationException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.RescueRequestRepository;
import com.group18.dewecs.repository.RescueTeamRepository;
import com.group18.dewecs.service.impl.RescueRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RescueRequestServiceImplTest {

    @Mock
    private RescueRequestRepository rescueRequestRepository;
    @Mock
    private RescueTeamRepository rescueTeamRepository;
    @Mock
    private DistrictRepository districtRepository;

    private RescueRequestServiceImpl rescueRequestService;
    private District district;
    private Organization organization;

    @BeforeEach
    void setUp() {
        rescueRequestService = new RescueRequestServiceImpl(rescueRequestRepository, rescueTeamRepository,
                districtRepository);

        district = new District();
        district.setId(1L);
        district.setName("Colombo");

        organization = new Organization();
        organization.setId(2L);
        organization.setName("Civil Defence");
        organization.setType(OrganizationType.GOVERNMENT);
    }

    @Test
    void submit_createsPendingRequest() {
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
        when(rescueRequestRepository.save(any(RescueRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RescueRequest request = rescueRequestService.submit(1L, "Jane Doe", "0771234567",
                new BigDecimal("6.9271"), new BigDecimal("79.8612"), "Trapped by rising floodwater", "high");

        assertThat(request.getStatus()).isEqualTo(RescueRequestStatus.PENDING);
        assertThat(request.getPriority()).isEqualTo(Severity.HIGH);
        assertThat(request.getSubmittedAt()).isNotNull();
    }

    @Test
    void submit_withInvalidPriority_throwsValidationException() {
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));

        assertThatThrownBy(() -> rescueRequestService.submit(1L, "Jane Doe", "0771234567", null, null,
                "Trapped by rising floodwater", "URGENT"))
                .isInstanceOf(RescueRequestValidationException.class);
    }

    @Test
    void assign_pendingRequestToAvailableTeam_setsAssignedAndDispatchesTeam() {
        RescueRequest request = requestOf(RescueRequestStatus.PENDING);
        RescueTeam team = teamOf(RescueTeamStatus.AVAILABLE);

        when(rescueRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(rescueTeamRepository.findById(200L)).thenReturn(Optional.of(team));
        when(rescueTeamRepository.save(any(RescueTeam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rescueRequestRepository.save(any(RescueRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RescueRequest assigned = rescueRequestService.assign(100L, 200L);

        assertThat(assigned.getStatus()).isEqualTo(RescueRequestStatus.ASSIGNED);
        assertThat(assigned.getAssignedTeam()).isEqualTo(team);
        assertThat(assigned.getAssignedAt()).isNotNull();
        assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.DISPATCHED);
    }

    @Test
    void assign_toUnavailableTeam_throwsValidationException() {
        RescueRequest request = requestOf(RescueRequestStatus.PENDING);
        RescueTeam team = teamOf(RescueTeamStatus.DISPATCHED);

        when(rescueRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(rescueTeamRepository.findById(200L)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> rescueRequestService.assign(100L, 200L))
                .isInstanceOf(RescueRequestValidationException.class);
    }

    @Test
    void assign_onAlreadyAssignedRequest_throwsValidationException() {
        RescueRequest request = requestOf(RescueRequestStatus.ASSIGNED);
        when(rescueRequestRepository.findById(100L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> rescueRequestService.assign(100L, 200L))
                .isInstanceOf(RescueRequestValidationException.class);
    }

    @Test
    void complete_assignedRequest_freesTeamAndCompletesRequest() {
        RescueTeam team = teamOf(RescueTeamStatus.DISPATCHED);
        RescueRequest request = requestOf(RescueRequestStatus.ASSIGNED);
        request.setAssignedTeam(team);

        when(rescueRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(rescueTeamRepository.save(any(RescueTeam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rescueRequestRepository.save(any(RescueRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RescueRequest completed = rescueRequestService.complete(100L);

        assertThat(completed.getStatus()).isEqualTo(RescueRequestStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.AVAILABLE);
    }

    @Test
    void complete_onPendingRequest_throwsValidationException() {
        RescueRequest request = requestOf(RescueRequestStatus.PENDING);
        when(rescueRequestRepository.findById(100L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> rescueRequestService.complete(100L))
                .isInstanceOf(RescueRequestValidationException.class);
    }

    @Test
    void cancel_assignedRequest_freesTeamAndCancelsRequest() {
        RescueTeam team = teamOf(RescueTeamStatus.DISPATCHED);
        RescueRequest request = requestOf(RescueRequestStatus.ASSIGNED);
        request.setAssignedTeam(team);

        when(rescueRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(rescueTeamRepository.save(any(RescueTeam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rescueRequestRepository.save(any(RescueRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RescueRequest cancelled = rescueRequestService.cancel(100L);

        assertThat(cancelled.getStatus()).isEqualTo(RescueRequestStatus.CANCELLED);
        assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.AVAILABLE);
    }

    @Test
    void cancel_completedRequest_throwsValidationException() {
        RescueRequest request = requestOf(RescueRequestStatus.COMPLETED);
        when(rescueRequestRepository.findById(100L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> rescueRequestService.cancel(100L))
                .isInstanceOf(RescueRequestValidationException.class);
    }

    private RescueRequest requestOf(RescueRequestStatus status) {
        RescueRequest request = new RescueRequest();
        request.setId(100L);
        request.setDistrict(district);
        request.setRequesterName("Jane Doe");
        request.setRequesterPhone("0771234567");
        request.setDescription("Trapped by rising floodwater");
        request.setPriority(Severity.HIGH);
        request.setStatus(status);
        return request;
    }

    private RescueTeam teamOf(RescueTeamStatus status) {
        RescueTeam team = new RescueTeam();
        team.setId(200L);
        team.setName("Alpha Team");
        team.setDistrict(district);
        team.setOrganization(organization);
        team.setStatus(status);
        return team;
    }
}
