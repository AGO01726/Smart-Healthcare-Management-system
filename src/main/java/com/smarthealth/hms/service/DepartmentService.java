package com.smarthealth.hms.service;

import com.smarthealth.hms.domain.Department;
import com.smarthealth.hms.dto.DepartmentForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.exception.ResourceNotFoundException;
import com.smarthealth.hms.repository.DepartmentRepository;
import com.smarthealth.hms.repository.DoctorRepository;
import com.smarthealth.hms.repository.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departments;
    private final DoctorRepository doctors;
    private final StaffRepository staff;

    public DepartmentService(DepartmentRepository departments, DoctorRepository doctors, StaffRepository staff) {
        this.departments = departments;
        this.doctors = doctors;
        this.staff = staff;
    }

    @Transactional(readOnly = true)
    public List<Department> findAll() {
        return departments.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Department get(Long id) {
        return departments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department not found"));
    }

    @Transactional
    public Department save(DepartmentForm form) {
        String name = form.getName().trim();
        Department department;
        if (form.getId() == null) {
            if (departments.existsByNameIgnoreCase(name)) {
                throw new BusinessRuleException("Department '" + name + "' already exists.");
            }
            department = new Department();
        } else {
            department = get(form.getId());
            if (!department.getName().equalsIgnoreCase(name) && departments.existsByNameIgnoreCase(name)) {
                throw new BusinessRuleException("Department '" + name + "' already exists.");
            }
        }
        department.setName(name);
        department.setDescription(StringUtils.hasText(form.getDescription()) ? form.getDescription().trim() : null);
        return departments.save(department);
    }

    @Transactional
    public void delete(Long id) {
        Department department = get(id);
        if (doctors.countByDepartmentId(id) > 0 || staff.countByDepartmentId(id) > 0) {
            throw new BusinessRuleException(
                    "Department '" + department.getName() + "' still has doctors or staff assigned and cannot be deleted.");
        }
        departments.delete(department);
    }

    @Transactional(readOnly = true)
    public long doctorCount(Long departmentId) {
        return doctors.countByDepartmentId(departmentId);
    }

    @Transactional(readOnly = true)
    public long staffCount(Long departmentId) {
        return staff.countByDepartmentId(departmentId);
    }
}
