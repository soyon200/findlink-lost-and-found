package com.example.findlink.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.findlink.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReportLostActivity extends AppCompatActivity {

    // ভেরিয়েবল তৈরি করা
    private EditText etLostItemName, etLostCategory, etLostDescription, etLostLocation;
    private Button btnSubmitLostReport;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_lost);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // ফায়ারবেস ইনিশিয়ালাইজ করা
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // XML এর আইডিগুলোর সাথে জাভাকে কানেক্ট করা
        etLostItemName = findViewById(R.id.etLostItemName);
        etLostCategory = findViewById(R.id.etLostCategory);
        etLostDescription = findViewById(R.id.etLostDescription);
        etLostLocation = findViewById(R.id.etLostLocation);
        btnSubmitLostReport = findViewById(R.id.btnSubmitLostReport);

        // সাবমিট বাটনে ক্লিক করলে কী হবে তার লজিক
        btnSubmitLostReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveLostItemData();
            }
        });
    }

    private void saveLostItemData() {
        // ফর্ম থেকে ইউজারের লেখাগুলো সংগ্রহ করা
        String name = etLostItemName.getText().toString().trim();
        String category = etLostCategory.getText().toString().trim();
        String description = etLostDescription.getText().toString().trim();
        String location = etLostLocation.getText().toString().trim();

        // কোনো বক্স খালি আছে কিনা চেক করা
        if (name.isEmpty() || category.isEmpty() || description.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // বাটনটি সাময়িকভাবে বন্ধ করে দেওয়া যাতে ডাবল ক্লিক না পড়ে
        btnSubmitLostReport.setText("Submitting...");
        btnSubmitLostReport.setEnabled(false);

        // লগইন করা ইউজারের আইডি বের করা
        String userId = mAuth.getCurrentUser().getUid();

        // পোস্টের জন্য ফায়ারবেস থেকে একটি নতুন ইউনিক আইডি (Unique ID) তৈরি করা
        String itemId = db.collection("items").document().getId();

        // ডাটাবেজে পাঠানোর জন্য তথ্যগুলোকে একসাথে প্যাক (Map) করা
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("itemId", itemId);
        itemData.put("userId", userId);
        itemData.put("title", name);
        itemData.put("category", category);
        itemData.put("description", description);
        itemData.put("location", location);
        itemData.put("reportType", "LOST"); // এটি লস্ট আইটেম তাই LOST লেখা হলো
        itemData.put("status", "ACTIVE");
        itemData.put("timestamp", System.currentTimeMillis()); // বর্তমান সময়

        // ফায়ারবেসের "items" নামের নতুন কালেকশনে ডেটা সেভ করা
        db.collection("items").document(itemId).set(itemData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ReportLostActivity.this, "Post Submitted Successfully!", Toast.LENGTH_SHORT).show();
                            finish(); // কাজ শেষ, তাই এই পেজটি বন্ধ করে আগের পেজে (Home) ফিরে যাবে
                        } else {
                            Toast.makeText(ReportLostActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            btnSubmitLostReport.setText("Submit Report");
                            btnSubmitLostReport.setEnabled(true);
                        }
                    }
                });
    }
}