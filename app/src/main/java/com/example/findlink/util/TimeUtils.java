package com.example.findlink.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * চ্যাটের টাইমস্ট্যাম্প পড়ার উপযোগী টেক্সটে বদলায়।
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    public static String formatChatTime(long millis) {
        if (millis <= 0) {
            return "";
        }
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(millis);

        if (isSameDay(now, then)) {
            return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(millis));
        }
        if (isYesterday(now, then)) {
            return "Yesterday";
        }
        if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
            return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(millis));
        }
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(millis));
    }

    /**
     * ইনবক্স লিস্টের ডান দিকে দেখানোর জন্য ছোট ফরম্যাট।
     */
    public static String formatInboxTime(long millis) {
        if (millis <= 0) {
            return "";
        }
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(millis);

        if (isSameDay(now, then)) {
            return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(millis));
        }
        if (isYesterday(now, then)) {
            return "Yesterday";
        }
        return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(millis));
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean isYesterday(Calendar now, Calendar then) {
        Calendar yesterday = (Calendar) now.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        return isSameDay(yesterday, then);
    }
}
