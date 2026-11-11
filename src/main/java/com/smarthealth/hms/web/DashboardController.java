package com.smarthealth.hms.web;

import com.smarthealth.hms.security.AppUserDetails;
import com.smarthealth.hms.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserDetails actor, Model model) {
        model.addAttribute("stats", dashboardService.forUser(actor));
        return switch (actor.getRole()) {
            case ADMIN -> "dashboard/admin";
            case RECEPTIONIST -> "dashboard/reception";
            case DOCTOR -> "dashboard/doctor";
            case PATIENT -> "dashboard/patient";
        };
    }
}
