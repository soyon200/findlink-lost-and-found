package com.example.findlink.fragments;

import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.findlink.R;
import com.example.findlink.models.ItemModel;
import com.example.findlink.util.ClaimCode;
import com.example.findlink.util.ImgbbUploader;
import com.example.findlink.util.SecurityCode;
import com.example.findlink.util.WindowInsetsUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;

public class AddPostFragment extends Fragment {

    private EditText etPostTitle, etPostCategory, etPostLocation, etPostDescription, etPostPhone, etSecurityCode;
    private RadioGroup rgReportType;
    private Button btnSubmitPost, btnRemovePhoto;
    private View layoutPhotoPrompt, layoutPhotoPreview, layoutSecurityCode;
    private ImageView ivPhotoPreview;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ইউজার যে ছবি বেছে নিয়েছে (না থাকলে null)
    private Uri selectedImageUri;

    // ক্যামেরা দিয়ে তোলা ছবি যেখানে সেভ হবে
    private Uri pendingCameraUri;

    // গ্যালারি/ফটো পিকার থেকে ছবি বেছে নেওয়া
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    showPhotoPreview(uri);
                }
            });

    // ক্যামেরা দিয়ে ছবি তোলা
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && pendingCameraUri != null) {
                    selectedImageUri = pendingCameraUri;
                    showPhotoPreview(pendingCameraUri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_post, container, false);

        // স্ট্যাটাস বার + কীবোর্ড (IME) ইনসেট প্যাডিং হিসেবে — কীবোর্ড খুললে স্ক্রিন ফাঁকা হয় না
        WindowInsetsUtil.applyTopPadding(view);

        etPostTitle = view.findViewById(R.id.etPostTitle);
        etPostCategory = view.findViewById(R.id.etPostCategory);
        etPostLocation = view.findViewById(R.id.etPostLocation);
        etPostDescription = view.findViewById(R.id.etPostDescription);
        etPostPhone = view.findViewById(R.id.etPostPhone);
        rgReportType = view.findViewById(R.id.rgReportType);
        btnSubmitPost = view.findViewById(R.id.btnSubmitPost);
        etSecurityCode = view.findViewById(R.id.etSecurityCode);
        layoutSecurityCode = view.findViewById(R.id.layoutSecurityCode);

        // সিকিউরিটি কোড শুধু Lost রিপোর্টে লাগে
        rgReportType.setOnCheckedChangeListener((group, checkedId) -> updateSecurityVisibility());
        updateSecurityVisibility();

        layoutPhotoPrompt = view.findViewById(R.id.layoutPhotoPrompt);
        layoutPhotoPreview = view.findViewById(R.id.layoutPhotoPreview);
        ivPhotoPreview = view.findViewById(R.id.ivPhotoPreview);
        btnRemovePhoto = view.findViewById(R.id.btnRemovePhoto);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        view.findViewById(R.id.cardPhoto).setOnClickListener(v -> showPhotoSourceDialog());
        btnRemovePhoto.setOnClickListener(v -> clearPhoto());

        btnSubmitPost.setOnClickListener(v -> submitPost());

        return view;
    }

    /**
     * Lost বেছে নিলে সিকিউরিটি কোডের ঘর দেখায়, Found-এ লুকায়।
     */
    private void updateSecurityVisibility() {
        // কোড অ্যাপ নিজেই বানায় — তাই হাতে লেখার ঘরটি আর দরকার নেই
        layoutSecurityCode.setVisibility(View.GONE);
    }

    /**
     * ছবি যোগ করার আগে পপ-আপ: Camera না Gallery।
     */
    private void showPhotoSourceDialog() {
        if (!isAdded()) {
            return;
        }
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_photo_source, null, false);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add a photo")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .create();

        dialogView.findViewById(R.id.rowCamera).setOnClickListener(v -> {
            dialog.dismiss();
            openCamera();
        });
        dialogView.findViewById(R.id.rowGallery).setOnClickListener(v -> {
            dialog.dismiss();
            pickImageLauncher.launch("image/*");
        });

        dialog.show();
    }

    private void openCamera() {
        try {
            File imageDir = new File(requireContext().getCacheDir(), "images");
            if (!imageDir.exists() && !imageDir.mkdirs()) {
                throw new IOException("Could not create image folder");
            }
            File photoFile = File.createTempFile("capture_", ".jpg", imageDir);

            pendingCameraUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);

            takePictureLauncher.launch(pendingCameraUri);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No camera app found on this device", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPhotoPreview(Uri uri) {
        ivPhotoPreview.setImageURI(uri);
        layoutPhotoPrompt.setVisibility(View.GONE);
        layoutPhotoPreview.setVisibility(View.VISIBLE);
    }

    private void clearPhoto() {
        selectedImageUri = null;
        ivPhotoPreview.setImageDrawable(null);
        layoutPhotoPreview.setVisibility(View.GONE);
        layoutPhotoPrompt.setVisibility(View.VISIBLE);
    }

    private void resetSubmitButton() {
        btnSubmitPost.setEnabled(true);
        btnSubmitPost.setText("Post report");
    }

    private void submitPost() {
        String title = etPostTitle.getText().toString().trim();
        String category = etPostCategory.getText().toString().trim();
        String location = etPostLocation.getText().toString().trim();
        String description = etPostDescription.getText().toString().trim();
        String phone = etPostPhone.getText().toString().trim();

        int selectedId = rgReportType.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(getContext(), "Please select Lost or Found", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton rbSelected = getView().findViewById(selectedId);
        String reportType = rbSelected.getText().toString().toUpperCase();

        if (title.isEmpty() || category.isEmpty() || location.isEmpty() || description.isEmpty() || phone.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields including phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // কোড আর ইউজার লেখেন না — LOST/FOUND দুটোতেই অ্যাপ নিজেই বানায়
        final String securityCode = "";

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitPost.setEnabled(false);

        if (selectedImageUri == null) {
            savePost(title, category, location, description, reportType, phone, null, securityCode);
            return;
        }

        // ছবি থাকলে আগে imgbb-তে আপলোড, তারপর পোস্ট সেভ
        btnSubmitPost.setText("Uploading photo…");
        ImgbbUploader.upload(requireContext(), selectedImageUri, new ImgbbUploader.Callback() {
            @Override
            public void onSuccess(String imageUrl) {
                savePost(title, category, location, description, reportType, phone, imageUrl, securityCode);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getContext(), "Photo upload failed: " + message, Toast.LENGTH_LONG).show();
                resetSubmitButton();
            }
        });
    }

    /**
     * FOUND পোস্ট করার পরেই অ্যাপ-বানানো হ্যান্ডওভার কোডটি দেখানো হয় — এটি যাঁকে
     * সত্যিই জিনিসটি দেবেন শুধু তাঁকেই দিন; তিনিই claim-এ বসিয়ে যাচাই করবেন।
     */
    private void showHandoverCodeDialog(String code, boolean isLostReport) {
        if (getContext() == null) {
            return;
        }
        String title = isLostReport ? "Your item code" : "Your handover code";
        String body = isLostReport
                ? code + "\n\nThis code belongs to your lost report — only you and the app know it. "
                + "When someone finds your item, they will ask for this code. Give it only after "
                + "they show you the item."
                : code + "\n\nGive this code only to the person you hand the item over to. "
                + "They enter it in the app to prove they are the real owner.";
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton("Got it", null)
                .show();
    }

    private void savePost(String title, String category, String location, String description,
                          String reportType, String phone, @Nullable String imageUrl,
                          String securityCode) {
        btnSubmitPost.setText("Posting…");

        String userId = mAuth.getCurrentUser().getUid();
        String itemId = db.collection("items").document().getId();
        long timestamp = System.currentTimeMillis();

        ItemModel item = new ItemModel(itemId, userId, title, category, location, description,
                reportType, "ACTIVE", phone, timestamp, imageUrl);

        // কোড শুধু LOST রিপোর্টে — FOUND-এ শুধু মেসেজ/প্রমাণই যথেষ্ট
        final boolean isLostReport = "LOST".equals(reportType);
        final String generatedCode;
        if (isLostReport) {
            generatedCode = ClaimCode.generate();
            item.setSecurityCode(generatedCode);
            item.setSecurityHash(SecurityCode.hash(itemId, generatedCode));
        } else {
            generatedCode = null;
        }

        db.collection("items").document(itemId).set(item)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(getContext(), "Post added successfully!", Toast.LENGTH_SHORT).show();
                    if (generatedCode != null) {
                        showHandoverCodeDialog(generatedCode, true);
                    }
                    etPostTitle.setText("");
                    etPostCategory.setText("");
                    etPostLocation.setText("");
                    etPostDescription.setText("");
                    etPostPhone.setText("");
                    etSecurityCode.setText("");
                    clearPhoto();
                    resetSubmitButton();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetSubmitButton();
                });
    }
}


