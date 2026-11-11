package com.smarthealth.hms;

import com.smarthealth.hms.domain.Patient;
import com.smarthealth.hms.domain.User;
import com.smarthealth.hms.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** End-to-end checks of authentication, role-based access and the main web workflows (MockMvc + H2). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired TestData data;
    @Autowired PatientRepository patientRepository;

    @Test
    void anonymousUsersAreRedirectedToLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/patients")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void formLoginWorksWithCorrectAndWrongPassword() throws Exception {
        User admin = data.admin();
        mvc.perform(formLogin("/login").user(admin.getUsername()).password(TestData.PASSWORD))
                .andExpect(authenticated()).andExpect(redirectedUrl("/dashboard"));
        mvc.perform(formLogin("/login").user(admin.getUsername()).password("wrong-Password1"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void everyRoleSeesItsOwnDashboard() throws Exception {
        mvc.perform(get("/dashboard").with(user(data.principal(data.admin()))))
                .andExpect(status().isOk()).andExpect(view().name("dashboard/admin"));
        mvc.perform(get("/dashboard").with(user(data.principal(data.receptionist()))))
                .andExpect(status().isOk()).andExpect(view().name("dashboard/reception"));
        mvc.perform(get("/dashboard").with(user(data.principal(data.doctor().getUser()))))
                .andExpect(status().isOk()).andExpect(view().name("dashboard/doctor"));
        mvc.perform(get("/dashboard").with(user(data.principal(data.patientWithLogin().getUser()))))
                .andExpect(status().isOk()).andExpect(view().name("dashboard/patient"));
    }

    @Test
    void adminAreaIsRestrictedToAdministrators() throws Exception {
        var patient = data.principal(data.patientWithLogin().getUser());
        var reception = data.principal(data.receptionist());
        var admin = data.principal(data.admin());
        mvc.perform(get("/admin/departments").with(user(patient))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/users").with(user(reception))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/departments").with(user(admin))).andExpect(status().isOk());
        mvc.perform(get("/admin/schedule").with(user(admin))).andExpect(status().isOk());
        mvc.perform(get("/admin/staff").with(user(admin))).andExpect(status().isOk());
        mvc.perform(get("/admin/users").with(user(admin))).andExpect(status().isOk());
    }

    @Test
    void patientsCannotBrowseThePatientRegistry() throws Exception {
        var patient = data.principal(data.patientWithLogin().getUser());
        mvc.perform(get("/patients").with(user(patient))).andExpect(status().isForbidden());
        mvc.perform(get("/patients/new").with(user(patient))).andExpect(status().isForbidden());
    }

    @Test
    void patientCannotOpenAnotherPatientsRecord() throws Exception {
        Patient mine = data.patientWithLogin();
        Patient other = data.patientWithLogin();
        mvc.perform(get("/patients/" + mine.getId()).with(user(data.principal(mine.getUser()))))
                .andExpect(status().isOk()).andExpect(view().name("patients/view"));
        mvc.perform(get("/patients/" + other.getId()).with(user(data.principal(mine.getUser()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void receptionistRegistersPatientThroughTheWebForm() throws Exception {
        var reception = data.principal(data.receptionist());
        long before = patientRepository.count();
        mvc.perform(post("/patients").with(user(reception)).with(csrf())
                        .param("firstName", "Asha").param("lastName", "Webform")
                        .param("gender", "FEMALE").param("dateOfBirth", "1992-05-17")
                        .param("phone", "9000000077").param("email", "asha.webform@example.com"))
                .andExpect(status().is3xxRedirection());
        assertThat(patientRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void invalidPatientFormIsRedisplayedWithoutSaving() throws Exception {
        var reception = data.principal(data.receptionist());
        long before = patientRepository.count();
        mvc.perform(post("/patients").with(user(reception)).with(csrf())
                        .param("firstName", "").param("lastName", "X").param("phone", "abc"))
                .andExpect(status().isOk()).andExpect(view().name("patients/form"))
                .andExpect(model().attributeHasFieldErrors("form", "firstName", "phone", "gender", "dateOfBirth"));
        assertThat(patientRepository.count()).isEqualTo(before);
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        var reception = data.principal(data.receptionist());
        mvc.perform(post("/patients").with(user(reception)).param("firstName", "A")).andExpect(status().isForbidden());
    }

    @Test
    void restApiRequiresBasicAuthAndHonoursRoles() throws Exception {
        User admin = data.admin();
        Patient patient = data.patientWithLogin();
        mvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/patients").with(httpBasic(admin.getUsername(), TestData.PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/patients").with(httpBasic(patient.getUser().getUsername(), TestData.PASSWORD)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/me").with(httpBasic(admin.getUsername(), "bad-Password1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRecordShowsSafeErrorPage() throws Exception {
        mvc.perform(get("/patients/999999").with(user(data.principal(data.admin()))))
                .andExpect(status().isNotFound()).andExpect(view().name("error"));
    }
}
