package com.hackathon.leave.security;

import com.hackathon.leave.exception.ApiException;

/** Minimum password rules: 10+ characters with at least one letter and one digit. */
public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static void validate(String password) {
        boolean ok = password != null && password.length() >= 10 && password.length() <= 128
                && password.chars().anyMatch(Character::isLetter) && password.chars().anyMatch(Character::isDigit);
        if (!ok) {
            throw ApiException.badRequest("WEAK_PASSWORD",
                    "Password must be 10-128 characters and contain at least one letter and one digit");
        }
    }
}
