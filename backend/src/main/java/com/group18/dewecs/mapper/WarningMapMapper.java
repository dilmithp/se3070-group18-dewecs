package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.dto.WarningMapView;
import com.group18.dewecs.dto.WarningMapView.Marker;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the markers of the active-warnings map: one per district that has a live warning, coloured by the worst one. */
@Component
public class WarningMapMapper {

    public WarningMapView toView(List<Warning> warnings) {
        Map<Long, Severity> worst = new LinkedHashMap<>();
        Map<Long, Integer> counts = new LinkedHashMap<>();
        Map<Long, District> districts = new LinkedHashMap<>();
        for (Warning warning : warnings) {
            if (warning.getStatus() != WarningStatus.ISSUED && warning.getStatus() != WarningStatus.UPDATED) {
                continue;
            }
            for (District district : warning.effectiveDistricts()) {
                districts.putIfAbsent(district.getId(), district);
                worst.merge(district.getId(), warning.getSeverity(), (a, b) -> a.ordinal() >= b.ordinal() ? a : b);
                counts.merge(district.getId(), 1, Integer::sum);
            }
        }
        List<Marker> markers = districts.values().stream()
                .sorted(Comparator.comparing(District::getName))
                .map(d -> SriLankaMap.positionOf(d.getName())
                        .map(p -> new Marker(d.getId(), d.getName(), Math.round(p[0]), Math.round(p[1]),
                                worst.get(d.getId()).name(), counts.get(d.getId())))
                        .orElse(null))
                .filter(m -> m != null)
                .toList();
        return new WarningMapView(SriLankaMap.WIDTH, SriLankaMap.HEIGHT, SriLankaMap.outlinePath(), markers);
    }
}
