package com.example.findlink.util;

import com.example.findlink.BuildConfig;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * imgbb (api.imgbb.com) এ ছবি আপলোড করে পাবলিক URL ফেরত দেয়।
 * Firebase Storage ছাড়াই ছবি রাখার সবচেয়ে সহজ পথ — Firestore-এ শুধু URL সেভ হয়।
 */
public final class ImgbbUploader {

    // imgbb API v1 key (ফ্রি অ্যাকাউন্ট) — local.properties থেকে আসে, কোডে রাখা হয় না
    private static final String API_KEY = BuildConfig.IMGBB_API_KEY;
    private static final String ENDPOINT = "https://api.imgbb.com/1/upload";

    // ছবি ছোট রাখা হচ্ছে: সর্বোচ্চ ১০৮০px, JPEG ৭০% (সাধারণত ১০০-২০০KB)
    private static final int MAX_DIMENSION = 1080;
    private static final int JPEG_QUALITY = 70;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(String imageUrl);

        void onError(String message);
    }

    private ImgbbUploader() {
    }

    public static void upload(Context context, Uri imageUri, Callback callback) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                String url = uploadBlocking(appContext, imageUri);
                MAIN_HANDLER.post(() -> callback.onSuccess(url));
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Upload failed" : e.getMessage();
                MAIN_HANDLER.post(() -> callback.onError(message));
            }
        });
    }

    private static String uploadBlocking(Context context, Uri imageUri) throws Exception {
        Bitmap bitmap = decodeScaled(context, imageUri);
        if (bitmap == null) {
            throw new IOException("Could not read the selected image");
        }

        ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, jpegStream);
        bitmap.recycle();

        String base64Image = Base64.encodeToString(jpegStream.toByteArray(), Base64.NO_WRAP);

        String boundary = "----FindLinkBoundary" + System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(40000);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8))) {
            writeField(writer, boundary, "key", API_KEY);
            writeField(writer, boundary, "image", base64Image);
            writer.append("--").append(boundary).append("--").append("\r\n");
            writer.flush();
        }

        int statusCode = connection.getResponseCode();
        InputStream responseStream = statusCode >= 200 && statusCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String response = readAll(responseStream);
        connection.disconnect();

        JSONObject json = new JSONObject(response);
        if (!json.optBoolean("success")) {
            throw new IOException("imgbb rejected the upload (HTTP " + statusCode + ")");
        }
        return json.getJSONObject("data").getString("url");
    }

    private static void writeField(PrintWriter writer, String boundary, String name, String value) {
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n");
        writer.append(value).append("\r\n");
    }

    private static String readAll(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    /**
     * বড় ছবি হলে মেমোরি বাঁচাতে inSampleSize দিয়ে ছোট করে ডিকোড করে,
     * তারপর সর্বোচ্চ ১০৮০px-এ স্কেল করে।
     */
    private static Bitmap decodeScaled(Context context, Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(stream, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight);

        Bitmap decoded;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            decoded = BitmapFactory.decodeStream(stream, null, options);
        }
        if (decoded == null) {
            return null;
        }

        int width = decoded.getWidth();
        int height = decoded.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= MAX_DIMENSION) {
            return decoded;
        }

        float ratio = (float) MAX_DIMENSION / longestSide;
        Bitmap scaled = Bitmap.createScaledBitmap(decoded,
                Math.round(width * ratio), Math.round(height * ratio), true);
        if (scaled != decoded) {
            decoded.recycle();
        }
        return scaled;
    }

    private static int calculateSampleSize(int width, int height) {
        int sampleSize = 1;
        while (Math.max(width, height) / sampleSize > MAX_DIMENSION * 2) {
            sampleSize *= 2;
        }
        return sampleSize;
    }
}
