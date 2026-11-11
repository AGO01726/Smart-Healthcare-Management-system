package com.smarthealth.hms.repository;

import com.smarthealth.hms.domain.Appointment;
import com.smarthealth.hms.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    /** Number of non-cancelled appointments of a doctor that overlap [start, end) on a date, ignoring one appointment id. */
    @Query("""
            select count(a) from Appointment a
            where a.doctor.id = :doctorId
              and a.appointmentDate = :date
              and a.status <> com.smarthealth.hms.domain.AppointmentStatus.CANCELLED
              and a.startTime < :end and a.endTime > :start
              and a.id <> :excludeId
            """)
    long countDoctorOverlaps(@Param("doctorId") Long doctorId, @Param("date") LocalDate date,
                             @Param("start") LocalTime start, @Param("end") LocalTime end,
                             @Param("excludeId") Long excludeId);

    /** Same check for a patient (a patient cannot be in two places at once). */
    @Query("""
            select count(a) from Appointment a
            where a.patient.id = :patientId
              and a.appointmentDate = :date
              and a.status <> com.smarthealth.hms.domain.AppointmentStatus.CANCELLED
              and a.startTime < :end and a.endTime > :start
              and a.id <> :excludeId
            """)
    long countPatientOverlaps(@Param("patientId") Long patientId, @Param("date") LocalDate date,
                              @Param("start") LocalTime start, @Param("end") LocalTime end,
                              @Param("excludeId") Long excludeId);

    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusNot(Long doctorId, LocalDate date, AppointmentStatus status);

    List<Appointment> findByAppointmentDateOrderByStartTimeAsc(LocalDate date);

    List<Appointment> findByDoctorIdAndAppointmentDateOrderByStartTimeAsc(Long doctorId, LocalDate date);

    List<Appointment> findByPatientIdAndStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(
            Long patientId, AppointmentStatus status, LocalDate from);

    List<Appointment> findByDoctorIdAndStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(
            Long doctorId, AppointmentStatus status, LocalDate from);

    long countByAppointmentDate(LocalDate date);

    long countByStatus(AppointmentStatus status);

    long countByDoctorIdAndAppointmentDate(Long doctorId, LocalDate date);

    long countByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    long countByDoctorIdAndStatusAndAppointmentDateGreaterThanEqual(Long doctorId, AppointmentStatus status, LocalDate from);

    long countByPatientIdAndStatus(Long patientId, AppointmentStatus status);

    long countByStatusAndAppointmentDateBetween(AppointmentStatus status, LocalDate from, LocalDate to);

    long countByDoctorId(Long doctorId);

    long countByPatientId(Long patientId);

    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);

    @Query("""
            select count(a) from Appointment a
            where a.doctor.department.id = :departmentId
              and a.status <> com.smarthealth.hms.domain.AppointmentStatus.CANCELLED
            """)
    long countActiveByDepartment(@Param("departmentId") Long departmentId);
}
