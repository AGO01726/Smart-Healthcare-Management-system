package com.smarthealth.hms;

import com.smarthealth.hms.domain.Department;
import com.smarthealth.hms.domain.Doctor;
import com.smarthealth.hms.domain.Gender;
import com.smarthealth.hms.domain.Patient;
import com.smarthealth.hms.domain.Role;
import com.smarthealth.hms.domain.User;
import com.smarthealth.hms.dto.DepartmentForm;
import com.smarthealth.hms.dto.DoctorForm;
import com.smarthealth.hms.dto.PatientForm;
import com.smarthealth.hms.security.AppUserDetails;
import com.smarthealth.hms.service.DepartmentService;
import com.smarthealth.hms.service.DoctorService;
import com.smarthealth.hms.service.PatientService;
import com.smarthealth.hms.service.UserService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

/** Test fixtures. Everything created here is fictional and rolled back after each test. */
@Component
public class TestData {

    public static final String PASSWORD = "Test@1234";
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final UserService userService;
    private final DepartmentService departmentService;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public TestData(UserService userService, DepartmentService departmentService, DoctorService doctorService,
                    PatientService patientService) {
        this.userService = userService;
        this.departmentService = departmentService;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    private static int next() {
        return COUNTER.incrementAndGet();
    }

    public User admin() {
        int n = next();
        return userService.createUser("admin" + n, PASSWORD, "Test Admin " + n, "admin" + n + "@example.com", Role.ADMIN);
    }

    public User receptionist() {
        int n = next();
        return userService.createUser("recep" + n, PASSWORD, "Test Receptionist " + n, "recep" + n + "@example.com", Role.RECEPTIONIST);
    }

    public Department department() {
        DepartmentForm form = new DepartmentForm();
        form.setName("Dept " + next());
        return departmentService.save(form);
    }

    /** Doctor available Monday-Friday 09:00-13:00. */
    public Doctor doctor() {
        int n = next();
        DoctorForm form = new DoctorForm();
        form.setFullName("Dr. Test " + n);
        form.setEmail("doc" + n + "@example.com");
        form.setUsername("doc" + n);
        form.setPassword(PASSWORD);
        form.setDepartmentId(department().getId());
        form.setSpecialization("General");
        form.setPhone("9000000000");
        form.setExperienceYears(5);
        form.setConsultationFee(new BigDecimal("400.00"));
        form.setAvailableDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
        form.setAvailableFrom(LocalTime.of(9, 0));
        form.setAvailableTo(LocalTime.of(13, 0));
        return doctorService.create(form);
    }

    public Patient patient() {
        return patientService.register(patientForm(null));
    }

    /** Patient with a portal login (username "patN"). */
    public Patient patientWithLogin() {
        int n = next();
        return patientService.register(patientForm("pat" + n));
    }

    public PatientForm patientForm(String username) {
        int n = next();
        PatientForm form = new PatientForm();
        form.setFirstName("Test" + n);
        form.setLastName("Patient");
        form.setGender(Gender.FEMALE);
        form.setDateOfBirth(LocalDate.of(1990, 1, 1));
        form.setPhone("9000000001");
        form.setEmail("patient" + n + "@example.com");
        if (username != null) {
            form.setUsername(username);
            form.setPassword(PASSWORD);
        }
        return form;
    }

    public AppUserDetails principal(User user) {
        return new AppUserDetails(user, true);
    }

    /** The next date (at least tomorrow) that falls on the given weekday. */
    public static LocalDate nextWeekday(DayOfWeek day) {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != day) {
            date = date.plusDays(1);
        }
        return date;
    }
}
