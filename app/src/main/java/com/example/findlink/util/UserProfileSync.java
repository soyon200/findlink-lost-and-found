package com.example.findlink.util;

import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * প্রতিটি অ্যাকাউন্টের জন্য users/{uid} ডকুমেন্ট নিশ্চিত করে — যাতে অ্যাপের
 * কোথাও "FindLink user" না দেখিয়ে সবসময় নাম ও জিমেইল দেখা যায়।
 *
 * পুরোনো অ্যাকাউন্টে ডকুমেন্ট না থাকলে বা নাম খালি থাকলে ইমেইল থেকে নাম তৈরি
 * করে সেভ করা হয়, আর ইমেইল সবসময় লেখা হয়।
 */
public final class UserProfileSync {

    public static final String FALLBACK_NAME = "FindLink user";

    private UserProfileSync() {
    }

    public static void ensureProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        String uid = user.getUid();
        String authEmail = user.getEmail() == null ? "" : user.getEmail().trim();
        DocumentReference ref = FirebaseFirestore.getInstance().collection("users").document(uid);

        ref.get().addOnSuccessListener(document -> {
            String storedName = document != null && document.exists() ? document.getString("name") : null;
            String storedEmail = document != null && document.exists() ? document.getString("email") : null;

            String name = clean(storedName);
            if (name == null) {
                name = clean(user.getDisplayName());
            }
            if (name == null) {
                name = nameFromEmail(authEmail);
            }

            Map<String, Object> patch = new HashMap<>();
            if (clean(storedName) == null) {
                patch.put("name", name);
            }
            if (clean(storedEmail) == null && !authEmail.isEmpty()) {
                patch.put("email", authEmail);
            }
            if (!patch.isEmpty()) {
                patch.put("userId", uid);
                ref.set(patch, SetOptions.merge());
            }

        });
    }

    /** ইমেইলের সামনের অংশ থেকে সুন্দর নাম: rahim.uddin99@ → Rahim Uddin99 */
    public static String nameFromEmail(String email) {
        String source = clean(email);
        if (source == null) {
            return FALLBACK_NAME;
        }
        int at = source.indexOf('@');
        String prefix = at > 0 ? source.substring(0, at) : source;
        prefix = prefix.replace('.', ' ').replace('_', ' ').replace('-', ' ').trim();
        if (prefix.isEmpty()) {
            return FALLBACK_NAME;
        }
        StringBuilder builder = new StringBuilder();
        for (String part : prefix.split("\\s+")) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.getDefault()));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() == 0 ? FALLBACK_NAME : builder.toString();
    }

    /** নাম → ইমেইল → "FindLink user" */
    public static String displayName(String name, String email) {
        String cleaned = clean(name);
        if (cleaned != null) {
            return cleaned;
        }
        String fromEmail = nameFromEmail(email);
        return fromEmail;
    }

    public static String initialsOf(String name) {
        String cleaned = clean(name);
        if (cleaned == null) {
            return "FL";
        }
        String[] parts = cleaned.split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && initials.length() < 2) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return initials.length() == 0 ? "FL" : initials.toString();
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return TextUtils.isEmpty(trimmed) ? null : trimmed;
    }
}
