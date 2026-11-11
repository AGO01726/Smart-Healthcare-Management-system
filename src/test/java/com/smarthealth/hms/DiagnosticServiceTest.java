package com.smarthealth.hms;

import com.smarthealth.hms.domain.DiagnosticTest;
import com.smarthealth.hms.domain.Doctor;
import com.smarthealth.hms.domain.Patient;
import com.smarthealth.hms.domain.TestStatus;
import com.smarthealth.hms.dto.AppointmentForm;
import com.smarthealth.hms.dto.DiagnosticRequestForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.security.AppUserDetails;
import com.smarthealth.hms.service.AppointmentService;
import com.smarthealth.hms.service.DiagnosticService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DiagnosticServiceTest {

    @Autowired TestData data;
    @Autowired DiagnosticService diagnostics;
    @Autowired AppointmentService appointments;

    Doctor doctor;
    Patient patient;
    AppUserDetails doctorP;
    AppUserDetails patientP;
    AppUserDetails receptionP;
    AppUserDetails adminP;

    @BeforeEach
    void setUp() {
        doctor = data.doctor();
        patient = data.patientWithLogin();
        doctorP = data.principal(doctor.getUser());
        patientP = data.principal(patient.getUser());
        receptionP = data.principal(data.receptionist());
        adminP = data.principal(data.admin());

        // a doctor may only request tests for patients they have an appointment with
        AppointmentForm f = new AppointmentForm();
        f.setPatientId(patient.getId());
        f.setDoctorId(doctor.getId());
        f.setAppointmentDate(TestData.nextWeekday(DayOfWeek.TUESDAY));
        f.setStartTime(LocalTime.of(9, 0));
        f.setReason("Test setup");
        appointments.book(f, receptionP);
    }

    private DiagnosticRequestForm request() {
        DiagnosticRequestForm f = new DiagnosticRequestForm();
        f.setPatientId(patient.getId());
        f.setTestName("Complete Blood Count");
        f.setCategory("Hematology");
        f.setClinicalNotes("Sample request");
        return f;
    }

    @Test
    void fullWorkflowFromRequestToResult() {
        DiagnosticTest t = diagnostics.request(request(), doctorP);
        assertThat(t.getStatus()).isEqualTo(TestStatus.REQUESTED);

        diagnostics.updateStatus(t.getId(), TestStatus.SAMPLE_COLLECTED, receptionP);
        diagnostics.updateStatus(t.getId(), TestStatus.IN_PROGRESS, receptionP);
        assertThat(t.getStatus()).isEqualTo(TestStatus.IN_PROGRESS);

        diagnostics.enterResult(t.getId(), "[SAMPLE DATA] Hemoglobin 13.5 g/dL", doctorP);
        assertThat(t.getStatus()).isEqualTo(TestStatus.COMPLETED);
        assertThat(t.getCompletedAt()).isNotNull();
        assertThat(t.getResultEnteredBy()).isEqualTo(doctor.getFullName());
    }

    @Test
    void invalidStatusTransitionIsRejected() {
        DiagnosticTest t = diagnostics.request(request(), doctorP);
        assertThatThrownBy(() -> diagnostics.updateStatus(t.getId(), TestStatus.IN_PROGRESS, receptionP))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> diagnostics.updateStatus(t.getId(), TestStatus.COMPLETED, adminP))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resultVisibilityFollowsRoles() {
        DiagnosticTest t = diagnostics.request(request(), doctorP);
        assertThat(diagnostics.canViewResult(t, patientP)).isFalse(); // not completed yet
        diagnostics.enterResult(t.getId(), "[SAMPLE DATA] normal", doctorP);

        assertThat(diagnostics.canViewResult(t, patientP)).isTrue();
        assertThat(diagnostics.canViewResult(t, doctorP)).isTrue();
        assertThat(diagnostics.canViewResult(t, adminP)).isTrue();
        assertThat(diagnostics.canViewResult(t, receptionP)).isFalse();
    }

    @Test
    void otherPatientsAndDoctorsCannotAccessTheTest() {
        DiagnosticTest t = diagnostics.request(request(), doctorP);
        AppUserDetails strangerPatient = data.principal(data.patientWithLogin().getUser());
        AppUserDetails strangerDoctor = data.principal(data.doctor().getUser());

        assertThatThrownBy(() -> diagnostics.getAuthorized(t.getId(), strangerPatient))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> diagnostics.getAuthorized(t.getId(), strangerDoctor))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> diagnostics.enterResult(t.getId(), "x", strangerDoctor))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> diagnostics.updateStatus(t.getId(), TestStatus.CANCELLED, patientP))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void receptionistCannotEnterResultsOrRequestTests() {
        DiagnosticTest t = diagnostics.request(request(), doctorP);
        assertThatThrownBy(() -> diagnostics.enterResult(t.getId(), "x", receptionP))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> diagnostics.request(request(), receptionP))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void doctorCannotRequestTestForUnrelatedPatient() {
        DiagnosticRequestForm f = request();
        f.setPatientId(data.patient().getId());
        assertThatThrownBy(() -> diagnostics.request(f, doctorP)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resultCannotBeEnteredTwice() {
        DiagnosticTest t = diagnostics.request(request(), doctorP);
        diagnostics.enterResult(t.getId(), "[SAMPLE DATA] first", doctorP);
        assertThatThrownBy(() -> diagnostics.enterResult(t.getId(), "second", doctorP))
                .isInstanceOf(BusinessRuleException.class);
    }
}
