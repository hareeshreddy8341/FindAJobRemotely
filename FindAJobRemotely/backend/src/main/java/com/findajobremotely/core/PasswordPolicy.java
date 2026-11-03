package com.findajobremotely.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Password rules. Length is capped at 64 because BCrypt only uses the first 72 bytes. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 64;

    private static final Set<String> COMMON = Set.of(
            "password", "password1", "password123", "password1234", "1234567890", "12345678910",
            "qwertyuiop", "qwerty12345", "iloveyou123", "letmein1234", "welcome1234", "admin12345",
            "abcd123456", "passw0rd123", "changeme123");

    private PasswordPolicy() {
    }

    public static List<String> problems(String password, String email) {
        List<String> out = new ArrayList<>();
        if (password == null || password.length() < MIN_LENGTH) {
            out.add("Password must be at least " + MIN_LENGTH + " characters long");
            return out;
        }
        if (password.length() > MAX_LENGTH) {
            out.add("Password must be at most " + MAX_LENGTH + " characters long");
            return out;
        }
        boolean letter = false;
        boolean digit = false;
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isLetter(ch)) {
                letter = true;
            } else if (Character.isDigit(ch)) {
                digit = true;
            }
        }
        if (!letter || !digit) {
            out.add("Password must contain at least one letter and one number");
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (COMMON.contains(lower)) {
            out.add("This password is too common");
        }
        if (email != null && email.contains("@")) {
            String local = email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT);
            if (local.length() >= 4 && lower.contains(local)) {
                out.add("Password must not contain your email name");
            }
        }
        return out;
    }

    public static String firstProblem(String password, String email) {
        List<String> p = problems(password, email);
        return p.isEmpty() ? null : p.get(0);
    }
}
