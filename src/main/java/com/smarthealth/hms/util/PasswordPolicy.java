package com.smarthealth.hms.util;

import com.smarthealth.hms.exception.BusinessRuleException;

/** Server-side password rules: 8-64 characters, at least one letter and one digit. */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        boolean ok = password != null
                && password.length() >= 8
                && password.length() <= 64
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
        if (!ok) {
            throw new BusinessRuleException(
                    "Password must be 8-64 characters long and contain at least one letter and one digit.");
        }
    }
}
