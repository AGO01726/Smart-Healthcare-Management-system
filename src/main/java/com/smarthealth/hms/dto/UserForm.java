package com.smarthealth.hms.dto;

import com.smarthealth.hms.domain.Role;
import jakarta.validation.constraints.*;

/** Admin form for creating back-office accounts (administrators and receptionists). */
public class UserForm {

    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[A-Za-z0-9._-]{4,50}$", message = "Username: 4-50 letters, digits, '.', '_' or '-'")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 64, message = "Maximum 64 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Maximum 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 100, message = "Maximum 100 characters")
    private String email;

    @NotNull(message = "Role is required")
    private Role role = Role.RECEPTIONIST;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
