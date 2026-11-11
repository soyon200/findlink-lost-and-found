package com.example.findlink;

import static org.junit.Assert.assertNull;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Test;
import org.junit.runner.RunWith;

/** ডিভাইসে সব সেশন বন্ধ করে নতুন অ্যাকাউন্ট দিয়ে শুরু করার জন্য। */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SignOutTest {

    @Test
    public void signOutEveryAccount() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signOut();
        Log.i("SIGNOUT", "currentUser after signOut = " + auth.getCurrentUser());
        assertNull(auth.getCurrentUser());
    }
}
