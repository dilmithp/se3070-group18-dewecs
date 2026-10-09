package com.group18.dewecs.dto;

import java.util.List;

/** What the warnings page needs to draw its built-in map: the island outline and one marker per warned district. */
public class WarningMapView {

    private final int width;
    private final int height;
    private final String outlinePath;
    private final List<Marker> markers;

    public WarningMapView(int width, int height, String outlinePath, List<Marker> markers) {
        this.width = width;
        this.height = height;
        this.outlinePath = outlinePath;
        this.markers = markers;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getOutlinePath() {
        return outlinePath;
    }

    public List<Marker> getMarkers() {
        return markers;
    }

    /** One district with live warnings; severity is the highest among them. */
    public static class Marker {

        private final Long districtId;
        private final String districtName;
        private final long x;
        private final long y;
        private final String severity;
        private final int warningCount;

        public Marker(Long districtId, String districtName, long x, long y, String severity, int warningCount) {
            this.districtId = districtId;
            this.districtName = districtName;
            this.x = x;
            this.y = y;
            this.severity = severity;
            this.warningCount = warningCount;
        }

        public Long getDistrictId() {
            return districtId;
        }

        public String getDistrictName() {
            return districtName;
        }

        public long getX() {
            return x;
        }

        public long getY() {
            return y;
        }

        public String getSeverity() {
            return severity;
        }

        public int getWarningCount() {
            return warningCount;
        }

        public int getRadius() {
            return 8 + 3 * Math.min(warningCount, 4);
        }
    }
}
