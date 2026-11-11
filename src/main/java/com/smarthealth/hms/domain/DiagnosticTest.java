package com.smarthealth.hms.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "diagnostic_tests")
public class DiagnosticTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_doctor_id", nullable = false)
    private Doctor requestedBy;

    @Column(name = "test_name", nullable = false, length = 100)
    private String testName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "clinical_notes", length = 500)
    private String clinicalNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestStatus status = TestStatus.REQUESTED;

    @Column(name = "result_summary", length = 2000)
    private String resultSummary;

    @Column(name = "result_entered_by", length = 100)
    private String resultEnteredBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Doctor getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Doctor requestedBy) { this.requestedBy = requestedBy; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }
    public TestStatus getStatus() { return status; }
    public void setStatus(TestStatus status) { this.status = status; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public String getResultEnteredBy() { return resultEnteredBy; }
    public void setResultEnteredBy(String resultEnteredBy) { this.resultEnteredBy = resultEnteredBy; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
