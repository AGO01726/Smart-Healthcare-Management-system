package com.smarthealth.hms.service;

import com.smarthealth.hms.domain.Department;
import com.smarthealth.hms.domain.Doctor;
import com.smarthealth.hms.domain.Role;
import com.smarthealth.hms.domain.User;
import com.smarthealth.hms.dto.DoctorForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.exception.ResourceNotFoundException;
import com.smarthealth.hms.repository.AppointmentRepository;
import com.smarthealth.hms.repository.DepartmentRepository;
import com.smarthealth.hms.repository.DiagnosticTestRepository;
import com.smarthealth.hms.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

@Service
public class DoctorService {

    private final DoctorRepository doctors;
    private final DepartmentRepository departments;
    private final AppointmentRepository appointments;
    private final DiagnosticTestRepository tests;
    private final UserService userService;

    public DoctorService(DoctorRepository doctors, DepartmentRepository departments,
                         AppointmentRepository appointments, DiagnosticTestRepository tests, UserService userService) {
        this.doctors = doctors;
        this.departments = departments;
        this.appointments = appointments;
        this.tests = tests;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Doctor> search(String q, Long departmentId) {
        String term = q == null ? "" : q.trim();
        return departmentId == null ? doctors.search(term) : doctors.searchByDepartment(term, departmentId);
    }

    @Transactional(readOnly = true)
    public List<Doctor> findActive() {
        return doctors.findAllActive();
    }

    @Transactional(readOnly = true)
    public Doctor get(Long id) {
        return doctors.findById(id).orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
    }

    @Transactional(readOnly = true)
    public Optional<Doctor> findByUserId(Long userId) {
        return doctors.findByUserId(userId);
    }

    @Transactional
    public Doctor create(DoctorForm form) {
        if (!StringUtils.hasText(form.getUsername()) || !StringUtils.hasText(form.getPassword())) {
            throw new BusinessRuleException("Username and password are required to create a doctor account.");
        }
        validateAvailability(form);
        Department department = department(form.getDepartmentId());
        User user = userService.createUser(form.getUsername(), form.getPassword(),
                form.getFullName(), form.getEmail(), Role.DOCTOR);
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        apply(doctor, form, department);
        return doctors.save(doctor);
    }

    @Transactional
    public Doctor update(Long id, DoctorForm form) {
        validateAvailability(form);
        Doctor doctor = get(id);
        userService.updateProfile(doctor.getUser(), form.getFullName(), form.getEmail());
        apply(doctor, form, department(form.getDepartmentId()));
        return doctors.save(doctor);
    }

    @Transactional
    public void delete(Long id) {
        Doctor doctor = get(id);
        if (appointments.countByDoctorId(id) > 0 || tests.countByRequestedById(id) > 0) {
            throw new BusinessRuleException(
                    "This doctor has appointments or test requests. Mark the doctor as inactive instead of deleting.");
        }
        User user = doctor.getUser();
        doctors.delete(doctor);
        userService.delete(user);
    }

    private Department department(Long id) {
        return departments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department not found"));
    }

    private void validateAvailability(DoctorForm form) {
        if (form.getAvailableDays() == null || form.getAvailableDays().isEmpty()) {
            throw new BusinessRuleException("Select at least one available day.");
        }
        if (form.getAvailableFrom() == null || form.getAvailableTo() == null
                || !form.getAvailableFrom().isBefore(form.getAvailableTo())) {
            throw new BusinessRuleException("Availability start time must be before the end time.");
        }
    }

    private void apply(Doctor d, DoctorForm f, Department department) {
        d.setDepartment(department);
        d.setSpecialization(f.getSpecialization().trim());
        d.setQualification(StringUtils.hasText(f.getQualification()) ? f.getQualification().trim() : null);
        d.setPhone(f.getPhone().trim());
        d.setExperienceYears(f.getExperienceYears());
        d.setConsultationFee(f.getConsultationFee());
        d.setAvailableFrom(f.getAvailableFrom());
        d.setAvailableTo(f.getAvailableTo());
        d.getAvailableDays().clear();
        d.getAvailableDays().addAll(new TreeSet<>(f.getAvailableDays()));
        d.setActive(f.isActive());
    }
}
