package com.smarthealth.hms.api;

import com.smarthealth.hms.domain.Appointment;
import com.smarthealth.hms.domain.DiagnosticTest;
import com.smarthealth.hms.domain.Doctor;
import com.smarthealth.hms.domain.Patient;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;

/** JSON representations returned by the REST API (entities are never serialized directly). */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <E, T> PageDto<T> of(Page<E> page, Function<E, T> mapper) {
            return new PageDto<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                    page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }

    public record PatientDto(Long id, String patientCode, String fullName, String gender, LocalDate dateOfBirth,
                             String phone, String email, String bloodGroup) {
        public static PatientDto from(Patient p) {
            return new PatientDto(p.getId(), p.getPatientCode(), p.getFullName(), p.getGender().name(),
                    p.getDateOfBirth(), p.getPhone(), p.getEmail(), p.getBloodGroup());
        }
    }

    public record DoctorDto(Long id, String fullName, String department, String specialization,
                            List<DayOfWeek> availableDays, LocalTime availableFrom, LocalTime availableTo,
                            BigDecimal consultationFee, boolean active) {
        public static DoctorDto from(Doctor d) {
            return new DoctorDto(d.getId(), d.getFullName(), d.getDepartment().getName(), d.getSpecialization(),
                    d.getAvailableDays().stream().sorted().toList(), d.getAvailableFrom(), d.getAvailableTo(),
                    d.getConsultationFee(), d.isActive());
        }
    }

    public record AppointmentDto(Long id, Long patientId, String patientName, Long doctorId, String doctorName,
                                 LocalDate date, LocalTime startTime, LocalTime endTime, String status,
                                 String reason) {
        public static AppointmentDto from(Appointment a) {
            return new AppointmentDto(a.getId(), a.getPatient().getId(), a.getPatient().getFullName(),
                    a.getDoctor().getId(), a.getDoctor().getFullName(), a.getAppointmentDate(), a.getStartTime(),
                    a.getEndTime(), a.getStatus().name(), a.getReason());
        }
    }

    /** The result text is included only when the caller is allowed to read it. */
    public record DiagnosticDto(Long id, String patientName, String testName, String category, String status,
                                LocalDateTime requestedAt, LocalDateTime completedAt, String result) {
        public static DiagnosticDto from(DiagnosticTest t, boolean includeResult) {
            return new DiagnosticDto(t.getId(), t.getPatient().getFullName(), t.getTestName(), t.getCategory(),
                    t.getStatus().name(), t.getRequestedAt(), t.getCompletedAt(),
                    includeResult ? t.getResultSummary() : null);
        }
    }
}
