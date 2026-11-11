package com.example.findlink;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AuthStateTest {

    @Test
    public void logCurrentAuthState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Log.i("AUTHSTATE", "user = " + (user == null ? "null" : user.getUid() + " / " + user.getEmail()));
    }
}
