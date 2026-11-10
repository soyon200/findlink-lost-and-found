package com.example.findlink.util;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * পাওয়া জিনিসের জন্য অ্যাপ নিজেই বানানো হ্যান্ডওভার কোড।
 *
 * যিনি জিনিসটি পেয়েছেন তিনিই এই কোডটি শুধু তাঁকে দেবেন যাঁকে সত্যিই জিনিসটি
 * हातে তুলে দিচ্ছেন। দাবিদার কোডটি অ্যাপে বসিয়ে প্রমাণ করবেন — ফলে যাঁর কোনো
 * LOST পোস্ট নেই (তাই সিকিউরিটি কোডও নেই) তিনিও যাচাই করতে পারবেন।
 */
public final class ClaimCode {

    private static final SecureRandom RANDOM = new SecureRandom();

    private ClaimCode() {
    }

    public static String generate() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        String trimmed = code.trim();
        return trimmed.matches("\\d{6}");
    }

    public static boolean matches(String storedCode, String enteredCode) {
        if (storedCode == null || storedCode.trim().isEmpty() || !isValid(enteredCode)) {
            return false;
        }
        return storedCode.trim().toLowerCase(Locale.US)
                .equals(enteredCode.trim().toLowerCase(Locale.US));
    }
}
