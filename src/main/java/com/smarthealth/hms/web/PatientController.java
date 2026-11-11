package com.smarthealth.hms.web;

import com.smarthealth.hms.domain.Gender;
import com.smarthealth.hms.domain.Patient;
import com.smarthealth.hms.dto.PatientForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.security.AppUserDetails;
import com.smarthealth.hms.service.AppointmentService;
import com.smarthealth.hms.service.DiagnosticService;
import com.smarthealth.hms.service.PatientService;
import com.smarthealth.hms.util.QueryStrings;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/patients")
public class PatientController {

    private static final int PAGE_SIZE = 10;
    private static final List<String> BLOOD_GROUPS = List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");

    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final DiagnosticService diagnosticService;

    public PatientController(PatientService patientService, AppointmentService appointmentService,
                             DiagnosticService diagnosticService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.diagnosticService = diagnosticService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal AppUserDetails actor, Model model) {
        Page<Patient> result = patientService.search(q, page, PAGE_SIZE, actor);
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("qs", QueryStrings.of("q", q));
        model.addAttribute("path", "/patients");
        return "patients/list";
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public String myRecord(@AuthenticationPrincipal AppUserDetails actor, RedirectAttributes ra) {
        return patientService.findByUserId(actor.getUserId())
                .map(p -> "redirect:/patients/" + p.getId())
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "No patient record is linked to your account. Please contact reception.");
                    return "redirect:/dashboard";
                });
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String newForm(Model model) {
        model.addAttribute("form", new PatientForm());
        return formView(model, false, null);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String create(@Valid @ModelAttribute("form") PatientForm form, BindingResult result,
                         Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return formView(model, false, null);
        }
        try {
            Patient patient = patientService.register(form);
            ra.addFlashAttribute("success", "Patient registered: " + patient.getFullName()
                    + " (" + patient.getPatientCode() + ")");
            return "redirect:/patients/" + patient.getId();
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return formView(model, false, null);
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor, Model model) {
        Patient patient = patientService.getAuthorized(id, actor);
        model.addAttribute("patient", patient);
        model.addAttribute("appointments", appointmentService.forPatient(id, actor));
        model.addAttribute("tests", diagnosticService.listForPatient(id, actor));
        return "patients/view";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", PatientForm.from(patientService.get(id)));
        return formView(model, true, id);
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") PatientForm form,
                         BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return formView(model, true, id);
        }
        try {
            patientService.update(id, form);
            ra.addFlashAttribute("success", "Patient record updated.");
            return "redirect:/patients/" + id;
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return formView(model, true, id);
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            patientService.delete(id);
            ra.addFlashAttribute("success", "Patient deleted.");
            return "redirect:/patients";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/patients/" + id;
        }
    }

    private String formView(Model model, boolean editing, Long id) {
        model.addAttribute("editing", editing);
        model.addAttribute("patientId", id);
        model.addAttribute("genders", Gender.values());
        model.addAttribute("bloodGroups", BLOOD_GROUPS);
        return "patients/form";
    }
}
