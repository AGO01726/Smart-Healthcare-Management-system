package com.smarthealth.hms.domain;

public enum AppointmentStatus {
    SCHEDULED("Scheduled", "bg-primary"),
    COMPLETED("Completed", "bg-success"),
    CANCELLED("Cancelled", "bg-secondary");

    private final String label;
    private final String badgeClass;

    AppointmentStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
