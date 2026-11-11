package com.smarthealth.hms;

import com.smarthealth.hms.domain.Appointment;
import com.smarthealth.hms.domain.AppointmentStatus;
import com.smarthealth.hms.domain.Doctor;
import com.smarthealth.hms.domain.Patient;
import com.smarthealth.hms.domain.User;
import com.smarthealth.hms.dto.AppointmentForm;
import com.smarthealth.hms.dto.RescheduleForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.security.AppUserDetails;
import com.smarthealth.hms.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentServiceTest {

    @Autowired TestData data;
    @Autowired AppointmentService appointments;

    Doctor doctor;
    Patient patientA;
    Patient patientB;
    AppUserDetails reception;
    LocalDate monday;

    @BeforeEach
    void setUp() {
        doctor = data.doctor();
        patientA = data.patient();
        patientB = data.patient();
        reception = data.principal(data.receptionist());
        monday = TestData.nextWeekday(DayOfWeek.MONDAY);
    }

    private AppointmentForm form(Patient patient, Doctor d, LocalDate date, LocalTime time) {
        AppointmentForm f = new AppointmentForm();
        f.setPatientId(patient.getId());
        f.setDoctorId(d.getId());
        f.setAppointmentDate(date);
        f.setStartTime(time);
        f.setReason("Check-up (test)");
        return f;
    }

    @Test
    void bookingStoresThirtyMinuteSlot() {
        Appointment a = appointments.book(form(patientA, doctor, monday, LocalTime.of(10, 0)), reception);
        assertThat(a.getId()).isNotNull();
        assertThat(a.getEndTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void overlappingBookingForSameDoctorIsRejected() {
        appointments.book(form(patientA, doctor, monday, LocalTime.of(10, 0)), reception);
        assertThatThrownBy(() -> appointments.book(form(patientB, doctor, monday, LocalTime.of(10, 15)), reception))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void backToBackBookingsAreAllowed() {
        appointments.book(form(patientA, doctor, monday, LocalTime.of(10, 0)), reception);
        Appointment second = appointments.book(form(patientB, doctor, monday, LocalTime.of(10, 30)), reception);
        assertThat(second.getStartTime()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    void samePatientCannotBeInTwoPlacesAtOnce() {
        Doctor other = data.doctor();
        appointments.book(form(patientA, doctor, monday, LocalTime.of(10, 0)), reception);
        assertThatThrownBy(() -> appointments.book(form(patientA, other, monday, LocalTime.of(10, 0)), reception))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("patient already has");
    }

    @Test
    void bookingOutsideWorkingHoursIsRejected() {
        assertThatThrownBy(() -> appointments.book(form(patientA, doctor, monday, LocalTime.of(14, 0)), reception))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("between 09:00 and 13:00");
        assertThatThrownBy(() -> appointments.book(form(patientA, doctor, monday, LocalTime.of(12, 45)), reception))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void bookingOnUnavailableDayIsRejected() {
        LocalDate saturday = TestData.nextWeekday(DayOfWeek.SATURDAY);
        assertThatThrownBy(() -> appointments.book(form(patientA, doctor, saturday, LocalTime.of(10, 0)), reception))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("not available");
    }

    @Test
    void bookingInThePastIsRejected() {
        assertThatThrownBy(() -> appointments.book(
                form(patientA, doctor, LocalDate.now().minusDays(1), LocalTime.of(10, 0)), reception))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("past");
    }

    @Test
    void cancellingFreesTheSlot() {
        Appointment a = appointments.book(form(patientA, doctor, monday, LocalTime.of(10, 0)), reception);
        appointments.cancel(a.getId(), reception);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        Appointment again = appointments.book(form(patientB, doctor, monday, LocalTime.of(10, 0)), reception);
        assertThat(again.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void cancelledAppointmentCannotBeCancelledAgain() {
        Appointment a = appointments.book(form(patientA, doctor, monday, LocalTime.of(10, 0)), reception);
        appointments.cancel(a.getId(), reception);
        assertThatThrownBy(() -> appointments.cancel(a.getId(), reception)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rescheduleMovesAppointmentAndChecksOverlap() {
        Appointment a = appointments.book(form(patientA, doctor, monday, LocalTime.of(10, 0)), reception);
        appointments.book(form(patientB, doctor, monday, LocalTime.of(11, 0)), reception);

        RescheduleForm blocked = new RescheduleForm();
        blocked.setAppointmentDate(monday);
        blocked.setStartTime(LocalTime.of(11, 15));
        assertThatThrownBy(() -> appointments.reschedule(a.getId(), blocked, reception))
                .isInstanceOf(BusinessRuleException.class);

        RescheduleForm shiftWithinOwnSlot = new RescheduleForm();
        shiftWithinOwnSlot.setAppointmentDate(monday);
        shiftWithinOwnSlot.setStartTime(LocalTime.of(10, 15));
        appointments.reschedule(a.getId(), shiftWithinOwnSlot, reception);
        assertThat(a.getStartTime()).isEqualTo(LocalTime.of(10, 15));

        RescheduleForm free = new RescheduleForm();
        free.setAppointmentDate(monday.plusDays(1));
        free.setStartTime(LocalTime.of(9, 0));
        appointments.reschedule(a.getId(), free, reception);
        assertThat(a.getAppointmentDate()).isEqualTo(monday.plusDays(1));
    }

    @Test
    void freeSlotsExcludeBookedTimes() {
        appointments.book(form(patientA, doctor, monday, LocalTime.of(10, 0)), reception);
        List<LocalTime> free = appointments.freeSlots(doctor.getId(), monday, null);
        assertThat(free).contains(LocalTime.of(9, 0), LocalTime.of(10, 30), LocalTime.of(12, 30));
        assertThat(free).doesNotContain(LocalTime.of(10, 0), LocalTime.of(13, 0));
        assertThat(appointments.freeSlots(doctor.getId(), TestData.nextWeekday(DayOfWeek.SUNDAY), null)).isEmpty();
    }

    @Test
    void patientCanOnlyManageOwnAppointments() {
        Patient owner = data.patientWithLogin();
        Patient stranger = data.patientWithLogin();
        AppUserDetails ownerPrincipal = data.principal(owner.getUser());
        AppUserDetails strangerPrincipal = data.principal(stranger.getUser());

        AppointmentForm f = form(owner, doctor, monday, LocalTime.of(9, 0));
        f.setPatientId(null); // a patient always books for themselves
        Appointment a = appointments.book(f, ownerPrincipal);
        assertThat(a.getPatient().getId()).isEqualTo(owner.getId());

        assertThatThrownBy(() -> appointments.cancel(a.getId(), strangerPrincipal))
                .isInstanceOf(AccessDeniedException.class);
        appointments.cancel(a.getId(), ownerPrincipal);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void futureAppointmentCannotBeCompleted() {
        User adminUser = data.admin();
        Appointment a = appointments.book(form(patientA, doctor, monday, LocalTime.of(10, 0)), reception);
        assertThatThrownBy(() -> appointments.complete(a.getId(), "notes", data.principal(adminUser)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("future");
    }
}
