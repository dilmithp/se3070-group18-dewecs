package com.group18.dewecs.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The read models of the relief command center and the consignment pages. Plain classes with public final fields
 * (read by the Thymeleaf pages and by Jackson for the JSON answers); no entity reaches a view.
 */
public final class LogisticsViews {

    private LogisticsViews() {
    }

    /** One unit of stock in the unified inventory. */
    public static class StockLine {
        public final Long resourceId;
        public final String organizationName;
        public final String resourceName;
        public final String type;
        public final int quantity;
        public final String unit;
        public final boolean low;

        public StockLine(Long resourceId, String organizationName, String resourceName, String type, int quantity,
                         String unit, boolean low) {
            this.resourceId = resourceId;
            this.organizationName = organizationName;
            this.resourceName = resourceName;
            this.type = type;
            this.quantity = quantity;
            this.unit = unit;
            this.low = low;
        }
    }

    /** One request for supplies from a shelter. */
    public static class NeedLine {
        public final Long id;
        public final Long shelterId;
        public final String shelterName;
        public final String resourceType;
        public final int needed;
        public final int allocated;
        public final int remaining;
        public final int percent;
        public final boolean urgent;
        public final String status;
        public final String note;
        public final LocalDateTime requestedAt;

        public NeedLine(Long id, Long shelterId, String shelterName, String resourceType, int needed, int allocated,
                        int remaining, int percent, boolean urgent, String status, String note,
                        LocalDateTime requestedAt) {
            this.id = id;
            this.shelterId = shelterId;
            this.shelterName = shelterName;
            this.resourceType = resourceType;
            this.needed = needed;
            this.allocated = allocated;
            this.remaining = remaining;
            this.percent = percent;
            this.urgent = urgent;
            this.status = status;
            this.note = note;
            this.requestedAt = requestedAt;
        }
    }

    /** A recent consignment in the command center list. */
    public static class ConsignmentLine {
        public final Long id;
        public final String reference;
        public final String organizationName;
        public final String shelterName;
        public final String itemName;
        public final int quantity;
        public final String unit;
        public final String status;
        public final boolean damaged;
        public final String parentReference;
        public final LocalDateTime dispatchedAt;

        public ConsignmentLine(Long id, String reference, String organizationName, String shelterName,
                               String itemName, int quantity, String unit, String status, boolean damaged,
                               String parentReference, LocalDateTime dispatchedAt) {
            this.id = id;
            this.reference = reference;
            this.organizationName = organizationName;
            this.shelterName = shelterName;
            this.itemName = itemName;
            this.quantity = quantity;
            this.unit = unit;
            this.status = status;
            this.damaged = damaged;
            this.parentReference = parentReference;
            this.dispatchedAt = dispatchedAt;
        }
    }

    /** What the screen shows for one proposed part of a split allocation. */
    public static class SplitLine {
        public final Long resourceId;
        public final String organizationName;
        public final String resourceName;
        public final int quantity;
        public final int stock;
        public final int percent;

        public SplitLine(Long resourceId, String organizationName, String resourceName, int quantity, int stock,
                         int percent) {
            this.resourceId = resourceId;
            this.organizationName = organizationName;
            this.resourceName = resourceName;
            this.quantity = quantity;
            this.stock = stock;
            this.percent = percent;
        }
    }

    /** The command center for one incident and district. */
    public static class CommandCenterView {
        public final Long hazardEventId;
        public final Long districtId;
        public final String districtName;
        public final String incidentLabel;
        public final List<StockLine> inventory;
        public final List<NeedLine> needs;
        public final List<ShelterResponse> shelters;
        public final List<ConsignmentLine> recent;
        public final int fulfilmentPercent;
        public final boolean hasNeeds;

        public CommandCenterView(Long hazardEventId, Long districtId, String districtName, String incidentLabel,
                                 List<StockLine> inventory, List<NeedLine> needs, List<ShelterResponse> shelters,
                                 List<ConsignmentLine> recent, int fulfilmentPercent, boolean hasNeeds) {
            this.hazardEventId = hazardEventId;
            this.districtId = districtId;
            this.districtName = districtName;
            this.incidentLabel = incidentLabel;
            this.inventory = inventory;
            this.needs = needs;
            this.shelters = shelters;
            this.recent = recent;
            this.fulfilmentPercent = fulfilmentPercent;
            this.hasNeeds = hasNeeds;
        }
    }

    /** One line of the logistics audit log. */
    public static class EventLine {
        public final LocalDateTime time;
        public final String type;
        public final String detail;
        public final String recordedBy;

        public EventLine(LocalDateTime time, String type, String detail, String recordedBy) {
            this.time = time;
            this.type = type;
            this.detail = detail;
            this.recordedBy = recordedBy;
        }
    }

    /** The extra record of a consignment: convoy, notes, handover. */
    public static class DetailsView {
        public final String incidentLabel;
        public final String handlingNotes;
        public final String vehicle;
        public final String driverName;
        public final String driverPhone;
        public final LocalDateTime expectedArrival;
        public final String parentReference;
        public final String needLabel;
        public final String receivedBy;
        public final Integer receivedQuantity;
        public final Integer damagedQuantity;
        public final String handoverNote;
        public final String handoverPhotoUrl;
        public final LocalDateTime handedOverAt;
        public final List<String> linkedReferences;

        public DetailsView(String incidentLabel, String handlingNotes, String vehicle, String driverName,
                           String driverPhone, LocalDateTime expectedArrival, String parentReference,
                           String needLabel, String receivedBy, Integer receivedQuantity, Integer damagedQuantity,
                           String handoverNote, String handoverPhotoUrl, LocalDateTime handedOverAt,
                           List<String> linkedReferences) {
            this.incidentLabel = incidentLabel;
            this.handlingNotes = handlingNotes;
            this.vehicle = vehicle;
            this.driverName = driverName;
            this.driverPhone = driverPhone;
            this.expectedArrival = expectedArrival;
            this.parentReference = parentReference;
            this.needLabel = needLabel;
            this.receivedBy = receivedBy;
            this.receivedQuantity = receivedQuantity;
            this.damagedQuantity = damagedQuantity;
            this.handoverNote = handoverNote;
            this.handoverPhotoUrl = handoverPhotoUrl;
            this.handedOverAt = handedOverAt;
            this.linkedReferences = linkedReferences;
        }
    }

    /** A message to an agency. */
    public static class NotificationLine {
        public final LocalDateTime time;
        public final String organizationName;
        public final String reference;
        public final String message;

        public NotificationLine(LocalDateTime time, String organizationName, String reference, String message) {
            this.time = time;
            this.organizationName = organizationName;
            this.reference = reference;
            this.message = message;
        }
    }
}
