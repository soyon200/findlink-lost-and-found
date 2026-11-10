package com.example.findlink.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * মালিকানা প্রমাণের সিকিউরিটি কোড।
 *
 * ইউজার LOST পোস্ট করার সময় ৪–৬ ডিজিটের একটি গোপন কোড দেন। ডেটাবেসে কোডটি
 * প্লেইন টেক্সটে রাখা হয় না — শুধু SHA-256 হ্যাশ রাখা হয়, এবং হ্যাশটি সেই
 * পোস্টের itemId দিয়ে salt করা হয়। ফলে একই কোড দুইজনের ক্ষেত্রে একই হ্যাশ
 * তৈরি করে না, আর কেউ শুধু হ্যাশ দেখে কোড অনুমান করতে পারে না।
 */
public final class SecurityCode {

    public static final int MIN_LENGTH = 4;
    public static final int MAX_LENGTH = 6;

    private SecurityCode() {
    }

    /**
     * শুধু ৪–৬ ডিজিটের সংখ্যা গ্রহণযোগ্য।
     */
    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        String trimmed = code.trim();
        return trimmed.length() >= MIN_LENGTH
                && trimmed.length() <= MAX_LENGTH
                && trimmed.matches("\\d+");
    }

    /**
     * itemId দিয়ে salt করা SHA-256 হ্যাশ (হেক্স স্ট্রিং)।
     */
    public static String hash(String itemId, String code) {
        if (itemId == null || code == null) {
            return "";
        }
        String material = itemId + ":" + code.trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    /**
     * ইউজারের কোনো LOST পোস্টের হ্যাশের সাথে কোডটি মেলে কি না।
     */
    public static boolean matches(String storedHash, String itemId, String code) {
        if (storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        String computed = hash(itemId, code);
        if (computed.isEmpty()) {
            return false;
        }
        return constantTimeEquals(storedHash, computed);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
