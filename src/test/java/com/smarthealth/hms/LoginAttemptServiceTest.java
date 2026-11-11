package com.smarthealth.hms;

import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.security.LoginAttemptService;
import com.smarthealth.hms.util.PasswordPolicy;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests (no Spring context). */
class LoginAttemptServiceTest {

    /** Clock whose time can be moved forward by the test. */
    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2030-01-01T10:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void accountIsLockedAfterFiveFailuresAndUnlocksLater() {
        MutableClock clock = new MutableClock();
        LoginAttemptService service = new LoginAttemptService(clock);
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) {
            service.loginFailed("Alice");
        }
        assertThat(service.isBlocked("alice")).isFalse();
        service.loginFailed("ALICE");
        assertThat(service.isBlocked("alice")).isTrue();

        clock.advance(LoginAttemptService.LOCK_DURATION.plusSeconds(1));
        assertThat(service.isBlocked("alice")).isFalse();
    }

    @Test
    void successfulLoginResetsFailureCounter() {
        LoginAttemptService service = new LoginAttemptService(new MutableClock());
        for (int i = 0; i < 4; i++) {
            service.loginFailed("bob");
        }
        service.loginSucceeded("bob");
        service.loginFailed("bob");
        assertThat(service.isBlocked("bob")).isFalse();
    }

    @Test
    void passwordPolicyAcceptsStrongAndRejectsWeakPasswords() {
        assertThatCode(() -> PasswordPolicy.validate("Strong#123")).doesNotThrowAnyException();
        assertThatThrownBy(() -> PasswordPolicy.validate("short1")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("onlyletters")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("12345678")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate(null)).isInstanceOf(BusinessRuleException.class);
    }
}
