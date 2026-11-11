package com.example.findlink;


import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.findlink.activities.ClaimActivity;
import com.example.findlink.activities.ContactActivity;
import com.example.findlink.activities.InboxActivity;
import com.example.findlink.activities.ItemDetailsActivity;
import com.example.findlink.activities.LoginActivity;
import com.example.findlink.activities.RegisterActivity;
import com.example.findlink.activities.SplashActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * প্রতিটি স্ক্রিনের স্ক্রিনশট নিয়ে ডিভাইসে ডিজাইন যাচাই করার টেস্ট।
 * ছবিগুলো অ্যাপের external files/screenshots ফোল্ডারে সেভ হয়।
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ScreenshotTourTest {

    private static final long SETTLE_MS = 900L;

    @Test
    public void captureAllScreens() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // ১. স্প্ল্যাশ
        try (ActivityScenario<SplashActivity> splash = ActivityScenario.launch(SplashActivity.class)) {
            sleep(500);
            capture("01_splash");
            splash.close();
        }

        // ২. হোম + অন্যান্য ট্যাব
        try (ActivityScenario<MainActivity> main = ActivityScenario.launch(MainActivity.class)) {
            sleep(1400);
            capture("02_home_menu");

            // রিসেন্ট ফিড
            main.onActivity(activity -> {
                activity.findViewById(R.id.cardRecent).performClick();
            });
            sleep(SETTLE_MS);
            capture("03_home_feed");

            // সার্চ ট্যাব
            selectTab(main, R.id.nav_search);
            sleep(SETTLE_MS);
            capture("04_search_empty");

            // টাইপ করে সাজেশন
            main.onActivity(activity -> ((android.widget.EditText) activity.findViewById(R.id.etSearchQuery)).setText("a"));
            sleep(1200);
            capture("05_search_suggestions");

            // সাবমিট করে রেজাল্ট
            main.onActivity(activity -> activity.findViewById(R.id.btnSearch).performClick());
            sleep(1200);
            capture("06_search_results");

            // পোস্ট ট্যাব
            selectTab(main, R.id.nav_add);
            sleep(SETTLE_MS);
            capture("07_add_post");

            // প্রোফাইল ট্যাব
            selectTab(main, R.id.nav_profile);
            sleep(1500);
            capture("08_profile");

            main.close();
        }

        // ৩. লগইন
        try (ActivityScenario<LoginActivity> login = ActivityScenario.launch(LoginActivity.class)) {
            sleep(SETTLE_MS);
            capture("09_login");
            login.close();
        }

        // ৪. রেজিস্টার
        try (ActivityScenario<RegisterActivity> register = ActivityScenario.launch(RegisterActivity.class)) {
            sleep(SETTLE_MS);
            capture("10_register");
            register.close();
        }

        // ৫. আইটেম ডিটেইলস
        Intent details = new Intent(context, ItemDetailsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("title", "Black leather wallet")
                .putExtra("category", "Accessories")
                .putExtra("location", "Central Library, 2nd floor")
                .putExtra("description", "Black leather wallet with a blue stripe on the back. Contains a student ID and a library card.")
                .putExtra("reportType", "LOST")
                .putExtra("itemId", "demo-item")
                .putExtra("userId", "demo-user")
                .putExtra("phone", "+8801712345678");
        try (ActivityScenario<ItemDetailsActivity> scenario = ActivityScenario.launch(details)) {
            sleep(SETTLE_MS);
            capture("11_item_details");
            scenario.close();
        }

        // ৬. ক্লেইম
        Intent claim = new Intent(context, ClaimActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("itemId", "demo-item");
        try (ActivityScenario<ClaimActivity> scenario = ActivityScenario.launch(claim)) {
            sleep(SETTLE_MS);
            capture("12_claim");
            scenario.close();
        }

        // ৭. কন্টাক্ট
        Intent contact = new Intent(context, ContactActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("itemId", "demo-item");
        try (ActivityScenario<ContactActivity> scenario = ActivityScenario.launch(contact)) {
            sleep(SETTLE_MS);
            capture("13_contact");
            scenario.close();
        }

        // ৮. ইনবক্স
        try (ActivityScenario<InboxActivity> scenario = ActivityScenario.launch(InboxActivity.class)) {
            sleep(1800);
            capture("14_inbox");
            scenario.close();
        }
    }

    private void selectTab(ActivityScenario<MainActivity> scenario, int menuId) {
        scenario.onActivity(activity -> {
            BottomNavigationView nav = activity.findViewById(R.id.bottomNavigationView);
            nav.setSelectedItemId(menuId);
        });
    }

    private void capture(String name) throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> { });
        sleep(250);

        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        if (bitmap == null) {
            return;
        }

        File dir = new File(
                InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),
                "screenshots");
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }

        File file = new File(dir, name + ".png");
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } finally {
            bitmap.recycle();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void captureRemainingScreens() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        try (ActivityScenario<MainActivity> main = ActivityScenario.launch(MainActivity.class)) {
            sleep(1200);
            selectTab(main, R.id.nav_add);
            sleep(SETTLE_MS);
            capture("07_add_post");

            selectTab(main, R.id.nav_profile);
            sleep(1500);
            capture("08_profile");
            main.close();
        }

        try (ActivityScenario<LoginActivity> login = ActivityScenario.launch(LoginActivity.class)) {
            sleep(SETTLE_MS);
            capture("09_login");
            login.close();
        }

        try (ActivityScenario<RegisterActivity> register = ActivityScenario.launch(RegisterActivity.class)) {
            sleep(SETTLE_MS);
            capture("10_register");
            register.close();
        }

        Intent details = new Intent(context, ItemDetailsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("title", "Black leather wallet")
                .putExtra("category", "Accessories")
                .putExtra("location", "Central Library, 2nd floor")
                .putExtra("description", "Black leather wallet with a blue stripe on the back. Contains a student ID and a library card.")
                .putExtra("reportType", "LOST")
                .putExtra("itemId", "demo-item")
                .putExtra("userId", "demo-user")
                .putExtra("phone", "+8801712345678");
        try (ActivityScenario<ItemDetailsActivity> scenario = ActivityScenario.launch(details)) {
            sleep(SETTLE_MS);
            capture("11_item_details");
            scenario.close();
        }

        Intent claim = new Intent(context, ClaimActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("itemId", "demo-item");
        try (ActivityScenario<ClaimActivity> scenario = ActivityScenario.launch(claim)) {
            sleep(SETTLE_MS);
            capture("12_claim");
            scenario.close();
        }

        Intent contact = new Intent(context, ContactActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("itemId", "demo-item");
        try (ActivityScenario<ContactActivity> scenario = ActivityScenario.launch(contact)) {
            sleep(SETTLE_MS);
            capture("13_contact");
            scenario.close();
        }

        try (ActivityScenario<InboxActivity> scenario = ActivityScenario.launch(InboxActivity.class)) {
            sleep(1800);
            capture("14_inbox");
            scenario.close();
        }
    }}

