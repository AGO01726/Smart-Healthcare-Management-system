package com.smarthealth.hms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DiagnosticRequestForm {

    @NotNull(message = "Please select a patient")
    private Long patientId;

    /** Only used when an administrator creates the request; doctors always request as themselves. */
    private Long doctorId;

    @NotBlank(message = "Test name is required")
    @Size(max = 100, message = "Maximum 100 characters")
    private String testName;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Maximum 50 characters")
    private String category;

    @Size(max = 500, message = "Maximum 500 characters")
    private String clinicalNotes;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }
}
