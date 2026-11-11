package com.smarthealth.hms.service;

import com.smarthealth.hms.domain.DiagnosticTest;
import com.smarthealth.hms.domain.Doctor;
import com.smarthealth.hms.domain.Patient;
import com.smarthealth.hms.domain.Role;
import com.smarthealth.hms.domain.TestStatus;
import com.smarthealth.hms.dto.DiagnosticRequestForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.exception.ResourceNotFoundException;
import com.smarthealth.hms.repository.AppointmentRepository;
import com.smarthealth.hms.repository.DiagnosticSpecs;
import com.smarthealth.hms.repository.DiagnosticTestRepository;
import com.smarthealth.hms.repository.DoctorRepository;
import com.smarthealth.hms.repository.PatientRepository;
import com.smarthealth.hms.security.AppUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Access rules for diagnostics:
 * <ul>
 *   <li>Admin: everything.</li>
 *   <li>Doctor: only tests they requested (create, update status, enter results).</li>
 *   <li>Receptionist (lab desk): can see the list and update the status, but cannot read result text.</li>
 *   <li>Patient: only their own tests; result text is visible once the test is COMPLETED.</li>
 * </ul>
 */
@Service
public class DiagnosticService {

    private final DiagnosticTestRepository tests;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final Clock clock;

    public DiagnosticService(DiagnosticTestRepository tests, PatientRepository patients, DoctorRepository doctors,
                             AppointmentRepository appointments, Clock clock) {
        this.tests = tests;
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<DiagnosticTest> search(AppUserDetails actor, String q, TestStatus status, int page, int size) {
        Long doctorFilter = null;
        Long patientFilter = null;
        if (actor.getRole() == Role.DOCTOR) {
            doctorFilter = doctorOf(actor).getId();
        } else if (actor.getRole() == Role.PATIENT) {
            Optional<Patient> own = patients.findByUserId(actor.getUserId());
            if (own.isEmpty()) {
                return new PageImpl<>(List.of(), PageRequest.of(0, size), 0);
            }
            patientFilter = own.get().getId();
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Order.desc("requestedAt")));
        return tests.findAll(DiagnosticSpecs.filter(q, status, doctorFilter, patientFilter), pageable);
    }

    @Transactional(readOnly = true)
    public List<DiagnosticTest> listForPatient(Long patientId, AppUserDetails actor) {
        Long doctorFilter = actor.getRole() == Role.DOCTOR ? doctorOf(actor).getId() : null;
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("requestedAt")));
        return tests.findAll(DiagnosticSpecs.filter(null, null, doctorFilter, patientId), pageable).getContent();
    }

    @Transactional(readOnly = true)
    public DiagnosticTest getAuthorized(Long id, AppUserDetails actor) {
        DiagnosticTest test = tests.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic test not found"));
        boolean allowed = switch (actor.getRole()) {
            case ADMIN, RECEPTIONIST -> true;
            case DOCTOR -> isOwnDoctor(test, actor);
            case PATIENT -> isOwnPatient(test, actor);
        };
        if (!allowed) {
            throw new AccessDeniedException("You are not allowed to access this diagnostic record.");
        }
        return test;
    }

    /** Whether the actor may read the result text of this test. */
    public boolean canViewResult(DiagnosticTest test, AppUserDetails actor) {
        return switch (actor.getRole()) {
            case ADMIN -> true;
            case DOCTOR -> isOwnDoctor(test, actor);
            case PATIENT -> isOwnPatient(test, actor) && test.getStatus() == TestStatus.COMPLETED;
            case RECEPTIONIST -> false;
        };
    }

    /** Whether the actor may type in a result for this test. */
    public boolean canEnterResult(DiagnosticTest test, AppUserDetails actor) {
        boolean role = actor.getRole() == Role.ADMIN || (actor.getRole() == Role.DOCTOR && isOwnDoctor(test, actor));
        return role && test.getStatus().isOpen();
    }

    @Transactional
    public DiagnosticTest request(DiagnosticRequestForm form, AppUserDetails actor) {
        Doctor doctor;
        if (actor.getRole() == Role.DOCTOR) {
            doctor = doctorOf(actor);
        } else if (actor.getRole() == Role.ADMIN) {
            if (form.getDoctorId() == null) {
                throw new BusinessRuleException("Please select the requesting doctor.");
            }
            doctor = doctors.findById(form.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        } else {
            throw new AccessDeniedException("Only doctors and administrators can request diagnostic tests.");
        }
        Patient patient = patients.findById(form.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        if (actor.getRole() == Role.DOCTOR
                && !appointments.existsByDoctorIdAndPatientId(doctor.getId(), patient.getId())) {
            throw new AccessDeniedException("You can only request tests for patients you have an appointment with.");
        }
        DiagnosticTest test = new DiagnosticTest();
        test.setPatient(patient);
        test.setRequestedBy(doctor);
        test.setTestName(form.getTestName().trim());
        test.setCategory(form.getCategory().trim());
        test.setClinicalNotes(StringUtils.hasText(form.getClinicalNotes()) ? form.getClinicalNotes().trim() : null);
        test.setStatus(TestStatus.REQUESTED);
        return tests.save(test);
    }

    @Transactional
    public DiagnosticTest updateStatus(Long id, TestStatus newStatus, AppUserDetails actor) {
        if (actor.getRole() == Role.PATIENT) {
            throw new AccessDeniedException("Patients cannot change test status.");
        }
        DiagnosticTest test = getAuthorized(id, actor);
        if (!test.getStatus().nextStatuses().contains(newStatus)) {
            throw new BusinessRuleException("A test that is '" + test.getStatus().getLabel()
                    + "' cannot be changed to '" + newStatus.getLabel() + "'.");
        }
        test.setStatus(newStatus);
        return test;
    }

    @Transactional
    public DiagnosticTest enterResult(Long id, String resultSummary, AppUserDetails actor) {
        DiagnosticTest test = getAuthorized(id, actor);
        if (!canEnterResult(test, actor)) {
            if (actor.getRole() == Role.ADMIN || actor.getRole() == Role.DOCTOR) {
                throw new BusinessRuleException("A result can only be entered for an open test request.");
            }
            throw new AccessDeniedException("You are not allowed to enter results.");
        }
        test.setResultSummary(resultSummary.trim());
        test.setResultEnteredBy(actor.getFullName());
        test.setCompletedAt(LocalDateTime.now(clock));
        test.setStatus(TestStatus.COMPLETED);
        return test;
    }

    private boolean isOwnDoctor(DiagnosticTest test, AppUserDetails actor) {
        return doctors.findByUserId(actor.getUserId())
                .map(d -> d.getId().equals(test.getRequestedBy().getId()))
                .orElse(false);
    }

    private boolean isOwnPatient(DiagnosticTest test, AppUserDetails actor) {
        return test.getPatient().getUser() != null
                && test.getPatient().getUser().getId().equals(actor.getUserId());
    }

    private Doctor doctorOf(AppUserDetails actor) {
        return doctors.findByUserId(actor.getUserId())
                .orElseThrow(() -> new AccessDeniedException("No doctor profile is linked to this account."));
    }
}
