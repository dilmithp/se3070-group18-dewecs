package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.ReportDetails;
import com.group18.dewecs.domain.ReportKpi;
import com.group18.dewecs.dto.ReportAnalyticsView;
import com.group18.dewecs.dto.ReportAnalyticsView.Bar;
import com.group18.dewecs.dto.ReportAnalyticsView.Kpi;
import com.group18.dewecs.dto.ReportAnalyticsView.NeedBar;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ReportAnalyticsMapper {

    private static final List<String> TILES = List.of("relief_per_capita", "response_latency", "delivery_rate",
            "units_dispatched", "units_delivered", "units_damaged", "needs_met");

    public ReportAnalyticsView toView(Optional<ReportDetails> details, List<ReportKpi> kpis) {
        if (details.isEmpty()) {
            return new ReportAnalyticsView(false, null, null, null, false, null, null, null, null, false, List.of(),
                    List.of(), List.of());
        }
        ReportDetails d = details.get();
        boolean provisional = Boolean.TRUE.equals(d.getProvisional());
        List<Kpi> tiles = kpis.stream().filter(k -> TILES.contains(k.getKpiKey()))
                .map(k -> new Kpi(k.getLabel(), value(k), k.getDetail())).toList();
        List<Bar> shares = kpis.stream().filter(k -> k.getKpiKey().equals("share"))
                .map(k -> new Bar(k.getLabel(), value(k), percent(k.getValue(), 100))).toList();
        return new ReportAnalyticsView(true, d.getWindowFrom(), d.getWindowTo(),
                d.getDonor() == null ? null : d.getDonor().getName(), provisional, d.getProvisionalReason(),
                d.getDataGaps(), d.getApprovedBy(), d.getApprovedAt(), !provisional && d.getApprovedAt() == null, tiles,
                shares, needBars(kpis));
    }

    private List<NeedBar> needBars(List<ReportKpi> kpis) {
        double max = kpis.stream().filter(k -> k.getKpiKey().equals("need") || k.getKpiKey().equals("distributed"))
                .mapToDouble(k -> k.getValue() == null ? 0 : k.getValue()).max().orElse(0);
        List<NeedBar> bars = new ArrayList<>();
        for (ReportKpi need : kpis.stream().filter(k -> k.getKpiKey().equals("need")).toList()) {
            double dist = kpis.stream().filter(k -> k.getKpiKey().equals("distributed") && k.getLabel().equals(need.getLabel()))
                    .mapToDouble(k -> k.getValue() == null ? 0 : k.getValue()).findFirst().orElse(0);
            double n = need.getValue() == null ? 0 : need.getValue();
            bars.add(new NeedBar(need.getLabel(), n, dist, percent(n, max), percent(dist, max)));
        }
        return bars;
    }

    private String value(ReportKpi k) {
        if (k.getValue() == null) {
            return "n/a";
        }
        double v = k.getValue();
        String number = v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
        return k.getUnit() == null ? number : number + " " + k.getUnit();
    }

    private int percent(Double value, double max) {
        if (value == null || max <= 0) {
            return 0;
        }
        return (int) Math.max(0, Math.min(100, Math.round(value * 100 / max)));
    }
}
