package com.smarthealth.hms.domain;

import java.util.List;

/** Life-cycle of a diagnostic test request. COMPLETED is reached only by entering a result. */
public enum TestStatus {
    REQUESTED("Requested", "bg-warning text-dark"),
    SAMPLE_COLLECTED("Sample collected", "bg-info text-dark"),
    IN_PROGRESS("In progress", "bg-primary"),
    COMPLETED("Completed", "bg-success"),
    CANCELLED("Cancelled", "bg-secondary");

    private final String label;
    private final String badgeClass;

    TestStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    /** Statuses that can be set manually from this status (COMPLETED is set by entering a result). */
    public List<TestStatus> nextStatuses() {
        return switch (this) {
            case REQUESTED -> List.of(SAMPLE_COLLECTED, CANCELLED);
            case SAMPLE_COLLECTED -> List.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> List.of(CANCELLED);
            case COMPLETED, CANCELLED -> List.of();
        };
    }

    /** True while the request is still open (not completed and not cancelled). */
    public boolean isOpen() {
        return this != COMPLETED && this != CANCELLED;
    }
}
