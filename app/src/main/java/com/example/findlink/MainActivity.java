package com.example.findlink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.findlink.fragments.AddPostFragment;
import com.example.findlink.fragments.HomeFragment;
import com.example.findlink.fragments.ProfileFragment;
import com.example.findlink.fragments.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_TAB = "state_selected_tab";
    private static final String TAB_TAG_PREFIX = "tab_";
    private static final long EXIT_INTERVAL_MS = 2000L;

    private BottomNavigationView bottomNavigationView;

    // বর্তমানে কোন ট্যাব সিলেক্ট করা আছে (রিলঞ্চ/রোটেশনে ধরে রাখার জন্য)
    private int selectedTabId = R.id.nav_home;

    private long lastBackPressTime = 0L;
    private Toast exitToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // লগআউট অবস্থায় থাকলে সরাসরি লগইন স্ক্রিনে পাঠানো হয়
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, com.example.findlink.activities.LoginActivity.class));
            finish();
            return;
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        boolean isRestored = savedInstanceState != null;
        if (isRestored) {
            selectedTabId = savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.nav_home);
        } else {
            selectedTabId = resolveStartTab();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });

        if (isRestored) {
            // FragmentManager আগের ফ্র্যাগমেন্টগুলো নিজেই রিস্টোর করেছে,
            // শুধু নিচের বারের সিলেকশন ঠিক রাখতে হবে
            bottomNavigationView.getMenu().findItem(selectedTabId).setChecked(true);
        } else {
            // প্রথমবার খুললে Home ট্যাব
            bottomNavigationView.setSelectedItemId(selectedTabId);
        }

        // হার্ডওয়্যার / জেসচার ব্যাক বাটন
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // ১) Home-এর ফিড খোলা থাকলে আগে ক্যাটাগরি মেনুতে ফিরবে
                if (goBackInsideHome()) {
                    return;
                }
                // ২) অন্য কোনো ট্যাবে থাকলে আগে Home ট্যাবে ফিরবে
                if (selectedTabId != R.id.nav_home) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    return;
                }
                // ৩) Home-এর রুট স্ক্রিনে আছি -> দুইবার চাপলে অ্যাপ মিনিমাইজ হবে
                confirmExit();
            }
        });
    }

    /**
     * ডিপ-লিংক/টেস্ট থেকে সরাসরি কোনো ট্যাবে খোলার সুবিধা (ডিফল্ট: Home)।
     */
    private int resolveStartTab() {
        switch (getIntent().getIntExtra("startTab", 0)) {
            case 1:
                return R.id.nav_search;
            case 2:
                return R.id.nav_add;
            case 3:
                return R.id.nav_profile;
            default:
                return R.id.nav_home;
        }
    }

    /**
     * অন্য ফ্র্যাগমেন্ট (যেমন Home ড্যাশবোর্ডের শর্টকাট) থেকে ট্যাব বদলানোর জন্য।
     */
    public void selectTab(int itemId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(itemId);
        }
    }

    private void showTab(int itemId) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        String tag = TAB_TAG_PREFIX + itemId;
        Fragment target = fm.findFragmentByTag(tag);

        FragmentTransaction tx = fm.beginTransaction();
        if (target == null) {
            target = createFragment(itemId);
            if (target == null) {
                return;
            }
            tx.add(R.id.fragmentContainer, target, tag);
        } else if (target.isHidden()) {
            tx.show(target);
        }

        // অন্য ট্যাবের ফ্র্যাগমেন্টগুলো সরিয়ে না ফেলে শুধু হাইড করা হচ্ছে,
        // যাতে ট্যাব বদলালে স্ক্রল/লিস্ট স্টেট হারিয়ে না যায়
        for (Fragment fragment : fm.getFragments()) {
            if (fragment != target && !fragment.isHidden()) {
                tx.hide(fragment);
            }
        }

        tx.commit();
        selectedTabId = itemId;
    }

    private Fragment createFragment(int itemId) {
        if (itemId == R.id.nav_home) {
            return new HomeFragment();
        } else if (itemId == R.id.nav_search) {
            return new SearchFragment();
        } else if (itemId == R.id.nav_add) {
            return new AddPostFragment();
        } else if (itemId == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return null;
    }

    private boolean goBackInsideHome() {
        Fragment current = getSupportFragmentManager()
                .findFragmentByTag(TAB_TAG_PREFIX + R.id.nav_home);
        return current instanceof HomeFragment && ((HomeFragment) current).handleBackPress();
    }

    private void confirmExit() {
        long now = System.currentTimeMillis();
        if (now - lastBackPressTime < EXIT_INTERVAL_MS) {
            if (exitToast != null) {
                exitToast.cancel();
            }
            lastBackPressTime = 0L;
            // finish() না করে ব্যাকগ্রাউন্ডে পাঠানো হচ্ছে, ফলে আবার অ্যাপ খুললে
            // ঠিক যেখানে ছিলাম সেখান থেকেই শুরু হবে (স্প্ল্যাশ/হোম থেকে নয়)
            moveTaskToBack(true);
        } else {
            lastBackPressTime = now;
            if (exitToast != null) {
                exitToast.cancel();
            }
            exitToast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT);
            exitToast.show();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, selectedTabId);
    }
}
