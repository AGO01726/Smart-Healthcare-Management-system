package com.smarthealth.hms.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentForm {

    /** Required for staff; ignored for patients (their own record is used). */
    private Long patientId;

    @NotNull(message = "Please select a doctor")
    private Long doctorId;

    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Date cannot be in the past")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @NotNull(message = "Time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotBlank(message = "Reason for visit is required")
    @Size(max = 255, message = "Maximum 255 characters")
    private String reason;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
