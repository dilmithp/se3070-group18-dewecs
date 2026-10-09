package com.group18.dewecs.config;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.RiverBasin;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.repository.AlertDeliveryLogRepository;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.RiverBasinRepository;
import com.group18.dewecs.repository.WarningRepository;
import com.group18.dewecs.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seeded demo data of the local profile: its own in-memory database (dewecs_local), so it never leaks into the
 * other tests. It guards the ids and counts that doc/DEMO_SCRIPT.md quotes.
 */
@SpringBootTest
@ActiveProfiles({"test", "local"})
class DemoDataLoaderTest {

    @Autowired
    private DistrictRepository districts;
    @Autowired
    private RiverBasinRepository basins;
    @Autowired
    private WarningRepository warnings;
    @Autowired
    private AlertDeliveryLogRepository logs;
    @Autowired
    private DashboardService dashboard;

    @Test
    void theDemoHasTheOriginalDistrictsAndTheNewBasinDistrictLast() {
        assertThat(districts.findAll()).extracting(District::getId, District::getName)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(1L, "Colombo"),
                        org.assertj.core.groups.Tuple.tuple(2L, "Galle"),
                        org.assertj.core.groups.Tuple.tuple(3L, "Kandy"),
                        org.assertj.core.groups.Tuple.tuple(4L, "Gampaha"));
    }

    @Test
    void theOriginalThreeWarningsKeepTheirIdsAndStatusesAndTheNewDraftComesAfterThem() {
        assertThat(warnings.findById(1L).orElseThrow().getStatus()).isEqualTo(WarningStatus.DRAFT);
        assertThat(warnings.findById(2L).orElseThrow().getStatus()).isEqualTo(WarningStatus.ISSUED);
        assertThat(warnings.findById(3L).orElseThrow().getStatus()).isEqualTo(WarningStatus.ISSUED);
        Warning basinDraft = warnings.findById(4L).orElseThrow();
        assertThat(basinDraft.getStatus()).isEqualTo(WarningStatus.DRAFT);
        assertThat(basinDraft.effectiveDistricts()).extracting(District::getName)
                .containsExactlyInAnyOrder("Colombo", "Gampaha");
    }

    @Test
    void theDashboardNumbersOfTheDemoScriptAreUnchanged() {
        assertThat(dashboard.getSummary().getOpenWarnings()).isEqualTo(2);
    }

    @Test
    void threeBasinsExistAndKelaniRunsThroughColomboAndGampaha() {
        assertThat(basins.findAllByOrderByNameAsc()).extracting(RiverBasin::getName)
                .containsExactly("Gin Ganga", "Kelani Ganga", "Mahaweli Ganga");
        RiverBasin kelani = basins.findAllByOrderByNameAsc().get(1);
        assertThat(kelani.getDistricts()).extracting(District::getName).containsExactlyInAnyOrder("Colombo", "Gampaha");
    }

    @Test
    void theAlreadyPublishedDemoWarningsHaveADeliveryLog() {
        // Warning 2 went out on SMS and siren, warning 3 on email; all simulated deliveries succeed.
        assertThat(logs.findByWarning_IdOrderByCreatedAtAscIdAsc(2L)).hasSize(2);
        assertThat(logs.findByWarning_IdOrderByCreatedAtAscIdAsc(3L)).hasSize(1);
        assertThat(logs.findByWarning_IdOrderByCreatedAtAscIdAsc(1L)).isEmpty();
        assertThat(logs.findByWarning_IdOrderByCreatedAtAscIdAsc(4L)).isEmpty();
    }
}
