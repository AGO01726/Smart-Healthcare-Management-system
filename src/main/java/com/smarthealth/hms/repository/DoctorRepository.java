package com.smarthealth.hms.repository;

import com.smarthealth.hms.domain.Doctor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserId(Long userId);

    long countByDepartmentId(Long departmentId);

    long countByActiveTrue();

    /** Row lock used while booking so two requests cannot book the same doctor slot at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);

    @Query("select d from Doctor d where d.active = true order by d.user.fullName")
    List<Doctor> findAllActive();

    @Query("""
            select d from Doctor d join d.user u join d.department dep
            where lower(u.fullName) like lower(concat('%', :q, '%'))
               or lower(d.specialization) like lower(concat('%', :q, '%'))
               or lower(dep.name) like lower(concat('%', :q, '%'))
            order by u.fullName
            """)
    List<Doctor> search(@Param("q") String q);

    @Query("""
            select d from Doctor d join d.user u join d.department dep
            where dep.id = :departmentId
              and (lower(u.fullName) like lower(concat('%', :q, '%'))
               or lower(d.specialization) like lower(concat('%', :q, '%')))
            order by u.fullName
            """)
    List<Doctor> searchByDepartment(@Param("q") String q, @Param("departmentId") Long departmentId);
}
