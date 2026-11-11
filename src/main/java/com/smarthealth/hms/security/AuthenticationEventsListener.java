package com.smarthealth.hms.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Feeds Spring Security authentication events into {@link LoginAttemptService}. */
@Component
public class AuthenticationEventsListener {

    private final LoginAttemptService loginAttempts;

    public AuthenticationEventsListener(LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        loginAttempts.loginFailed(event.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttempts.loginSucceeded(event.getAuthentication().getName());
    }
}
