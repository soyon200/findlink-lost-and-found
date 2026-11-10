package com.example.findlink.util;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * অফিসে জমা দেওয়ার সময় তৈরি হওয়া রিসিপ্ট কোড, যেমন: FL-4821KM
 */
public final class DepositCode {

    public static final String PREFIX = "FL-";
    private static final String PATTERN = "^FL-\\d{4}[A-Z]{2}$";

    private DepositCode() {
    }

    public static String generate() {
        SecureRandom random = new SecureRandom();
        int digits = 1000 + random.nextInt(9000);
        char first = (char) ('A' + random.nextInt(26));
        char second = (char) ('A' + random.nextInt(26));
        return String.format(Locale.US, "%s%d%c%c", PREFIX, digits, first, second);
    }

    public static boolean isValid(String code) {
        return code != null && code.matches(PATTERN);
    }
}
