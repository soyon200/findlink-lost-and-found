package com.example.findlink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.findlink.MainActivity;
import com.example.findlink.util.UserProfileSync;
import com.example.findlink.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    // স্প্ল্যাশ স্ক্রিনের সময় (আগে ৩ সেকেন্ড ছিল)
    private static final long SPLASH_DELAY_MS = 1200L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable navigateRunnable = this::goToNextScreen;
    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // অ্যাপ আগে থেকেই চালু থাকা অবস্থায় আবার লঞ্চার থেকে ওপেন হলে
        // স্প্ল্যাশ দেখিয়ে নতুন করে শুরু না করে সরাসরি আগের স্ক্রিনেই ফিরে যাবে
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }

        handler.postDelayed(navigateRunnable, SPLASH_DELAY_MS);
    }

    private void goToNextScreen() {
        if (navigated || isFinishing() || isDestroyed()) {
            return;
        }
        navigated = true;

        // ফায়ারবেস থেকে চেক করা হচ্ছে ইউজার আগে থেকেই লগইন করা আছে কিনা
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // লগইন থাকা অবস্থায় নাম/ইমেইল প্রোফাইল ঠিক আছে কি না নিশ্চিত করা
            UserProfileSync.ensureProfile();
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(navigateRunnable);
        super.onDestroy();
    }
}
