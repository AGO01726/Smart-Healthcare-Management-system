package com.smarthealth.hms.web;

import com.smarthealth.hms.dto.PatientForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.exception.ResourceNotFoundException;
import com.smarthealth.hms.domain.Gender;
import com.smarthealth.hms.security.AppUserDetails;
import com.smarthealth.hms.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final PatientService patientService;
    private final boolean registrationEnabled;

    public AuthController(PatientService patientService,
                          @Value("${hms.registration.enabled:true}") boolean registrationEnabled) {
        this.patientService = patientService;
        this.registrationEnabled = registrationEnabled;
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal AppUserDetails user, Model model) {
        if (user != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("registrationEnabled", registrationEnabled);
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        requireRegistration();
        model.addAttribute("form", new PatientForm());
        model.addAttribute("genders", Gender.values());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") PatientForm form, BindingResult result, Model model) {
        requireRegistration();
        model.addAttribute("genders", Gender.values());
        if (!StringUtils.hasText(form.getUsername())) {
            result.rejectValue("username", "required", "Username is required");
        }
        if (!StringUtils.hasText(form.getPassword())) {
            result.rejectValue("password", "required", "Password is required");
        }
        if (!StringUtils.hasText(form.getEmail())) {
            result.rejectValue("email", "required", "Email is required");
        }
        if (result.hasErrors()) {
            return "register";
        }
        try {
            patientService.register(form);
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }

    private void requireRegistration() {
        if (!registrationEnabled) {
            throw new ResourceNotFoundException("Registration is disabled");
        }
    }
}
