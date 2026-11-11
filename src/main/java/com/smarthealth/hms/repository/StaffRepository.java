package com.smarthealth.hms.repository;

import com.smarthealth.hms.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    long countByDepartmentId(Long departmentId);

    long countByActiveTrue();

    @Query("""
            select s from Staff s
            where lower(s.fullName) like lower(concat('%', :q, '%'))
               or lower(s.designation) like lower(concat('%', :q, '%'))
            order by s.fullName
            """)
    List<Staff> search(@Param("q") String q);
}
