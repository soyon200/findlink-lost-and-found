package com.example.findlink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBackUnconditionally;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertFalse;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * ব্যাক বাটন নেভিগেশনের আচরণ যাচাই করার টেস্ট।
 *
 * ১. Home-এর রুট স্ক্রিনে একবার ব্যাক চাপলে অ্যাপ বন্ধ হবে না
 * ২. ফিড খোলা থাকলে ব্যাক চাপলে ক্যাটাগরি মেনুতে ফিরবে
 * ৩. অন্য ট্যাবে থাকলে ব্যাক চাপলে Home ট্যাবে ফিরবে
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BackNavigationTest {

    @Test
    public void backOnHomeRoot_doesNotExitApp() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.layoutCategoryMenu)).check(matches(isDisplayed()));

            pressBackUnconditionally();

            scenario.onActivity(activity -> assertFalse(
                    "Home-এ একবার ব্যাক চাপলে অ্যাপ বন্ধ হয়ে গেছে", activity.isFinishing()));
            onView(withId(R.id.layoutCategoryMenu)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void backFromFeed_returnsToCategoryMenu() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.cardRecent)).perform(click());
            onView(withId(R.id.layoutFeed)).check(matches(isDisplayed()));

            pressBackUnconditionally();

            onView(withId(R.id.layoutCategoryMenu)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> assertFalse(
                    "ফিড থেকে ব্যাক চাপলে অ্যাপ বন্ধ হয়ে গেছে", activity.isFinishing()));
        }
    }

    @Test
    public void backFromOtherTab_returnsToHome() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_profile)).perform(click());

            pressBackUnconditionally();

            onView(withId(R.id.layoutCategoryMenu)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> assertFalse(
                    "অন্য ট্যাব থেকে ব্যাক চাপলে অ্যাপ বন্ধ হয়ে গেছে", activity.isFinishing()));
        }
    }
}
