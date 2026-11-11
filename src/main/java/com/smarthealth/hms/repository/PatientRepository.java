package com.smarthealth.hms.repository;

import com.smarthealth.hms.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUserId(Long userId);

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    @Query("""
            select p from Patient p
            where lower(p.firstName) like lower(concat('%', :q, '%'))
               or lower(p.lastName) like lower(concat('%', :q, '%'))
               or lower(concat(p.firstName, ' ', p.lastName)) like lower(concat('%', :q, '%'))
               or lower(p.patientCode) like lower(concat('%', :q, '%'))
               or p.phone like concat('%', :q, '%')
               or lower(coalesce(p.email, '')) like lower(concat('%', :q, '%'))
            order by p.id desc
            """)
    Page<Patient> search(@Param("q") String q, Pageable pageable);

    /** Search restricted to patients that have an appointment or test request with the given doctor. */
    @Query("""
            select p from Patient p
            where (lower(p.firstName) like lower(concat('%', :q, '%'))
               or lower(p.lastName) like lower(concat('%', :q, '%'))
               or lower(concat(p.firstName, ' ', p.lastName)) like lower(concat('%', :q, '%'))
               or lower(p.patientCode) like lower(concat('%', :q, '%'))
               or p.phone like concat('%', :q, '%'))
              and (exists (select a.id from Appointment a where a.patient = p and a.doctor.id = :doctorId)
                or exists (select t.id from DiagnosticTest t where t.patient = p and t.requestedBy.id = :doctorId))
            order by p.id desc
            """)
    Page<Patient> searchForDoctor(@Param("q") String q, @Param("doctorId") Long doctorId, Pageable pageable);
}
