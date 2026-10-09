package com.group18.dewecs.mapper;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Geometry for the built-in warning map: a coarse outline of the island and the position of each district, projected
 * onto a fixed SVG canvas. Nothing is fetched from outside, so the map works offline. Districts are placed by name
 * (the districts table has no coordinates); a district that is not listed here simply gets no marker.
 */
public final class SriLankaMap {

    public static final int WIDTH = 420;
    public static final int HEIGHT = 680;

    private static final double LNG_MIN = 79.45;
    private static final double LAT_MAX = 10.0;
    private static final double SCALE = 160;

    /** Clockwise from the northern tip: down the east coast, up the west coast, back over Jaffna (lat, lng). */
    private static final double[][] OUTLINE = {
            {9.83, 80.23}, {9.55, 80.62}, {9.27, 80.81}, {9.10, 80.93},
            {8.93, 81.00}, {8.68, 81.12}, {8.57, 81.24}, {8.30, 81.32},
            {7.93, 81.57}, {7.72, 81.70}, {7.41, 81.83}, {7.10, 81.87},
            {6.84, 81.84}, {6.60, 81.72}, {6.29, 81.31}, {6.12, 81.12},
            {6.02, 80.80}, {5.92, 80.59}, {5.97, 80.43}, {6.03, 80.22},
            {6.14, 80.10}, {6.43, 79.99}, {6.58, 79.96}, {6.93, 79.85},
            {7.21, 79.83}, {7.58, 79.79}, {8.03, 79.83}, {8.23, 79.72},
            {8.50, 79.80}, {8.80, 79.88}, {9.10, 79.72}, {9.50, 79.92},
            {9.72, 79.88}, {9.82, 80.03}
    };

    private static final Map<String, double[]> DISTRICTS = Map.ofEntries(
            Map.entry("colombo", new double[] {6.93, 79.86}),
            Map.entry("gampaha", new double[] {7.09, 79.99}),
            Map.entry("kalutara", new double[] {6.59, 79.96}),
            Map.entry("kandy", new double[] {7.29, 80.63}),
            Map.entry("matale", new double[] {7.47, 80.62}),
            Map.entry("nuwara eliya", new double[] {6.97, 80.77}),
            Map.entry("galle", new double[] {6.05, 80.22}),
            Map.entry("matara", new double[] {5.95, 80.55}),
            Map.entry("hambantota", new double[] {6.12, 81.12}),
            Map.entry("jaffna", new double[] {9.66, 80.02}),
            Map.entry("kilinochchi", new double[] {9.39, 80.40}),
            Map.entry("mannar", new double[] {8.98, 79.91}),
            Map.entry("vavuniya", new double[] {8.75, 80.50}),
            Map.entry("mullaitivu", new double[] {9.27, 80.81}),
            Map.entry("batticaloa", new double[] {7.73, 81.69}),
            Map.entry("ampara", new double[] {7.30, 81.67}),
            Map.entry("trincomalee", new double[] {8.57, 81.23}),
            Map.entry("kurunegala", new double[] {7.49, 80.37}),
            Map.entry("puttalam", new double[] {8.03, 79.83}),
            Map.entry("anuradhapura", new double[] {8.31, 80.40}),
            Map.entry("polonnaruwa", new double[] {7.94, 81.00}),
            Map.entry("badulla", new double[] {6.99, 81.06}),
            Map.entry("monaragala", new double[] {6.87, 81.35}),
            Map.entry("ratnapura", new double[] {6.68, 80.40}),
            Map.entry("kegalle", new double[] {7.25, 80.35})
    );

    private SriLankaMap() {
    }

    /** The island as an SVG path ("M x y L x y ... Z"). */
    public static String outlinePath() {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < OUTLINE.length; i++) {
            double[] p = project(OUTLINE[i][0], OUTLINE[i][1]);
            path.append(i == 0 ? "M" : " L").append(round(p[0])).append(' ').append(round(p[1]));
        }
        return path.append(" Z").toString();
    }

    /** The marker position {x, y} of a district, or empty when the name is not a known district. */
    public static Optional<double[]> positionOf(String districtName) {
        if (districtName == null) {
            return Optional.empty();
        }
        double[] latLng = DISTRICTS.get(districtName.trim().toLowerCase(Locale.ROOT));
        return latLng == null ? Optional.empty() : Optional.of(project(latLng[0], latLng[1]));
    }

    public static List<String> knownDistricts() {
        return DISTRICTS.keySet().stream().sorted().toList();
    }

    private static double[] project(double lat, double lng) {
        return new double[] {(lng - LNG_MIN) * SCALE, (LAT_MAX - lat) * SCALE};
    }

    private static long round(double v) {
        return Math.round(v);
    }
}
