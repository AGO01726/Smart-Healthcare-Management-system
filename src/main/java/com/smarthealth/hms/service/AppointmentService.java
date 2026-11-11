package com.smarthealth.hms.service;

import com.smarthealth.hms.domain.Appointment;
import com.smarthealth.hms.domain.AppointmentStatus;
import com.smarthealth.hms.domain.Doctor;
import com.smarthealth.hms.domain.Patient;
import com.smarthealth.hms.domain.Role;
import com.smarthealth.hms.dto.AppointmentForm;
import com.smarthealth.hms.dto.RescheduleForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.exception.ResourceNotFoundException;
import com.smarthealth.hms.repository.AppointmentRepository;
import com.smarthealth.hms.repository.AppointmentSpecs;
import com.smarthealth.hms.repository.DoctorRepository;
import com.smarthealth.hms.repository.PatientRepository;
import com.smarthealth.hms.security.AppUserDetails;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Booking rules:
 * <ul>
 *   <li>the doctor must be active and available on that weekday and time window,</li>
 *   <li>the slot must not be in the past,</li>
 *   <li>the doctor and the patient must not have another non-cancelled appointment that overlaps the slot.</li>
 * </ul>
 * The doctor row is locked (SELECT ... FOR UPDATE) during booking so concurrent requests cannot double-book.
 */
@Service
public class AppointmentService {

    private static final Long NO_EXCLUDE = -1L;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final Clock clock;
    private final int slotMinutes;

    public AppointmentService(AppointmentRepository appointments, PatientRepository patients,
                              DoctorRepository doctors, Clock clock,
                              @Value("${hms.appointment.slot-minutes:30}") int slotMinutes) {
        this.appointments = appointments;
        this.patients = patients;
        this.doctors = doctors;
        this.clock = clock;
        this.slotMinutes = slotMinutes;
    }

    public int getSlotMinutes() {
        return slotMinutes;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public Page<Appointment> search(AppUserDetails actor, String q, AppointmentStatus status, Long doctorId,
                                    LocalDate date, int page, int size) {
        Long doctorFilter = doctorId;
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
        Pageable pageable = PageRequest.of(Math.max(page, 0), size,
                Sort.by(Sort.Order.desc("appointmentDate"), Sort.Order.asc("startTime")));
        return appointments.findAll(AppointmentSpecs.filter(q, status, doctorFilter, patientFilter, date), pageable);
    }

    /** Recent appointments of one patient, as visible to the actor (doctors only see their own). */
    @Transactional(readOnly = true)
    public List<Appointment> forPatient(Long patientId, AppUserDetails actor) {
        Long doctorFilter = actor.getRole() == Role.DOCTOR ? doctorOf(actor).getId() : null;
        Pageable pageable = PageRequest.of(0, 20,
                Sort.by(Sort.Order.desc("appointmentDate"), Sort.Order.asc("startTime")));
        return appointments.findAll(AppointmentSpecs.filter(null, null, doctorFilter, patientId, null), pageable)
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<Appointment> schedule(LocalDate date) {
        return appointments.findByAppointmentDateOrderByStartTimeAsc(date);
    }

    @Transactional(readOnly = true)
    public Appointment getAuthorized(Long id, AppUserDetails actor) {
        Appointment appointment = appointments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        boolean allowed = switch (actor.getRole()) {
            case ADMIN, RECEPTIONIST -> true;
            case DOCTOR -> doctorOf(actor).getId().equals(appointment.getDoctor().getId());
            case PATIENT -> appointment.getPatient().getUser() != null
                    && appointment.getPatient().getUser().getId().equals(actor.getUserId());
        };
        if (!allowed) {
            throw new AccessDeniedException("You are not allowed to access this appointment.");
        }
        return appointment;
    }

    /** Start times (HH:mm) that are still free for the doctor on the date. */
    @Transactional(readOnly = true)
    public List<LocalTime> freeSlots(Long doctorId, LocalDate date, Long excludeAppointmentId) {
        Doctor doctor = doctors.findById(doctorId).orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        LocalDate today = LocalDate.now(clock);
        if (!doctor.isActive() || date.isBefore(today) || !doctor.getAvailableDays().contains(date.getDayOfWeek())) {
            return List.of();
        }
        LocalTime now = LocalTime.now(clock);
        List<Appointment> booked = appointments.findByDoctorIdAndAppointmentDateAndStatusNot(
                doctorId, date, AppointmentStatus.CANCELLED);
        List<LocalTime> free = new ArrayList<>();
        for (LocalTime t = doctor.getAvailableFrom(); ; t = t.plusMinutes(slotMinutes)) {
            final LocalTime start = t;
            final LocalTime end = start.plusMinutes(slotMinutes);
            if (!end.isAfter(start) || end.isAfter(doctor.getAvailableTo())) {
                break;
            }
            if (date.equals(today) && !start.isAfter(now)) {
                continue;
            }
            boolean taken = booked.stream().anyMatch(a ->
                    !Objects.equals(a.getId(), excludeAppointmentId)
                            && a.getStartTime().isBefore(end) && a.getEndTime().isAfter(start));
            if (!taken) {
                free.add(start);
            }
        }
        return free;
    }

    // ------------------------------------------------------------------ commands

    @Transactional
    public Appointment book(AppointmentForm form, AppUserDetails actor) {
        Patient patient = resolvePatientForBooking(form.getPatientId(), actor);
        Doctor doctor = lockDoctor(form.getDoctorId());
        validateSlot(doctor, patient.getId(), form.getAppointmentDate(), form.getStartTime(), NO_EXCLUDE);

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(form.getAppointmentDate());
        appointment.setStartTime(form.getStartTime());
        appointment.setEndTime(form.getStartTime().plusMinutes(slotMinutes));
        appointment.setReason(form.getReason().trim());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointments.save(appointment);
    }

    @Transactional
    public Appointment reschedule(Long id, RescheduleForm form, AppUserDetails actor) {
        if (actor.getRole() == Role.DOCTOR) {
            throw new AccessDeniedException("Doctors cannot reschedule appointments.");
        }
        Appointment appointment = getAuthorized(id, actor);
        requireScheduled(appointment, "rescheduled");
        Doctor doctor = lockDoctor(appointment.getDoctor().getId());
        validateSlot(doctor, appointment.getPatient().getId(), form.getAppointmentDate(), form.getStartTime(),
                appointment.getId());
        appointment.setAppointmentDate(form.getAppointmentDate());
        appointment.setStartTime(form.getStartTime());
        appointment.setEndTime(form.getStartTime().plusMinutes(slotMinutes));
        return appointment;
    }

    @Transactional
    public Appointment cancel(Long id, AppUserDetails actor) {
        Appointment appointment = getAuthorized(id, actor);
        requireScheduled(appointment, "cancelled");
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return appointment;
    }

    @Transactional
    public Appointment complete(Long id, String notes, AppUserDetails actor) {
        if (actor.getRole() != Role.DOCTOR && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only the treating doctor or an administrator can complete an appointment.");
        }
        Appointment appointment = getAuthorized(id, actor);
        requireScheduled(appointment, "completed");
        if (appointment.getAppointmentDate().isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleException("A future appointment cannot be marked as completed.");
        }
        appointment.setNotes(StringUtils.hasText(notes) ? notes.trim() : null);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointment;
    }

    // ------------------------------------------------------------------ helpers

    private Patient resolvePatientForBooking(Long patientId, AppUserDetails actor) {
        return switch (actor.getRole()) {
            case PATIENT -> patients.findByUserId(actor.getUserId())
                    .orElseThrow(() -> new BusinessRuleException("No patient record is linked to your account."));
            case ADMIN, RECEPTIONIST -> {
                if (patientId == null) {
                    throw new BusinessRuleException("Please select a patient.");
                }
                yield patients.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
            }
            case DOCTOR -> throw new AccessDeniedException("Doctors cannot book appointments.");
        };
    }

    private Doctor lockDoctor(Long doctorId) {
        Doctor doctor = doctors.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        if (!doctor.isActive()) {
            throw new BusinessRuleException("This doctor is currently not accepting appointments.");
        }
        return doctor;
    }

    private void requireScheduled(Appointment appointment, String action) {
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessRuleException("Only scheduled appointments can be " + action
                    + ". This appointment is " + appointment.getStatus().getLabel().toLowerCase() + ".");
        }
    }

    private void validateSlot(Doctor doctor, Long patientId, LocalDate date, LocalTime start, Long excludeId) {
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today) || (date.equals(today) && !start.isAfter(LocalTime.now(clock)))) {
            throw new BusinessRuleException("The selected date and time are in the past.");
        }
        LocalTime end = start.plusMinutes(slotMinutes);
        if (!end.isAfter(start)) {
            throw new BusinessRuleException("The appointment must end before midnight.");
        }
        if (!doctor.getAvailableDays().contains(date.getDayOfWeek())) {
            throw new BusinessRuleException(doctor.getFullName() + " is not available on "
                    + date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                    + ". Available days: " + doctor.getAvailableDaysLabel() + ".");
        }
        if (start.isBefore(doctor.getAvailableFrom()) || end.isAfter(doctor.getAvailableTo())) {
            throw new BusinessRuleException("Appointments for " + doctor.getFullName() + " must fall between "
                    + doctor.getAvailableFrom().format(TIME_FORMAT) + " and "
                    + doctor.getAvailableTo().format(TIME_FORMAT) + ".");
        }
        if (appointments.countDoctorOverlaps(doctor.getId(), date, start, end, excludeId) > 0) {
            throw new BusinessRuleException("This time overlaps another appointment for "
                    + doctor.getFullName() + ". Please choose a different slot.");
        }
        if (appointments.countPatientOverlaps(patientId, date, start, end, excludeId) > 0) {
            throw new BusinessRuleException("The patient already has another appointment at this time.");
        }
    }

    private Doctor doctorOf(AppUserDetails actor) {
        return doctors.findByUserId(actor.getUserId())
                .orElseThrow(() -> new AccessDeniedException("No doctor profile is linked to this account."));
    }
}
