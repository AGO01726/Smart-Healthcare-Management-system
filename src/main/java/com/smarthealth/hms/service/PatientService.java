package com.smarthealth.hms.service;

import com.smarthealth.hms.domain.Doctor;
import com.smarthealth.hms.domain.Patient;
import com.smarthealth.hms.domain.Role;
import com.smarthealth.hms.domain.User;
import com.smarthealth.hms.dto.PatientForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.exception.ResourceNotFoundException;
import com.smarthealth.hms.repository.AppointmentRepository;
import com.smarthealth.hms.repository.DiagnosticTestRepository;
import com.smarthealth.hms.repository.DoctorRepository;
import com.smarthealth.hms.repository.PatientRepository;
import com.smarthealth.hms.security.AppUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final DiagnosticTestRepository tests;
    private final DoctorRepository doctors;
    private final UserService userService;

    public PatientService(PatientRepository patients, AppointmentRepository appointments,
                          DiagnosticTestRepository tests, DoctorRepository doctors, UserService userService) {
        this.patients = patients;
        this.appointments = appointments;
        this.tests = tests;
        this.doctors = doctors;
        this.userService = userService;
    }

    /** Search patients. Doctors only see patients they have treated or requested tests for. */
    @Transactional(readOnly = true)
    public Page<Patient> search(String q, int page, int size, AppUserDetails actor) {
        String term = q == null ? "" : q.trim();
        Pageable pageable = PageRequest.of(Math.max(page, 0), size);
        if (actor.getRole() == Role.DOCTOR) {
            return patients.searchForDoctor(term, doctorOf(actor).getId(), pageable);
        }
        return patients.search(term, pageable);
    }

    /** Patients that the actor may pick from a dropdown (capped at 500). */
    @Transactional(readOnly = true)
    public List<Patient> selectable(AppUserDetails actor) {
        return search("", 0, 500, actor).getContent();
    }

    @Transactional(readOnly = true)
    public Patient get(Long id) {
        return patients.findById(id).orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
    }

    @Transactional(readOnly = true)
    public Optional<Patient> findByUserId(Long userId) {
        return patients.findByUserId(userId);
    }

    /** Returns the patient only if the actor is allowed to see this record. */
    @Transactional(readOnly = true)
    public Patient getAuthorized(Long id, AppUserDetails actor) {
        Patient patient = get(id);
        switch (actor.getRole()) {
            case ADMIN, RECEPTIONIST -> {
                return patient;
            }
            case PATIENT -> {
                if (patient.getUser() != null && patient.getUser().getId().equals(actor.getUserId())) {
                    return patient;
                }
            }
            case DOCTOR -> {
                Long doctorId = doctorOf(actor).getId();
                if (appointments.existsByDoctorIdAndPatientId(doctorId, id)
                        || tests.existsByRequestedByIdAndPatientId(doctorId, id)) {
                    return patient;
                }
            }
        }
        throw new AccessDeniedException("You are not allowed to view this patient record.");
    }

    @Transactional
    public Patient register(PatientForm form) {
        User account = null;
        boolean hasUsername = StringUtils.hasText(form.getUsername());
        boolean hasPassword = StringUtils.hasText(form.getPassword());
        if (hasUsername || hasPassword) {
            if (!hasUsername || !hasPassword) {
                throw new BusinessRuleException("Provide both a username and a password to create a patient login.");
            }
            if (!StringUtils.hasText(form.getEmail())) {
                throw new BusinessRuleException("An email address is required to create a patient login.");
            }
            account = userService.createUser(form.getUsername(), form.getPassword(),
                    form.getFirstName().trim() + " " + form.getLastName().trim(), form.getEmail(), Role.PATIENT);
        }
        Patient patient = new Patient();
        apply(patient, form);
        patient.setUser(account);
        patient.setPatientCode("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        patient = patients.save(patient);
        patient.setPatientCode(String.format("PAT-%06d", patient.getId()));
        return patient;
    }

    @Transactional
    public Patient update(Long id, PatientForm form) {
        Patient patient = get(id);
        apply(patient, form);
        if (patient.getUser() != null) {
            userService.updateProfile(patient.getUser(), patient.getFullName(),
                    StringUtils.hasText(form.getEmail()) ? form.getEmail() : patient.getUser().getEmail());
        }
        return patient;
    }

    @Transactional
    public void delete(Long id) {
        Patient patient = get(id);
        if (appointments.countByPatientId(id) > 0 || tests.countByPatientId(id) > 0) {
            throw new BusinessRuleException(
                    "This patient has appointments or diagnostic records and cannot be deleted.");
        }
        User account = patient.getUser();
        patients.delete(patient);
        if (account != null) {
            userService.delete(account);
        }
    }

    private Doctor doctorOf(AppUserDetails actor) {
        return doctors.findByUserId(actor.getUserId())
                .orElseThrow(() -> new AccessDeniedException("No doctor profile is linked to this account."));
    }

    private void apply(Patient p, PatientForm f) {
        p.setFirstName(f.getFirstName().trim());
        p.setLastName(f.getLastName().trim());
        p.setGender(f.getGender());
        p.setDateOfBirth(f.getDateOfBirth());
        p.setPhone(f.getPhone().trim());
        p.setEmail(blankToNull(f.getEmail()));
        p.setAddress(blankToNull(f.getAddress()));
        p.setBloodGroup(blankToNull(f.getBloodGroup()));
        p.setAllergies(blankToNull(f.getAllergies()));
        p.setEmergencyContact(blankToNull(f.getEmergencyContact()));
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
