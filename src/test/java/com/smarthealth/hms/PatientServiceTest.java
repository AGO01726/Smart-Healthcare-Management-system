package com.smarthealth.hms;

import com.smarthealth.hms.domain.Doctor;
import com.smarthealth.hms.domain.Patient;
import com.smarthealth.hms.domain.Role;
import com.smarthealth.hms.domain.User;
import com.smarthealth.hms.dto.AppointmentForm;
import com.smarthealth.hms.dto.PatientForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.repository.UserRepository;
import com.smarthealth.hms.security.AppUserDetails;
import com.smarthealth.hms.service.AppointmentService;
import com.smarthealth.hms.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PatientServiceTest {

    @Autowired TestData data;
    @Autowired PatientService patients;
    @Autowired AppointmentService appointments;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void registrationGeneratesPatientCode() {
        Patient p = data.patient();
        assertThat(p.getId()).isNotNull();
        assertThat(p.getPatientCode()).matches("PAT-\\d{6}");
    }

    @Test
    void portalLoginPasswordIsStoredAsBCryptHash() {
        Patient p = data.patientWithLogin();
        User user = p.getUser();
        assertThat(user.getRole()).isEqualTo(Role.PATIENT);
        assertThat(user.getPasswordHash()).isNotEqualTo(TestData.PASSWORD).startsWith("$2");
        assertThat(encoder.matches(TestData.PASSWORD, user.getPasswordHash())).isTrue();
    }

    @Test
    void weakPasswordAndDuplicateUsernameAreRejected() {
        PatientForm weak = data.patientForm("weakuser1");
        weak.setPassword("short");
        assertThatThrownBy(() -> patients.register(weak)).isInstanceOf(BusinessRuleException.class);

        Patient first = data.patientWithLogin();
        PatientForm duplicate = data.patientForm(first.getUser().getUsername());
        assertThatThrownBy(() -> patients.register(duplicate))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("already taken");
    }

    @Test
    void loginRequiresBothUsernameAndPassword() {
        PatientForm f = data.patientForm("onlyusername");
        f.setPassword(null);
        assertThatThrownBy(() -> patients.register(f)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void searchFindsByNameAndCode() {
        AppUserDetails admin = data.principal(data.admin());
        PatientForm f = data.patientForm(null);
        f.setFirstName("Zebulon");
        f.setLastName("Quillfeather");
        Patient p = patients.register(f);

        Page<Patient> byName = patients.search("quillfeath", 0, 10, admin);
        assertThat(byName.getContent()).extracting(Patient::getId).contains(p.getId());
        Page<Patient> byCode = patients.search(p.getPatientCode(), 0, 10, admin);
        assertThat(byCode.getContent()).extracting(Patient::getId).contains(p.getId());
        assertThat(patients.search("no-such-patient-xyz", 0, 10, admin).getContent()).isEmpty();
    }

    @Test
    void updateChangesStoredValues() {
        Patient p = data.patient();
        PatientForm f = PatientForm.from(p);
        f.setPhone("9111111111");
        f.setBloodGroup("O+");
        patients.update(p.getId(), f);
        Patient reloaded = patients.get(p.getId());
        assertThat(reloaded.getPhone()).isEqualTo("9111111111");
        assertThat(reloaded.getBloodGroup()).isEqualTo("O+");
    }

    @Test
    void patientWithAppointmentsCannotBeDeleted() {
        Doctor doctor = data.doctor();
        Patient p = data.patient();
        AppointmentForm f = new AppointmentForm();
        f.setPatientId(p.getId());
        f.setDoctorId(doctor.getId());
        f.setAppointmentDate(TestData.nextWeekday(DayOfWeek.WEDNESDAY));
        f.setStartTime(LocalTime.of(9, 0));
        f.setReason("x");
        appointments.book(f, data.principal(data.receptionist()));
        assertThatThrownBy(() -> patients.delete(p.getId())).isInstanceOf(BusinessRuleException.class);

        Patient unused = data.patient();
        patients.delete(unused.getId());
        assertThat(users.count()).isGreaterThan(0);
    }

    @Test
    void patientsCanOnlyViewTheirOwnRecordAndDoctorsOnlyTheirOwnPatients() {
        Patient a = data.patientWithLogin();
        Patient b = data.patientWithLogin();
        assertThat(patients.getAuthorized(a.getId(), data.principal(a.getUser()))).isEqualTo(a);
        assertThatThrownBy(() -> patients.getAuthorized(b.getId(), data.principal(a.getUser())))
                .isInstanceOf(AccessDeniedException.class);

        Doctor doctor = data.doctor();
        AppUserDetails doctorP = data.principal(doctor.getUser());
        assertThatThrownBy(() -> patients.getAuthorized(a.getId(), doctorP)).isInstanceOf(AccessDeniedException.class);
        assertThat(patients.search("", 0, 10, doctorP).getContent()).isEmpty();

        AppointmentForm f = new AppointmentForm();
        f.setPatientId(a.getId());
        f.setDoctorId(doctor.getId());
        f.setAppointmentDate(TestData.nextWeekday(DayOfWeek.THURSDAY));
        f.setStartTime(LocalTime.of(9, 0));
        f.setReason("x");
        appointments.book(f, data.principal(data.receptionist()));
        assertThat(patients.getAuthorized(a.getId(), doctorP)).isEqualTo(a);
        assertThat(patients.search("", 0, 10, doctorP).getContent()).extracting(Patient::getId).containsExactly(a.getId());
    }
}
