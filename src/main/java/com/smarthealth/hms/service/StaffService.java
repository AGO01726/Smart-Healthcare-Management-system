package com.smarthealth.hms.service;

import com.smarthealth.hms.domain.Staff;
import com.smarthealth.hms.dto.StaffForm;
import com.smarthealth.hms.exception.ResourceNotFoundException;
import com.smarthealth.hms.repository.DepartmentRepository;
import com.smarthealth.hms.repository.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class StaffService {

    private final StaffRepository staff;
    private final DepartmentRepository departments;

    public StaffService(StaffRepository staff, DepartmentRepository departments) {
        this.staff = staff;
        this.departments = departments;
    }

    @Transactional(readOnly = true)
    public List<Staff> search(String q) {
        return staff.search(q == null ? "" : q.trim());
    }

    @Transactional(readOnly = true)
    public Staff get(Long id) {
        return staff.findById(id).orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
    }

    @Transactional
    public Staff save(StaffForm form) {
        Staff member = form.getId() == null ? new Staff() : get(form.getId());
        member.setFullName(form.getFullName().trim());
        member.setDesignation(form.getDesignation().trim());
        member.setDepartment(form.getDepartmentId() == null ? null
                : departments.findById(form.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found")));
        member.setPhone(form.getPhone().trim());
        member.setEmail(StringUtils.hasText(form.getEmail()) ? form.getEmail().trim() : null);
        member.setJoinedDate(form.getJoinedDate());
        member.setActive(form.isActive());
        return staff.save(member);
    }

    @Transactional
    public void delete(Long id) {
        staff.delete(get(id));
    }
}
