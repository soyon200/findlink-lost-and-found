package com.example.findlink.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.findlink.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.findlink.util.ClaimCode;
import com.example.findlink.util.SecurityCode;
import com.example.findlink.util.WindowInsetsUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ItemDetailsActivity extends AppCompatActivity {

    private TextView tvDetailsTitle, tvDetailsCategory, tvDetailsLocation, tvDetailsDescription, tvDetailsReportType, tvDetailsPhone;
    private TextView tvOfficeInfoName, tvOfficeInfoAddress, tvOfficeInfoHours;
    private Button btnAction, btnBack, btnOfficeAction;
    private View rowPhone, cardDetailsImage, cardOfficeInfo, cardClaimCode;
    private TextView tvClaimCode;
    private ImageView ivDetailsImage;
    private String itemIdValue, titleValue, reportTypeValue, postUserIdValue, currentUserIdValue;

    @Override
    protected void onResume() {
        super.onResume();
        // অফিসে জমা দেওয়ার পর ফিরে এলে কার্ড/বাটন সাথে সাথে হালনাগাদ হয়
        if (itemIdValue != null && !itemIdValue.isEmpty()) {
            loadOfficeState(itemIdValue, titleValue, reportTypeValue,
                    postUserIdValue, currentUserIdValue);
        }
    }

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvDetailsTitle = findViewById(R.id.tvDetailsTitle);
        tvDetailsCategory = findViewById(R.id.tvDetailsCategory);
        tvDetailsLocation = findViewById(R.id.tvDetailsLocation);
        tvDetailsDescription = findViewById(R.id.tvDetailsDescription);
        tvDetailsReportType = findViewById(R.id.tvDetailsReportType);
        tvDetailsPhone = findViewById(R.id.tvDetailsPhone);
        rowPhone = findViewById(R.id.rowPhone);
        cardDetailsImage = findViewById(R.id.cardDetailsImage);
        ivDetailsImage = findViewById(R.id.ivDetailsImage);
        btnAction = findViewById(R.id.btnAction);
        btnBack = findViewById(R.id.btnBack);
        btnOfficeAction = findViewById(R.id.btnOfficeAction);
        cardOfficeInfo = findViewById(R.id.cardOfficeInfo);
        tvOfficeInfoName = findViewById(R.id.tvOfficeInfoName);
        tvOfficeInfoAddress = findViewById(R.id.tvOfficeInfoAddress);
        tvOfficeInfoHours = findViewById(R.id.tvOfficeInfoHours);
        cardClaimCode = findViewById(R.id.cardClaimCode);
        tvClaimCode = findViewById(R.id.tvClaimCode);

        WindowInsetsUtil.applyTopPadding(findViewById(R.id.detailsTopBar));

        // স্ক্রিনের ব্যাক বাটনে ক্লিক করলে পেজ ক্লোজ হবে
        btnBack.setOnClickListener(v -> finish());

        String title = getIntent().getStringExtra("title");
        String category = getIntent().getStringExtra("category");
        String location = getIntent().getStringExtra("location");
        String description = getIntent().getStringExtra("description");
        String reportType = getIntent().getStringExtra("reportType");
        String itemId = getIntent().getStringExtra("itemId");
        String postUserId = getIntent().getStringExtra("userId");
        String phone = getIntent().getStringExtra("phone");
        String imageUrl = getIntent().getStringExtra("imageUrl");

        tvDetailsTitle.setText(title);
        tvDetailsCategory.setText(category == null || category.trim().isEmpty() ? "—" : category);
        tvDetailsLocation.setText(location == null || location.trim().isEmpty() ? "—" : location);
        tvDetailsDescription.setText(description == null || description.trim().isEmpty()
                ? "No description was added for this report."
                : description);
        tvDetailsReportType.setText(reportType);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            cardDetailsImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.color.colorPrimaryLight)
                    .into(ivDetailsImage);
        } else {
            cardDetailsImage.setVisibility(View.GONE);
        }

        if (phone != null && !phone.isEmpty()) {
            rowPhone.setVisibility(View.VISIBLE);
            tvDetailsPhone.setText(phone);
            rowPhone.setOnClickListener(v -> {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + phone));
                startActivity(callIntent);
            });
        } else {
            rowPhone.setVisibility(View.GONE);
        }

        if ("LOST".equals(reportType)) {
            styleChip(tvDetailsReportType, R.color.colorWarningSoft, R.color.colorWarning);
            btnAction.setText("I Found This Item");
            btnAction.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorAccent)));
        } else {
            styleChip(tvDetailsReportType, R.color.colorSuccessSoft, R.color.colorSuccess);
            btnAction.setText("Claim This Item");
            btnAction.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorSuccess)));
        }

        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        itemIdValue = itemId;
        titleValue = title;
        reportTypeValue = reportType;
        postUserIdValue = postUserId;
        currentUserIdValue = currentUserId;
        if (currentUserId.equals(postUserId)) {
            btnAction.setText("Mark as Resolved");
            btnAction.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPrimary)));
        }

        btnAction.setOnClickListener(v -> {
            if (currentUserId.equals(postUserId)) {
                db.collection("items").document(itemId).update("status", "RESOLVED")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ItemDetailsActivity.this, "Post marked as Resolved!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(ItemDetailsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else if (postUserId == null || postUserId.trim().isEmpty()) {
                Toast.makeText(ItemDetailsActivity.this,
                        "This report has no owner to contact", Toast.LENGTH_SHORT).show();
            } else if ("FOUND".equals(reportType)) {
                // দাবি করার সময় প্রমাণ লিখে সরাসরি চ্যাটে পাঠানো হবে
                Intent intent = new Intent(ItemDetailsActivity.this, ClaimActivity.class);
                intent.putExtra(ClaimActivity.EXTRA_ITEM_ID, itemId);
                intent.putExtra(ClaimActivity.EXTRA_ITEM_TITLE, title);
                intent.putExtra(ClaimActivity.EXTRA_ITEM_IMAGE, imageUrl);
                intent.putExtra(ClaimActivity.EXTRA_POST_OWNER_ID, postUserId);
                startActivity(intent);
            } else {
                // "I Found This Item" → সরাসরি কথোপকথন শুরু
                Intent intent = new Intent(ItemDetailsActivity.this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_ITEM_ID, itemId);
                intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, postUserId);
                intent.putExtra(ChatActivity.EXTRA_ITEM_TITLE, title);
                intent.putExtra(ChatActivity.EXTRA_ITEM_IMAGE, imageUrl);
                intent.putExtra(ChatActivity.EXTRA_POST_OWNER_ID, postUserId);
                intent.putExtra(ChatActivity.EXTRA_PREFILL, "Hi, I found this item. ");
                startActivity(intent);
            }
        });

        loadOfficeState(itemId, title, reportType, postUserId, currentUserId);
    }

    /**
     * আইটেমটি কোনো Lost & Found ডেস্কে জমা আছে কি না দেখে UI সাজায়।
     * - জমা থাকলে: অফিসের তথ্য + "Collect from office"
     * - FOUND রিপোর্টের মালিক হলে: "Deposit at lost & found office" বাটন
     */
    private void loadOfficeState(String itemId, String title, String reportType,
                                 String postUserId, String currentUserId) {
        if (itemId == null || itemId.isEmpty()) {
            return;
        }
        db.collection("items").document(itemId).get()
                .addOnSuccessListener(document -> {
                    if (document == null || !document.exists()) {
                        return;
                    }
                    String status = document.getString("status");

                    if ("AT_OFFICE".equals(status)) {
                        String officeName = document.getString("officeName");
                        String officeAddress = document.getString("officeAddress");
                        String officeHours = document.getString("officeHours");

                        cardOfficeInfo.setVisibility(View.VISIBLE);
                        tvOfficeInfoName.setText(officeName == null ? "Lost & found office" : officeName);
                        tvOfficeInfoAddress.setText(officeAddress == null ? "" : officeAddress);
                        tvOfficeInfoHours.setText(officeHours == null ? "" : officeHours);

                        btnOfficeAction.setVisibility(View.GONE);
                        btnAction.setText("Collect from office");
                        btnAction.setBackgroundTintList(
                                ColorStateList.valueOf(getColor(R.color.colorSuccess)));
                        btnAction.setOnClickListener(v -> showOfficePickupDialog(
                                officeName, officeAddress, officeHours));
                        return;
                    }

                    boolean isOwner = currentUserId != null && currentUserId.equals(postUserId);

                    // কোড শুধু LOST পোস্টের মালিকের জন্য — FOUND-এ শুধু মেসেজ/প্রমাণ
                    if (isOwner && "LOST".equals(reportType) && !"RESOLVED".equals(status)) {
                        final boolean isLostPost = true;
                        String code = isLostPost
                                ? document.getString("securityCode")
                                : document.getString("claimCode");

                        if (code == null || code.trim().isEmpty()) {
                            code = ClaimCode.generate();
                            saveOwnerCode(itemId, code, isLostPost);
                        }

                        cardClaimCode.setVisibility(View.VISIBLE);
                        ((TextView) findViewById(R.id.tvClaimCodeLabel))
                                .setText(isLostPost ? "YOUR ITEM CODE" : "HANDOVER CODE");
                        tvClaimCode.setText(code);

                        final boolean lost = isLostPost;
                        findViewById(R.id.btnRegenerateCode).setOnClickListener(v -> {
                            String fresh = ClaimCode.generate();
                            saveOwnerCode(itemId, fresh, lost);
                            tvClaimCode.setText(fresh);
                            Toast.makeText(ItemDetailsActivity.this,
                                    "New code saved", Toast.LENGTH_SHORT).show();
                        });
                    }

                    if ("FOUND".equals(reportType) && isOwner && !"RESOLVED".equals(status)) {
                        btnOfficeAction.setVisibility(View.VISIBLE);
                        btnOfficeAction.setText("Deposit at lost & found office");
                        btnOfficeAction.setOnClickListener(v -> {
                            Intent intent = new Intent(ItemDetailsActivity.this,
                                    OfficeDepositActivity.class);
                            intent.putExtra(OfficeDepositActivity.EXTRA_ITEM_ID, itemId);
                            intent.putExtra(OfficeDepositActivity.EXTRA_ITEM_TITLE, title);
                            startActivity(intent);
                        });
                        return;
                    }

                });
    }

    /**
     * অ্যাপ-বানানো কোড পোস্টে সেভ করে — LOST হলে হ্যাশও (যাচাইয়ের জন্য), FOUND হলে সরাসরি।
     */
    private void saveOwnerCode(String itemId, String code, boolean isLostPost) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        if (isLostPost) {
            updates.put("securityCode", code);
            updates.put("securityHash", SecurityCode.hash(itemId, code));
        } else {
            updates.put("claimCode", code);
        }
        db.collection("items").document(itemId).update(updates);
    }

    /**
     * মালিক তাঁর LOST পোস্টে সিকিউরিটি কোড যোগ/বদল করতে পারবেন — কোড শুধু হ্যাশ আকারে সেভ হয়।
     */
    private void showSecurityCodeDialog(String itemId) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        input.setHint("4–6 digit code");

        FrameLayout container = new FrameLayout(this);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add a security code")
                .setMessage("Only you should know this code. Whoever finds your item must ask for it "
                        + "before handing the item over.")
                .setView(container)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    if (!SecurityCode.isValid(code)) {
                        Toast.makeText(this, "Use 4–6 digits", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collection("items").document(itemId)
                            .update("securityHash", SecurityCode.hash(itemId, code))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Security code saved",
                                        Toast.LENGTH_SHORT).show();
                                btnOfficeAction.setVisibility(View.GONE);
                            })
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Could not save: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .show();
    }

    private void showOfficePickupDialog(String officeName, String officeAddress, String officeHours) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Collect from the office")
                .setMessage((officeName == null ? "Lost & found office" : officeName)
                        + "\n" + (officeAddress == null ? "" : officeAddress)
                        + "\n" + (officeHours == null ? "" : officeHours)
                        + "\n\nBring your security code and a photo ID. The office verifies the "
                        + "code before handing the item over.")
                .setPositiveButton("Got it", null)
                .show();
    }

    /**
     * রিপোর্ট টাইপ চিপকে রাউন্ডেড ব্যাকগ্রাউন্ড দেয় (রঙ অনুযায়ী)।
     */
    private void styleChip(TextView view, int backgroundColorRes, int textColorRes) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(8 * getResources().getDisplayMetrics().density);
        background.setColor(getColor(backgroundColorRes));
        view.setBackground(background);
        view.setTextColor(getColor(textColorRes));
    }
}

