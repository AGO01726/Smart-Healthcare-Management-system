package com.smarthealth.hms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DiagnosticResultForm {

    @NotBlank(message = "Result is required")
    @Size(max = 2000, message = "Maximum 2000 characters")
    private String resultSummary;

    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
}
