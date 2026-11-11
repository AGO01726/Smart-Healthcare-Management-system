package com.smarthealth.hms.config;

import com.smarthealth.hms.domain.*;
import com.smarthealth.hms.repository.*;
import com.smarthealth.hms.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Creates FICTIONAL demo data for local development, but only when the database has no users yet.
 * Disable with HMS_SEED_ENABLED=false. Never run with demo data in a real deployment.
 * All names, phone numbers and e-mail addresses below are invented (e-mails use the reserved example.com domain).
 */
@Component
@ConditionalOnProperty(name = "hms.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String SAMPLE = "[SAMPLE DATA - fictional] ";

    private final UserRepository users;
    private final UserService userService;
    private final DepartmentRepository departments;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final StaffRepository staff;
    private final AppointmentRepository appointments;
    private final DiagnosticTestRepository tests;
    private final Clock clock;
    private final String demoPassword;

    public DataSeeder(UserRepository users, UserService userService, DepartmentRepository departments,
                      DoctorRepository doctors, PatientRepository patients, StaffRepository staff,
                      AppointmentRepository appointments, DiagnosticTestRepository tests, Clock clock,
                      @Value("${hms.seed.demo-password}") String demoPassword) {
        this.users = users;
        this.userService = userService;
        this.departments = departments;
        this.doctors = doctors;
        this.patients = patients;
        this.staff = staff;
        this.appointments = appointments;
        this.tests = tests;
        this.clock = clock;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            log.info("Users already exist - demo data NOT seeded.");
            return;
        }
        log.warn("Seeding FICTIONAL demo data and demo accounts. Do not use this in production.");

        userService.createUser("admin", demoPassword, "Demo Administrator", "admin@example.com", Role.ADMIN);
        userService.createUser("reception", demoPassword, "Demo Receptionist", "reception@example.com", Role.RECEPTIONIST);

        Department cardiology = department("Cardiology", "Heart and vascular care");
        Department pediatrics = department("Pediatrics", "Child health and development");
        Department general = department("General Medicine", "Primary care and internal medicine");
        Department pathology = department("Pathology & Laboratory", "Diagnostic laboratory services");
        department("Orthopedics", "Bones, joints and sports injuries");

        Doctor sharma = doctor("dr.sharma", "Dr. Aarav Sharma (Demo)", "dr.sharma@example.com", cardiology,
                "Cardiologist", "MD, DM (fictional)", "+91 9000000101", 14, "800.00",
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), LocalTime.of(9, 0), LocalTime.of(13, 0));
        Doctor iyer = doctor("dr.iyer", "Dr. Meera Iyer (Demo)", "dr.iyer@example.com", pediatrics,
                "Pediatrician", "MD Pediatrics (fictional)", "+91 9000000102", 9, "600.00",
                EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), LocalTime.of(10, 0), LocalTime.of(16, 0));
        Doctor khan = doctor("dr.khan", "Dr. Imran Khan (Demo)", "dr.khan@example.com", general,
                "General Physician", "MBBS, MD (fictional)", "+91 9000000103", 11, "500.00",
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                LocalTime.of(9, 0), LocalTime.of(17, 0));

        staffMember("Demo Nurse Kavya Nair", "Head Nurse", general, "+91 9000000201");
        staffMember("Demo Technician Rohan Das", "Lab Technician", pathology, "+91 9000000202");
        staffMember("Demo Technician Sana Ali", "Radiology Technician", cardiology, "+91 9000000203");

        Patient riya = patient("patient1", "Riya", "Verma", Gender.FEMALE, LocalDate.of(1991, 4, 12), "+91 9000000301", "O+");
        Patient arjun = patient("patient2", "Arjun", "Mehta", Gender.MALE, LocalDate.of(1985, 9, 3), "+91 9000000302", "B+");
        Patient neha = patientWithoutLogin("Neha", "Kulkarni", Gender.FEMALE, LocalDate.of(2016, 1, 20), "+91 9000000303", "A+");

        // past (completed) and upcoming (scheduled) appointments
        appointment(riya, sharma, previousDay(sharma, 7), LocalTime.of(9, 0), AppointmentStatus.COMPLETED,
                "Chest discomfort follow-up", SAMPLE + "Advised routine ECG and lipid profile.");
        appointment(arjun, khan, previousDay(khan, 3), LocalTime.of(11, 0), AppointmentStatus.COMPLETED,
                "Seasonal fever", SAMPLE + "Rest, fluids, review if fever persists.");
        appointment(neha, iyer, previousDay(iyer, 5), LocalTime.of(10, 30), AppointmentStatus.COMPLETED,
                "Routine growth check", SAMPLE + "Growth within expected range.");
        appointment(riya, sharma, nextDay(sharma, 1), LocalTime.of(10, 0), AppointmentStatus.SCHEDULED,
                "Review of ECG report", null);
        appointment(arjun, khan, nextDay(khan, 1), LocalTime.of(9, 30), AppointmentStatus.SCHEDULED,
                "Annual health check", null);
        appointment(neha, iyer, nextDay(iyer, 2), LocalTime.of(11, 0), AppointmentStatus.SCHEDULED,
                "Vaccination consultation", null);

        diagnostic(riya, sharma, "Lipid Profile", "Biochemistry", TestStatus.COMPLETED,
                "Fasting sample requested.", SAMPLE + "Total cholesterol 182 mg/dL, LDL 104 mg/dL, HDL 52 mg/dL (all within sample reference range).");
        diagnostic(riya, sharma, "Resting ECG", "Cardiology", TestStatus.IN_PROGRESS, "Chest discomfort.", null);
        diagnostic(arjun, khan, "Complete Blood Count", "Hematology", TestStatus.REQUESTED, "Recurrent fever.", null);

        log.warn("Demo accounts created (username / password from HMS_DEMO_PASSWORD): admin, reception, "
                + "dr.sharma, dr.iyer, dr.khan, patient1, patient2");
    }

    // ------------------------------------------------------------------ helpers

    private Department department(String name, String description) {
        Department d = new Department();
        d.setName(name);
        d.setDescription(description);
        return departments.save(d);
    }

    private Doctor doctor(String username, String fullName, String email, Department department, String specialization,
                          String qualification, String phone, int experience, String fee, Set<DayOfWeek> days,
                          LocalTime from, LocalTime to) {
        User user = userService.createUser(username, demoPassword, fullName, email, Role.DOCTOR);
        Doctor d = new Doctor();
        d.setUser(user);
        d.setDepartment(department);
        d.setSpecialization(specialization);
        d.setQualification(qualification);
        d.setPhone(phone);
        d.setExperienceYears(experience);
        d.setConsultationFee(new BigDecimal(fee));
        d.getAvailableDays().addAll(days);
        d.setAvailableFrom(from);
        d.setAvailableTo(to);
        return doctors.save(d);
    }

    private void staffMember(String name, String designation, Department department, String phone) {
        Staff s = new Staff();
        s.setFullName(name);
        s.setDesignation(designation);
        s.setDepartment(department);
        s.setPhone(phone);
        s.setEmail(null);
        s.setJoinedDate(LocalDate.now(clock).minusYears(2));
        staff.save(s);
    }

    private Patient patient(String username, String first, String last, Gender gender, LocalDate dob, String phone,
                            String blood) {
        User user = userService.createUser(username, demoPassword, first + " " + last,
                username + "@example.com", Role.PATIENT);
        Patient p = newPatient(first, last, gender, dob, phone, blood);
        p.setEmail(username + "@example.com");
        p.setUser(user);
        return savePatient(p);
    }

    private Patient patientWithoutLogin(String first, String last, Gender gender, LocalDate dob, String phone,
                                        String blood) {
        return savePatient(newPatient(first, last, gender, dob, phone, blood));
    }

    private Patient newPatient(String first, String last, Gender gender, LocalDate dob, String phone, String blood) {
        Patient p = new Patient();
        p.setFirstName(first);
        p.setLastName(last);
        p.setGender(gender);
        p.setDateOfBirth(dob);
        p.setPhone(phone);
        p.setBloodGroup(blood);
        p.setAddress("Demo address, Sample Nagar (fictional)");
        p.setAllergies("None recorded (sample data)");
        p.setEmergencyContact("Demo Contact +91 9000000999");
        p.setPatientCode("TMP-" + java.util.UUID.randomUUID().toString().substring(0, 12));
        return p;
    }

    private Patient savePatient(Patient p) {
        p = patients.save(p);
        p.setPatientCode(String.format("PAT-%06d", p.getId()));
        return p;
    }

    private void appointment(Patient patient, Doctor doctor, LocalDate date, LocalTime start, AppointmentStatus status,
                             String reason, String notes) {
        Appointment a = new Appointment();
        a.setPatient(patient);
        a.setDoctor(doctor);
        a.setAppointmentDate(date);
        a.setStartTime(start);
        a.setEndTime(start.plusMinutes(30));
        a.setReason(reason);
        a.setNotes(notes);
        a.setStatus(status);
        appointments.save(a);
    }

    private void diagnostic(Patient patient, Doctor doctor, String name, String category, TestStatus status,
                            String notes, String result) {
        DiagnosticTest t = new DiagnosticTest();
        t.setPatient(patient);
        t.setRequestedBy(doctor);
        t.setTestName(name);
        t.setCategory(category);
        t.setClinicalNotes(notes);
        t.setStatus(status);
        t.setRequestedAt(LocalDateTime.now(clock).minusDays(2));
        if (result != null) {
            t.setResultSummary(result);
            t.setResultEnteredBy(doctor.getFullName());
            t.setCompletedAt(LocalDateTime.now(clock).minusDays(1));
        }
        tests.save(t);
    }

    /** First date strictly after today (plus {@code skip - 1} further matching days) on which the doctor works. */
    private LocalDate nextDay(Doctor doctor, int skip) {
        LocalDate date = LocalDate.now(clock);
        int found = 0;
        while (true) {
            date = date.plusDays(1);
            if (doctor.getAvailableDays().contains(date.getDayOfWeek()) && ++found >= skip) {
                return date;
            }
        }
    }

    /** A past date on which the doctor works, at least {@code minDaysAgo} days back. */
    private LocalDate previousDay(Doctor doctor, int minDaysAgo) {
        LocalDate date = LocalDate.now(clock).minusDays(minDaysAgo);
        while (!doctor.getAvailableDays().contains(date.getDayOfWeek())) {
            date = date.minusDays(1);
        }
        return date;
    }
}
