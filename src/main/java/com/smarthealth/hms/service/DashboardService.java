package com.smarthealth.hms.service;

import com.smarthealth.hms.domain.AppointmentStatus;
import com.smarthealth.hms.domain.Department;
import com.smarthealth.hms.domain.Doctor;
import com.smarthealth.hms.domain.Patient;
import com.smarthealth.hms.domain.TestStatus;
import com.smarthealth.hms.repository.AppointmentRepository;
import com.smarthealth.hms.repository.DepartmentRepository;
import com.smarthealth.hms.repository.DiagnosticTestRepository;
import com.smarthealth.hms.repository.DoctorRepository;
import com.smarthealth.hms.repository.PatientRepository;
import com.smarthealth.hms.repository.StaffRepository;
import com.smarthealth.hms.repository.UserRepository;
import com.smarthealth.hms.security.AppUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds the statistics shown on the role-specific dashboards. */
@Service
public class DashboardService {

    private static final Set<TestStatus> OPEN_TESTS =
            Set.of(TestStatus.REQUESTED, TestStatus.SAMPLE_COLLECTED, TestStatus.IN_PROGRESS);

    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final DepartmentRepository departments;
    private final StaffRepository staff;
    private final UserRepository users;
    private final AppointmentRepository appointments;
    private final DiagnosticTestRepository tests;
    private final Clock clock;

    public DashboardService(PatientRepository patients, DoctorRepository doctors, DepartmentRepository departments,
                            StaffRepository staff, UserRepository users, AppointmentRepository appointments,
                            DiagnosticTestRepository tests, Clock clock) {
        this.patients = patients;
        this.doctors = doctors;
        this.departments = departments;
        this.staff = staff;
        this.users = users;
        this.appointments = appointments;
        this.tests = tests;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> forUser(AppUserDetails actor) {
        return switch (actor.getRole()) {
            case ADMIN -> admin();
            case RECEPTIONIST -> reception();
            case DOCTOR -> doctor(actor);
            case PATIENT -> patient(actor);
        };
    }

    private Map<String, Object> admin() {
        LocalDate today = LocalDate.now(clock);
        Map<String, Object> m = new LinkedHashMap<>();
        long scheduled = appointments.countByStatus(AppointmentStatus.SCHEDULED);
        long completed = appointments.countByStatus(AppointmentStatus.COMPLETED);
        long cancelled = appointments.countByStatus(AppointmentStatus.CANCELLED);
        long total = Math.max(1, scheduled + completed + cancelled);
        m.put("totalPatients", patients.count());
        m.put("totalDoctors", doctors.count());
        m.put("activeDoctors", doctors.countByActiveTrue());
        m.put("totalDepartments", departments.count());
        m.put("totalStaff", staff.count());
        m.put("totalUsers", users.count());
        m.put("todayAppointments", appointments.countByAppointmentDate(today));
        m.put("scheduled", scheduled);
        m.put("completed", completed);
        m.put("cancelled", cancelled);
        m.put("scheduledPct", scheduled * 100 / total);
        m.put("completedPct", completed * 100 / total);
        m.put("cancelledPct", cancelled * 100 / total);
        m.put("pendingTests", tests.countByStatusIn(OPEN_TESTS));
        m.put("completedTests", tests.countByStatus(TestStatus.COMPLETED));
        m.put("todayList", appointments.findByAppointmentDateOrderByStartTimeAsc(today));
        List<Map<String, Object>> deptRows = new ArrayList<>();
        for (Department d : departments.findAllByOrderByNameAsc()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", d.getName());
            row.put("doctors", doctors.countByDepartmentId(d.getId()));
            row.put("staff", staff.countByDepartmentId(d.getId()));
            row.put("appointments", appointments.countActiveByDepartment(d.getId()));
            deptRows.add(row);
        }
        m.put("departmentRows", deptRows);
        return m;
    }

    private Map<String, Object> reception() {
        LocalDate today = LocalDate.now(clock);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todayAppointments", appointments.countByAppointmentDate(today));
        m.put("upcomingWeek", appointments.countByStatusAndAppointmentDateBetween(
                AppointmentStatus.SCHEDULED, today, today.plusDays(7)));
        m.put("patientsToday", patients.countByCreatedAtGreaterThanEqual(today.atStartOfDay()));
        m.put("totalPatients", patients.count());
        m.put("pendingTests", tests.countByStatusIn(OPEN_TESTS));
        m.put("todayList", appointments.findByAppointmentDateOrderByStartTimeAsc(today));
        return m;
    }

    private Map<String, Object> doctor(AppUserDetails actor) {
        LocalDate today = LocalDate.now(clock);
        Doctor doctor = doctors.findByUserId(actor.getUserId()).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        if (doctor == null) {
            m.put("noProfile", true);
            return m;
        }
        m.put("doctor", doctor);
        m.put("todayAppointments", appointments.countByDoctorIdAndAppointmentDate(doctor.getId(), today));
        m.put("upcoming", appointments.countByDoctorIdAndStatusAndAppointmentDateGreaterThanEqual(
                doctor.getId(), AppointmentStatus.SCHEDULED, today));
        m.put("completed", appointments.countByDoctorIdAndStatus(doctor.getId(), AppointmentStatus.COMPLETED));
        m.put("pendingTests", tests.countByRequestedByIdAndStatusIn(doctor.getId(), OPEN_TESTS));
        m.put("todayList", appointments.findByDoctorIdAndAppointmentDateOrderByStartTimeAsc(doctor.getId(), today));
        m.put("upcomingList", appointments
                .findByDoctorIdAndStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(
                        doctor.getId(), AppointmentStatus.SCHEDULED, today)
                .stream().limit(6).toList());
        return m;
    }

    private Map<String, Object> patient(AppUserDetails actor) {
        LocalDate today = LocalDate.now(clock);
        Patient patient = patients.findByUserId(actor.getUserId()).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        if (patient == null) {
            m.put("noProfile", true);
            return m;
        }
        m.put("patient", patient);
        var upcoming = appointments
                .findByPatientIdAndStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(
                        patient.getId(), AppointmentStatus.SCHEDULED, today);
        m.put("upcomingCount", upcoming.size());
        m.put("upcomingList", upcoming.stream().limit(5).toList());
        m.put("completedCount", appointments.countByPatientIdAndStatus(patient.getId(), AppointmentStatus.COMPLETED));
        m.put("testCount", tests.countByPatientId(patient.getId()));
        m.put("recentTests", tests.findTop5ByPatientIdOrderByRequestedAtDesc(patient.getId()));
        return m;
    }
}
