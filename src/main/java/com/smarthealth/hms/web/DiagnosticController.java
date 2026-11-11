package com.smarthealth.hms.web;

import com.smarthealth.hms.domain.DiagnosticTest;
import com.smarthealth.hms.domain.Role;
import com.smarthealth.hms.domain.TestStatus;
import com.smarthealth.hms.dto.DiagnosticRequestForm;
import com.smarthealth.hms.dto.DiagnosticResultForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.security.AppUserDetails;
import com.smarthealth.hms.service.DiagnosticService;
import com.smarthealth.hms.service.DoctorService;
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
@RequestMapping("/diagnostics")
public class DiagnosticController {

    private static final int PAGE_SIZE = 10;
    private static final List<String> CATEGORIES =
            List.of("Hematology", "Biochemistry", "Radiology", "Microbiology", "Cardiology", "Other");

    private final DiagnosticService diagnosticService;
    private final PatientService patientService;
    private final DoctorService doctorService;

    public DiagnosticController(DiagnosticService diagnosticService, PatientService patientService,
                                DoctorService doctorService) {
        this.diagnosticService = diagnosticService;
        this.patientService = patientService;
        this.doctorService = doctorService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) TestStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal AppUserDetails actor, Model model) {
        Page<DiagnosticTest> result = diagnosticService.search(actor, q, status, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statuses", TestStatus.values());
        model.addAttribute("qs", QueryStrings.of("q", q, "status", status));
        model.addAttribute("path", "/diagnostics");
        return "diagnostics/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public String newForm(@RequestParam(required = false) Long patientId,
                          @AuthenticationPrincipal AppUserDetails actor, Model model) {
        DiagnosticRequestForm form = new DiagnosticRequestForm();
        form.setPatientId(patientId);
        model.addAttribute("form", form);
        return formView(model, actor);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public String create(@Valid @ModelAttribute("form") DiagnosticRequestForm form, BindingResult result,
                         @AuthenticationPrincipal AppUserDetails actor, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return formView(model, actor);
        }
        try {
            DiagnosticTest test = diagnosticService.request(form, actor);
            ra.addFlashAttribute("success", "Test requested: " + test.getTestName() + ".");
            return "redirect:/diagnostics/" + test.getId();
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return formView(model, actor);
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor, Model model) {
        DiagnosticTest test = diagnosticService.getAuthorized(id, actor);
        model.addAttribute("test", test);
        model.addAttribute("canViewResult", diagnosticService.canViewResult(test, actor));
        model.addAttribute("canEnterResult", diagnosticService.canEnterResult(test, actor));
        model.addAttribute("resultForm", new DiagnosticResultForm());
        return "diagnostics/view";
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public String updateStatus(@PathVariable Long id, @RequestParam TestStatus status,
                               @AuthenticationPrincipal AppUserDetails actor, RedirectAttributes ra) {
        try {
            diagnosticService.updateStatus(id, status, actor);
            ra.addFlashAttribute("success", "Status updated to " + status.getLabel() + ".");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/diagnostics/" + id;
    }

    @PostMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public String enterResult(@PathVariable Long id,
                              @Valid @ModelAttribute("resultForm") DiagnosticResultForm form, BindingResult result,
                              @AuthenticationPrincipal AppUserDetails actor, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", result.getFieldError("resultSummary") != null
                    ? result.getFieldError("resultSummary").getDefaultMessage() : "Invalid result");
            return "redirect:/diagnostics/" + id;
        }
        try {
            diagnosticService.enterResult(id, form.getResultSummary(), actor);
            ra.addFlashAttribute("success", "Result saved and test marked as completed.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/diagnostics/" + id;
    }

    private String formView(Model model, AppUserDetails actor) {
        model.addAttribute("patients", patientService.selectable(actor));
        model.addAttribute("categories", CATEGORIES);
        if (actor.getRole() == Role.ADMIN) {
            model.addAttribute("doctors", doctorService.findActive());
        }
        return "diagnostics/form";
    }
}
