package com.smarthealth.hms.web;

import com.smarthealth.hms.domain.Doctor;
import com.smarthealth.hms.dto.DoctorForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.service.DepartmentService;
import com.smarthealth.hms.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;

@Controller
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final DepartmentService departmentService;

    public DoctorController(DoctorService doctorService, DepartmentService departmentService) {
        this.doctorService = doctorService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) Long departmentId, Model model) {
        model.addAttribute("doctors", doctorService.search(q, departmentId));
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("q", q);
        model.addAttribute("departmentId", departmentId);
        return "doctors/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Doctor doctor = doctorService.get(id);
        model.addAttribute("doctor", doctor);
        return "doctors/view";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("form", new DoctorForm());
        return formView(model, false, null);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String create(@Valid @ModelAttribute("form") DoctorForm form, BindingResult result,
                         Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return formView(model, false, null);
        }
        try {
            Doctor doctor = doctorService.create(form);
            ra.addFlashAttribute("success", "Doctor profile created for " + doctor.getFullName() + ".");
            return "redirect:/doctors/" + doctor.getId();
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return formView(model, false, null);
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", DoctorForm.from(doctorService.get(id)));
        return formView(model, true, id);
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") DoctorForm form,
                         BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return formView(model, true, id);
        }
        try {
            doctorService.update(id, form);
            ra.addFlashAttribute("success", "Doctor profile updated.");
            return "redirect:/doctors/" + id;
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return formView(model, true, id);
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            doctorService.delete(id);
            ra.addFlashAttribute("success", "Doctor deleted.");
            return "redirect:/doctors";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/doctors/" + id;
        }
    }

    private String formView(Model model, boolean editing, Long id) {
        model.addAttribute("editing", editing);
        model.addAttribute("doctorId", id);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("days", DayOfWeek.values());
        return "doctors/form";
    }
}
