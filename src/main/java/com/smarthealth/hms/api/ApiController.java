package com.smarthealth.hms.api;

import com.smarthealth.hms.api.ApiDtos.AppointmentDto;
import com.smarthealth.hms.api.ApiDtos.DiagnosticDto;
import com.smarthealth.hms.api.ApiDtos.DoctorDto;
import com.smarthealth.hms.api.ApiDtos.PageDto;
import com.smarthealth.hms.api.ApiDtos.PatientDto;
import com.smarthealth.hms.domain.AppointmentStatus;
import com.smarthealth.hms.domain.TestStatus;
import com.smarthealth.hms.dto.AppointmentForm;
import com.smarthealth.hms.dto.PatientForm;
import com.smarthealth.hms.dto.RescheduleForm;
import com.smarthealth.hms.security.AppUserDetails;
import com.smarthealth.hms.service.AppointmentService;
import com.smarthealth.hms.service.DashboardService;
import com.smarthealth.hms.service.DiagnosticService;
import com.smarthealth.hms.service.DoctorService;
import com.smarthealth.hms.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * JSON REST API (HTTP Basic authentication). Uses the same services and authorization rules as the web UI.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final int PAGE_SIZE = 20;
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final DiagnosticService diagnosticService;
    private final DashboardService dashboardService;

    public ApiController(PatientService patientService, DoctorService doctorService,
                         AppointmentService appointmentService, DiagnosticService diagnosticService,
                         DashboardService dashboardService) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.diagnosticService = diagnosticService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AppUserDetails actor) {
        return Map.of("username", actor.getUsername(), "fullName", actor.getFullName(),
                "role", actor.getRole().name());
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@AuthenticationPrincipal AppUserDetails actor) {
        Map<String, Object> stats = new java.util.LinkedHashMap<>(dashboardService.forUser(actor));
        // keep the JSON simple: lists of entities are not part of the API response
        stats.entrySet().removeIf(e -> !(e.getValue() instanceof Number || e.getValue() instanceof Boolean));
        return stats;
    }

    // ------------------------------------------------------------ patients

    @GetMapping("/patients")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public PageDto<PatientDto> patients(@RequestParam(defaultValue = "") String q,
                                        @RequestParam(defaultValue = "0") int page,
                                        @AuthenticationPrincipal AppUserDetails actor) {
        return PageDto.of(patientService.search(q, page, PAGE_SIZE, actor), PatientDto::from);
    }

    @GetMapping("/patients/{id}")
    public PatientDto patient(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor) {
        return PatientDto.from(patientService.getAuthorized(id, actor));
    }

    @PostMapping("/patients")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<PatientDto> registerPatient(@Valid @RequestBody PatientForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientDto.from(patientService.register(form)));
    }

    // ------------------------------------------------------------ doctors

    @GetMapping("/doctors")
    public List<DoctorDto> doctors(@RequestParam(defaultValue = "") String q,
                                   @RequestParam(required = false) Long departmentId) {
        return doctorService.search(q, departmentId).stream().map(DoctorDto::from).toList();
    }

    @GetMapping("/doctors/{id}/slots")
    public List<String> slots(@PathVariable Long id,
                              @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return appointmentService.freeSlots(id, date, null).stream().map(t -> t.format(HHMM)).toList();
    }

    // ------------------------------------------------------------ appointments

    @GetMapping("/appointments")
    public PageDto<AppointmentDto> appointments(@RequestParam(defaultValue = "") String q,
                                                @RequestParam(required = false) AppointmentStatus status,
                                                @RequestParam(required = false) Long doctorId,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                @RequestParam(defaultValue = "0") int page,
                                                @AuthenticationPrincipal AppUserDetails actor) {
        return PageDto.of(appointmentService.search(actor, q, status, doctorId, date, page, PAGE_SIZE),
                AppointmentDto::from);
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','PATIENT')")
    public ResponseEntity<AppointmentDto> book(@Valid @RequestBody AppointmentForm form,
                                               @AuthenticationPrincipal AppUserDetails actor) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppointmentDto.from(appointmentService.book(form, actor)));
    }

    @PutMapping("/appointments/{id}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','PATIENT')")
    public AppointmentDto reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleForm form,
                                     @AuthenticationPrincipal AppUserDetails actor) {
        return AppointmentDto.from(appointmentService.reschedule(id, form, actor));
    }

    @DeleteMapping("/appointments/{id}")
    public AppointmentDto cancel(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor) {
        return AppointmentDto.from(appointmentService.cancel(id, actor));
    }

    // ------------------------------------------------------------ diagnostics

    @GetMapping("/diagnostics")
    public PageDto<DiagnosticDto> diagnostics(@RequestParam(defaultValue = "") String q,
                                              @RequestParam(required = false) TestStatus status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @AuthenticationPrincipal AppUserDetails actor) {
        return PageDto.of(diagnosticService.search(actor, q, status, page, PAGE_SIZE),
                t -> DiagnosticDto.from(t, diagnosticService.canViewResult(t, actor)));
    }
}
