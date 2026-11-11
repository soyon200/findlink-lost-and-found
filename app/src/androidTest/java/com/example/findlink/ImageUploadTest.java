package com.example.findlink;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.widget.EditText;
import android.widget.ScrollView;
import android.view.inputmethod.InputMethodManager;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.findlink.util.ImgbbUploader;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * imgbb আপলোড সত্যিই কাজ করে কি না তা ডিভাইস থেকে যাচাই করে
 * (অ্যাপের ভেতরের একই কোড পাথ ব্যবহার করে, শুধু UI ছাড়া)।
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ImageUploadTest {

    @Test
    public void uploadsImageToImgbb() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // টেস্টের জন্য একটা ছবি তৈরি
        Bitmap bitmap = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.rgb(20, 47, 77));
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(36f);
        canvas.drawText("FindLink upload test", 60, 210, paint);

        File file = new File(context.getCacheDir(), "upload-test.png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> uploadedUrl = new AtomicReference<>();
        final AtomicReference<String> errorMessage = new AtomicReference<>();

        ImgbbUploader.upload(context, Uri.fromFile(file), new ImgbbUploader.Callback() {
            @Override
            public void onSuccess(String imageUrl) {
                uploadedUrl.set(imageUrl);
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                errorMessage.set(message);
                latch.countDown();
            }
        });

        assertTrue("আপলোড ৬০ সেকেন্ডে শেষ হয়নি", latch.await(60, TimeUnit.SECONDS));
        assertNull("আপলোড ব্যর্থ: " + errorMessage.get(), errorMessage.get());
        assertNotNull("কোনো URL পাওয়া যায়নি", uploadedUrl.get());
        assertTrue("অপ্রত্যাশিত URL: " + uploadedUrl.get(),
                uploadedUrl.get().startsWith("https://i.ibb.co/"));

        Log.i("ImageUploadTest", "Uploaded image URL: " + uploadedUrl.get());
    }

    /**
     * Add Post স্ক্রিনের "Photo" সেকশনটা দেখতে কেমন, তার স্ক্রিনশট নেয়।
     */
    @Test
    public void capturePhotoSection() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, MainActivity.class)
                .putExtra("startTab", 2)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(2000);
            scenario.onActivity(activity -> {
                ScrollView scrollView = activity.findViewById(R.id.addPostScroll);
                if (scrollView != null) {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
            SystemClock.sleep(1200);

            Bitmap screenshot = InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation().takeScreenshot();
            assertNotNull("স্ক্রিনশট নেওয়া যায়নি", screenshot);

            File dir = new File(context.getExternalFilesDir(null), "screenshots");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(new File(dir, "04b_add_post_photo.png"))) {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            screenshot.recycle();
        }
    }
    /**
     * ফিল্ডে ফোকাস দিয়ে কীবোর্ড খুলে স্ক্রিন কেমন দেখায় তা ক্যাপচার করে।
     */
    @Test
    public void captureKeyboardOpenState() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, MainActivity.class)
                .putExtra("startTab", 2)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(2000);
            scenario.onActivity(activity -> {
                EditText title = activity.findViewById(R.id.etPostTitle);
                title.requestFocus();
                InputMethodManager imm = (InputMethodManager)
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(title, InputMethodManager.SHOW_IMPLICIT);
                }
            });
            SystemClock.sleep(2500);
            saveScreenshot(context, "05_keyboard_open");

            scenario.onActivity(activity -> {
                EditText title = activity.findViewById(R.id.etPostTitle);
                title.setText("Black leather wallet");
            });
            SystemClock.sleep(1200);
            saveScreenshot(context, "06_keyboard_typing");
        }
    }

    private void saveScreenshot(Context context, String name) throws Exception {
        Bitmap screenshot = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().takeScreenshot();
        assertNotNull("স্ক্রিনশট নেওয়া যায়নি", screenshot);

        File dir = new File(context.getExternalFilesDir(null), "screenshots");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        screenshot.recycle();
    }
    /**
     * "Add a photo" ট্যাপ করলে Camera/Gallery পপ-আপ আসে কি না তা ক্যাপচার করে।
     */
    @Test
    public void capturePhotoSourceDialog() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, MainActivity.class)
                .putExtra("startTab", 2)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(2000);
            scenario.onActivity(activity -> activity.findViewById(R.id.cardPhoto).performClick());
            SystemClock.sleep(1500);
            saveScreenshot(context, "07_photo_dialog");
        }
    }
    /**
     * ক্যামেরার ছবির জন্য FileProvider ঠিকভাবে কনফিগার করা আছে কি না।
     */
    @Test
    public void fileProviderOffersCameraUri() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File dir = new File(context.getCacheDir(), "images");
        assertTrue("images ফোল্ডার তৈরি হয়নি", dir.exists() || dir.mkdirs());

        File photo = new File(dir, "probe.jpg");
        assertTrue("টেস্ট ফাইল তৈরি হয়নি", photo.exists() || photo.createNewFile());

        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", photo);
        assertNotNull("FileProvider URI পাওয়া যায়নি", uri);
        assertTrue("অপ্রত্যাশিত URI: " + uri, uri.toString().startsWith("content://"));
        photo.delete();
    }}

