package com.group18.dewecs.repository;

import com.group18.dewecs.domain.Citizen;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.RescueRequest;
import com.group18.dewecs.domain.RescueRequestStatus;
import com.group18.dewecs.domain.Severity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** GPS coordinates must survive a save and reload with 7 decimals (numeric(10,7)), not be rounded to 2. */
@DataJpaTest
@ActiveProfiles("test")
class GpsPrecisionTest {

    private static final BigDecimal LAT = new BigDecimal("6.9271234");
    private static final BigDecimal LNG = new BigDecimal("79.8612345");

    @Autowired
    private TestEntityManager em;
    @Autowired
    private EntityManager entityManager;

    @Test
    void groundReportAndRescueRequestKeepSevenDecimalsAfterReload() {
        District district = new District();
        district.setName("Colombo");
        em.persist(district);

        Citizen citizen = new Citizen();
        citizen.setFullName("Nimal Perera");
        citizen.setNic("199012345678");
        citizen.setDistrict(district);
        em.persist(citizen);

        GroundReport report = new GroundReport();
        report.setReportedBy(citizen);
        report.setDistrict(district);
        report.setCategory(HazardType.FLOOD);
        report.setStatus(GroundReportStatus.PENDING_REVIEW);
        report.setGpsLat(LAT);
        report.setGpsLng(LNG);
        report.setSubmittedAt(LocalDateTime.now());
        em.persist(report);

        RescueRequest request = new RescueRequest();
        request.setDistrict(district);
        request.setRequesterName("Kamal");
        request.setRequesterPhone("0771234567");
        request.setDescription("help");
        request.setPriority(Severity.HIGH);
        request.setStatus(RescueRequestStatus.PENDING);
        request.setGpsLat(LAT);
        request.setGpsLng(LNG);
        request.setSubmittedAt(LocalDateTime.now());
        em.persist(request);

        em.flush();
        em.clear();

        GroundReport reloadedReport = em.find(GroundReport.class, report.getId());
        RescueRequest reloadedRequest = em.find(RescueRequest.class, request.getId());

        printColumnDefinitions();
        System.out.println("GPS-CHECK report   lat=" + reloadedReport.getGpsLat() + " lng=" + reloadedReport.getGpsLng());
        System.out.println("GPS-CHECK request  lat=" + reloadedRequest.getGpsLat() + " lng=" + reloadedRequest.getGpsLng());

        assertThat(reloadedReport.getGpsLat()).isEqualByComparingTo(LAT);
        assertThat(reloadedReport.getGpsLng()).isEqualByComparingTo(LNG);
        assertThat(reloadedRequest.getGpsLat()).isEqualByComparingTo(LAT);
        assertThat(reloadedRequest.getGpsLng()).isEqualByComparingTo(LNG);
    }

    @Test
    void gpsColumnsAreNumeric10Comma7() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT TABLE_NAME, COLUMN_NAME, NUMERIC_PRECISION, NUMERIC_SCALE FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE UPPER(TABLE_NAME) IN ('GROUND_REPORTS','RESCUE_REQUESTS') "
                        + "AND UPPER(COLUMN_NAME) IN ('GPS_LAT','GPS_LNG')").getResultList();
        assertThat(rows).hasSize(4);
        for (Object[] row : rows) {
            assertThat(((Number) row[2]).intValue()).as(row[0] + "." + row[1] + " precision").isEqualTo(10);
            assertThat(((Number) row[3]).intValue()).as(row[0] + "." + row[1] + " scale").isEqualTo(7);
        }
    }

    private void printColumnDefinitions() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT TABLE_NAME, COLUMN_NAME, NUMERIC_PRECISION, NUMERIC_SCALE FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE UPPER(TABLE_NAME) IN ('GROUND_REPORTS','RESCUE_REQUESTS') "
                        + "AND UPPER(COLUMN_NAME) IN ('GPS_LAT','GPS_LNG') ORDER BY 1, 2").getResultList();
        for (Object[] row : rows) {
            System.out.println("GPS-CHECK column " + row[0] + "." + row[1] + " precision=" + row[2] + " scale=" + row[3]);
        }
    }
}
