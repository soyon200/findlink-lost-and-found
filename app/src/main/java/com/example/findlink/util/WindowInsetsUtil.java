package com.example.findlink.util;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Android 15+ এ edge-to-edge বাধ্যতামূলক, তাই হেডার/কনটেন্ট স্ট্যাটাস বারের নিচে
 * ঢুকে যেত। এই ইউটিলিটি স্ট্যাটাস বার/নেভিগেশন বারের ইনসেট যোগ করে দেয়।
 */
public final class WindowInsetsUtil {

    private WindowInsetsUtil() {
    }

    /**
     * ভিউয়ের উপরের প্যাডিংয়ে স্ট্যাটাস বারের উচ্চতা যোগ করে (ব্যাকগ্রাউন্ড অপরিবর্তিত থাকে)।
     */
    public static void applyTopPadding(View view) {
        if (view == null) {
            return;
        }

        final int initialTop = view.getPaddingTop();
        final int initialBottom = view.getPaddingBottom();
        final int initialStart = view.getPaddingStart();
        final int initialEnd = view.getPaddingEnd();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPaddingRelative(initialStart, initialTop + bars.top, initialEnd, initialBottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /**
     * স্ক্রলযোগ্য স্ক্রিনের জন্য: উপরে স্ট্যাটাস বার, নিচে নেভিগেশন বার + কীবোর্ড (IME)।
     * কীবোর্ড খোলার সময় স্ক্রিন ফাঁকা হয়ে যাওয়া আটকায়, কারণ padding-ই কনটেন্ট সরে
     * দেয় — আলাদা করে window resize/fitsSystemWindows লাগে না।
     */
    public static void applySystemBarsAndImePadding(View view) {
        if (view == null) {
            return;
        }

        final int initialTop = view.getPaddingTop();
        final int initialBottom = view.getPaddingBottom();
        final int initialStart = view.getPaddingStart();
        final int initialEnd = view.getPaddingEnd();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(bars.bottom, ime.bottom);
            v.setPaddingRelative(initialStart, initialTop + bars.top, initialEnd, initialBottom + bottomInset);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /**
     * স্ক্রল/লিস্টের নিচের প্যাডিংয়ে নেভিগেশন বারের উচ্চতা যোগ করে।
     */
    public static void applyBottomPadding(View view) {
        if (view == null) {
            return;
        }

        final int initialTop = view.getPaddingTop();
        final int initialBottom = view.getPaddingBottom();
        final int initialStart = view.getPaddingStart();
        final int initialEnd = view.getPaddingEnd();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPaddingRelative(initialStart, initialTop, initialEnd, initialBottom + bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
