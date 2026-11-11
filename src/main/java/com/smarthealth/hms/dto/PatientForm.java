package com.smarthealth.hms.dto;

import com.smarthealth.hms.domain.Gender;
import com.smarthealth.hms.domain.Patient;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class PatientForm {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "Maximum 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Maximum 50 characters")
    private String lastName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[0-9 \\-]{7,15}$", message = "Enter a valid phone number (7-15 digits)")
    private String phone;

    @Email(message = "Enter a valid email address")
    @Size(max = 100, message = "Maximum 100 characters")
    private String email;

    @Size(max = 255, message = "Maximum 255 characters")
    private String address;

    @Pattern(regexp = "^((A|B|AB|O)[+-])?$", message = "Use a valid blood group such as A+, O-, AB+")
    private String bloodGroup;

    @Size(max = 255, message = "Maximum 255 characters")
    private String allergies;

    @Size(max = 100, message = "Maximum 100 characters")
    private String emergencyContact;

    /** Optional patient-portal login (only used when registering). */
    @Pattern(regexp = "^([A-Za-z0-9._-]{4,50})?$", message = "Username: 4-50 letters, digits, '.', '_' or '-'")
    private String username;

    @Size(max = 64, message = "Maximum 64 characters")
    private String password;

    public static PatientForm from(Patient p) {
        PatientForm f = new PatientForm();
        f.firstName = p.getFirstName();
        f.lastName = p.getLastName();
        f.gender = p.getGender();
        f.dateOfBirth = p.getDateOfBirth();
        f.phone = p.getPhone();
        f.email = p.getEmail();
        f.address = p.getAddress();
        f.bloodGroup = p.getBloodGroup();
        f.allergies = p.getAllergies();
        f.emergencyContact = p.getEmergencyContact();
        return f;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
