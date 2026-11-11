package com.example.findlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Add Post স্ক্রিনে সিকিউরিটি কোড সেকশন ঠিকভাবে দেখাচ্ছে কি না — ছবি সহ।
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddPostSecurityUiTest {

    private static final String TAG = "AddPostSecurityUiTest";

    @Test
    public void securitySectionIsVisibleOnLost() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("startTab", 2);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            sleep(2000);

            // নিশ্চিতভাবে Post ট্যাবে যাওয়া (intent extra কার্যকর না হলেও)
            scenario.onActivity(activity -> {
                com.google.android.material.bottomnavigation.BottomNavigationView nav =
                        activity.findViewById(R.id.bottomNavigationView);
                nav.setSelectedItemId(R.id.nav_add);
            });
            sleep(2000);

            scenario.onActivity(activity -> {
                View section = activity.findViewById(R.id.layoutSecurityCode);
                assertNotNull("Security section missing", section);
                assertEquals("Security section should show for Lost posts",
                        View.VISIBLE, section.getVisibility());

                ScrollView scroll = activity.findViewById(R.id.addPostScroll);
                scroll.post(() -> scroll.smoothScrollTo(0, section.getTop()));
            });
            sleep(2000);

            scenario.onActivity(activity -> {
                View decor = activity.getWindow().getDecorView();
                Bitmap bitmap = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(),
                        Bitmap.Config.ARGB_8888);
                decor.draw(new Canvas(bitmap));

                File dir = new File(activity.getExternalFilesDir(null), "screenshots");
                if (!dir.exists() && !dir.mkdirs()) {
                    bitmap.recycle();
                    return;
                }
                File file = new File(dir, "18_add_post_security.png");
                try (OutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (Exception e) {
                    Log.w(TAG, "Could not save screenshot", e);
                } finally {
                    bitmap.recycle();
                }
            });
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
