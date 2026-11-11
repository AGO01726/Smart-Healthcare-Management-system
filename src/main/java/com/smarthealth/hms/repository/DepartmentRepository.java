package com.smarthealth.hms.repository;

import com.smarthealth.hms.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Department> findAllByOrderByNameAsc();
}
