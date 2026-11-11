package com.smarthealth.hms.web;

import com.smarthealth.hms.domain.Appointment;
import com.smarthealth.hms.domain.Department;
import com.smarthealth.hms.domain.Role;
import com.smarthealth.hms.domain.Staff;
import com.smarthealth.hms.dto.DepartmentForm;
import com.smarthealth.hms.dto.StaffForm;
import com.smarthealth.hms.dto.UserForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.security.AppUserDetails;
import com.smarthealth.hms.service.AppointmentService;
import com.smarthealth.hms.service.DepartmentService;
import com.smarthealth.hms.service.DoctorService;
import com.smarthealth.hms.service.StaffService;
import com.smarthealth.hms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Hospital administration: departments, staff, user accounts and the daily schedule. */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DepartmentService departmentService;
    private final StaffService staffService;
    private final UserService userService;
    private final AppointmentService appointmentService;
    private final DoctorService doctorService;

    public AdminController(DepartmentService departmentService, StaffService staffService, UserService userService,
                           AppointmentService appointmentService, DoctorService doctorService) {
        this.departmentService = departmentService;
        this.staffService = staffService;
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
    }

    // ---------------------------------------------------------------- departments

    @GetMapping("/departments")
    public String departments(@RequestParam(required = false) Long edit, Model model) {
        DepartmentForm form = edit == null ? new DepartmentForm() : DepartmentForm.from(departmentService.get(edit));
        model.addAttribute("form", form);
        return departmentsView(model);
    }

    @PostMapping("/departments")
    public String saveDepartment(@Valid @ModelAttribute("form") DepartmentForm form, BindingResult result,
                                 Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return departmentsView(model);
        }
        try {
            boolean creating = form.getId() == null;
            departmentService.save(form);
            ra.addFlashAttribute("success", creating ? "Department created." : "Department updated.");
            return "redirect:/admin/departments";
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return departmentsView(model);
        }
    }

    @PostMapping("/departments/{id}/delete")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes ra) {
        try {
            departmentService.delete(id);
            ra.addFlashAttribute("success", "Department deleted.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/departments";
    }

    private String departmentsView(Model model) {
        List<Department> departments = departmentService.findAll();
        Map<Long, Long> doctorCounts = new LinkedHashMap<>();
        Map<Long, Long> staffCounts = new LinkedHashMap<>();
        for (Department d : departments) {
            doctorCounts.put(d.getId(), departmentService.doctorCount(d.getId()));
            staffCounts.put(d.getId(), departmentService.staffCount(d.getId()));
        }
        model.addAttribute("departments", departments);
        model.addAttribute("doctorCounts", doctorCounts);
        model.addAttribute("staffCounts", staffCounts);
        return "admin/departments";
    }

    // ---------------------------------------------------------------- staff

    @GetMapping("/staff")
    public String staff(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) Long edit,
                        Model model) {
        StaffForm form = edit == null ? new StaffForm() : StaffForm.from(staffService.get(edit));
        model.addAttribute("form", form);
        return staffView(model, q);
    }

    @PostMapping("/staff")
    public String saveStaff(@Valid @ModelAttribute("form") StaffForm form, BindingResult result,
                            Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return staffView(model, "");
        }
        try {
            boolean creating = form.getId() == null;
            staffService.save(form);
            ra.addFlashAttribute("success", creating ? "Staff member added." : "Staff member updated.");
            return "redirect:/admin/staff";
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return staffView(model, "");
        }
    }

    @PostMapping("/staff/{id}/delete")
    public String deleteStaff(@PathVariable Long id, RedirectAttributes ra) {
        staffService.delete(id);
        ra.addFlashAttribute("success", "Staff member removed.");
        return "redirect:/admin/staff";
    }

    private String staffView(Model model, String q) {
        List<Staff> members = staffService.search(q);
        model.addAttribute("staffList", members);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("q", q);
        return "admin/staff";
    }

    // ---------------------------------------------------------------- users

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("form", new UserForm());
        return usersView(model);
    }

    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute("form") UserForm form, BindingResult result,
                             Model model, RedirectAttributes ra) {
        if (form.getRole() == Role.DOCTOR || form.getRole() == Role.PATIENT) {
            result.rejectValue("role", "invalid",
                    "Doctor accounts are created on the Doctors page and patient accounts on the Patients page.");
        }
        if (result.hasErrors()) {
            return usersView(model);
        }
        try {
            userService.createUser(form.getUsername(), form.getPassword(), form.getFullName(),
                    form.getEmail(), form.getRole());
            ra.addFlashAttribute("success", "Account created for " + form.getFullName() + ".");
            return "redirect:/admin/users";
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return usersView(model);
        }
    }

    @PostMapping("/users/{id}/enabled")
    public String setEnabled(@PathVariable Long id, @RequestParam boolean enabled,
                             @AuthenticationPrincipal AppUserDetails actor, RedirectAttributes ra) {
        try {
            userService.setEnabled(id, enabled, actor);
            ra.addFlashAttribute("success", enabled ? "Account enabled." : "Account disabled.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, @RequestParam String newPassword, RedirectAttributes ra) {
        try {
            userService.resetPassword(id, newPassword);
            ra.addFlashAttribute("success", "Password updated.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    private String usersView(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("roles", List.of(Role.ADMIN, Role.RECEPTIONIST));
        return "admin/users";
    }

    // ---------------------------------------------------------------- schedule

    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                           Model model) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<Appointment> appointments = appointmentService.schedule(day);
        Map<String, List<Appointment>> byDoctor = new LinkedHashMap<>();
        for (Appointment a : appointments) {
            byDoctor.computeIfAbsent(a.getDoctor().getFullName(), k -> new java.util.ArrayList<>()).add(a);
        }
        model.addAttribute("date", day);
        model.addAttribute("byDoctor", byDoctor);
        model.addAttribute("total", appointments.size());
        model.addAttribute("doctors", doctorService.findActive());
        return "admin/schedule";
    }
}
