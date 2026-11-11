package com.smarthealth.hms.dto;

import com.smarthealth.hms.domain.Staff;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class StaffForm {

    private Long id;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Maximum 100 characters")
    private String fullName;

    @NotBlank(message = "Designation is required")
    @Size(max = 100, message = "Maximum 100 characters")
    private String designation;

    private Long departmentId;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[0-9 \\-]{7,15}$", message = "Enter a valid phone number (7-15 digits)")
    private String phone;

    @Email(message = "Enter a valid email address")
    @Size(max = 100, message = "Maximum 100 characters")
    private String email;

    @NotNull(message = "Joining date is required")
    @PastOrPresent(message = "Joining date cannot be in the future")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate joinedDate = LocalDate.now();

    private boolean active = true;

    public static StaffForm from(Staff s) {
        StaffForm f = new StaffForm();
        f.id = s.getId();
        f.fullName = s.getFullName();
        f.designation = s.getDesignation();
        f.departmentId = s.getDepartment() == null ? null : s.getDepartment().getId();
        f.phone = s.getPhone();
        f.email = s.getEmail();
        f.joinedDate = s.getJoinedDate();
        f.active = s.isActive();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getJoinedDate() { return joinedDate; }
    public void setJoinedDate(LocalDate joinedDate) { this.joinedDate = joinedDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
