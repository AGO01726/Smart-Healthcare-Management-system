package com.smarthealth.hms.dto;

import com.smarthealth.hms.domain.Doctor;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.TreeSet;

public class DoctorForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Maximum 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 100, message = "Maximum 100 characters")
    private String email;

    /** Login credentials: required only when creating a doctor. */
    @Pattern(regexp = "^([A-Za-z0-9._-]{4,50})?$", message = "Username: 4-50 letters, digits, '.', '_' or '-'")
    private String username;

    @Size(max = 64, message = "Maximum 64 characters")
    private String password;

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotBlank(message = "Specialization is required")
    @Size(max = 100, message = "Maximum 100 characters")
    private String specialization;

    @Size(max = 150, message = "Maximum 150 characters")
    private String qualification;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[0-9 \\-]{7,15}$", message = "Enter a valid phone number (7-15 digits)")
    private String phone;

    @NotNull(message = "Experience is required")
    @Min(value = 0, message = "Must be 0 or more")
    @Max(value = 60, message = "Must be 60 or less")
    private Integer experienceYears = 0;

    @NotNull(message = "Consultation fee is required")
    @DecimalMin(value = "0.00", message = "Must be 0 or more")
    @Digits(integer = 6, fraction = 2, message = "Use at most 6 digits and 2 decimals")
    private BigDecimal consultationFee = BigDecimal.ZERO;

    private Set<DayOfWeek> availableDays = new TreeSet<>();

    @NotNull(message = "Start time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime availableFrom = LocalTime.of(9, 0);

    @NotNull(message = "End time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime availableTo = LocalTime.of(17, 0);

    private boolean active = true;

    public static DoctorForm from(Doctor d) {
        DoctorForm f = new DoctorForm();
        f.fullName = d.getUser().getFullName();
        f.email = d.getUser().getEmail();
        f.departmentId = d.getDepartment().getId();
        f.specialization = d.getSpecialization();
        f.qualification = d.getQualification();
        f.phone = d.getPhone();
        f.experienceYears = d.getExperienceYears();
        f.consultationFee = d.getConsultationFee();
        f.availableDays = new TreeSet<>(d.getAvailableDays());
        f.availableFrom = d.getAvailableFrom();
        f.availableTo = d.getAvailableTo();
        f.active = d.isActive();
        return f;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }
    public Set<DayOfWeek> getAvailableDays() { return availableDays; }
    public void setAvailableDays(Set<DayOfWeek> availableDays) { this.availableDays = availableDays; }
    public LocalTime getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(LocalTime availableFrom) { this.availableFrom = availableFrom; }
    public LocalTime getAvailableTo() { return availableTo; }
    public void setAvailableTo(LocalTime availableTo) { this.availableTo = availableTo; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
