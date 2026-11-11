package com.smarthealth.hms.web;

import com.smarthealth.hms.domain.Appointment;
import com.smarthealth.hms.domain.AppointmentStatus;
import com.smarthealth.hms.domain.Role;
import com.smarthealth.hms.dto.AppointmentForm;
import com.smarthealth.hms.dto.RescheduleForm;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.security.AppUserDetails;
import com.smarthealth.hms.service.AppointmentService;
import com.smarthealth.hms.service.DoctorService;
import com.smarthealth.hms.service.PatientService;
import com.smarthealth.hms.util.QueryStrings;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentService appointmentService;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public AppointmentController(AppointmentService appointmentService, DoctorService doctorService,
                                 PatientService patientService) {
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) AppointmentStatus status,
                       @RequestParam(required = false) Long doctorId,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal AppUserDetails actor, Model model) {
        Page<Appointment> result = appointmentService.search(actor, q, status, doctorId, date, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("date", date);
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("doctors", doctorService.findActive());
        model.addAttribute("qs", QueryStrings.of("q", q, "status", status, "doctorId", doctorId, "date", date));
        model.addAttribute("path", "/appointments");
        return "appointments/list";
    }

    /** JSON list of free start times ("HH:mm") used by the booking form. */
    @GetMapping("/slots")
    @ResponseBody
    public List<String> slots(@RequestParam Long doctorId,
                              @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                              @RequestParam(required = false) Long excludeId) {
        return appointmentService.freeSlots(doctorId, date, excludeId).stream().map(t -> t.format(HHMM)).toList();
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','PATIENT')")
    public String newForm(@RequestParam(required = false) Long patientId,
                          @RequestParam(required = false) Long doctorId,
                          @AuthenticationPrincipal AppUserDetails actor, Model model) {
        AppointmentForm form = new AppointmentForm();
        form.setPatientId(patientId);
        form.setDoctorId(doctorId);
        form.setAppointmentDate(LocalDate.now());
        model.addAttribute("form", form);
        return formView(model, actor);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','PATIENT')")
    public String create(@Valid @ModelAttribute("form") AppointmentForm form, BindingResult result,
                         @AuthenticationPrincipal AppUserDetails actor, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return formView(model, actor);
        }
        try {
            Appointment appointment = appointmentService.book(form, actor);
            ra.addFlashAttribute("success", "Appointment booked with " + appointment.getDoctor().getFullName()
                    + " on " + appointment.getAppointmentDate() + " at " + appointment.getStartTime().format(HHMM) + ".");
            return "redirect:/appointments/" + appointment.getId();
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return formView(model, actor);
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor, Model model) {
        model.addAttribute("appointment", appointmentService.getAuthorized(id, actor));
        return "appointments/view";
    }

    @GetMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','PATIENT')")
    public String rescheduleForm(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor, Model model) {
        Appointment appointment = appointmentService.getAuthorized(id, actor);
        RescheduleForm form = new RescheduleForm();
        form.setAppointmentDate(appointment.getAppointmentDate());
        form.setStartTime(appointment.getStartTime());
        model.addAttribute("form", form);
        return rescheduleView(model, appointment);
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','PATIENT')")
    public String reschedule(@PathVariable Long id, @Valid @ModelAttribute("form") RescheduleForm form,
                             BindingResult result, @AuthenticationPrincipal AppUserDetails actor,
                             Model model, RedirectAttributes ra) {
        Appointment appointment = appointmentService.getAuthorized(id, actor);
        if (result.hasErrors()) {
            return rescheduleView(model, appointment);
        }
        try {
            appointmentService.reschedule(id, form, actor);
            ra.addFlashAttribute("success", "Appointment rescheduled.");
            return "redirect:/appointments/" + id;
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return rescheduleView(model, appointment);
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor, RedirectAttributes ra) {
        try {
            appointmentService.cancel(id, actor);
            ra.addFlashAttribute("success", "Appointment cancelled.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/appointments/" + id;
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public String complete(@PathVariable Long id, @RequestParam(required = false) String notes,
                           @AuthenticationPrincipal AppUserDetails actor, RedirectAttributes ra) {
        if (notes != null && notes.length() > 500) {
            ra.addFlashAttribute("error", "Notes can be at most 500 characters.");
            return "redirect:/appointments/" + id;
        }
        try {
            appointmentService.complete(id, notes, actor);
            ra.addFlashAttribute("success", "Appointment marked as completed.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/appointments/" + id;
    }

    private String formView(Model model, AppUserDetails actor) {
        model.addAttribute("doctors", doctorService.findActive());
        if (actor.getRole() != Role.PATIENT) {
            model.addAttribute("patients", patientService.selectable(actor));
        }
        model.addAttribute("slotMinutes", appointmentService.getSlotMinutes());
        return "appointments/form";
    }

    private String rescheduleView(Model model, Appointment appointment) {
        model.addAttribute("appointment", appointment);
        model.addAttribute("slotMinutes", appointmentService.getSlotMinutes());
        return "appointments/reschedule";
    }
}
