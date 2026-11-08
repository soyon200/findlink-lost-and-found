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

public class ReportFoundActivity extends AppCompatActivity {

    private EditText etFoundItemName, etFoundCategory, etFoundDescription, etFoundLocation;
    private Button btnSubmitFoundReport;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_found);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFoundItemName = findViewById(R.id.etFoundItemName);
        etFoundCategory = findViewById(R.id.etFoundCategory);
        etFoundDescription = findViewById(R.id.etFoundDescription);
        etFoundLocation = findViewById(R.id.etFoundLocation);
        btnSubmitFoundReport = findViewById(R.id.btnSubmitFoundReport);

        btnSubmitFoundReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFoundItemData();
            }
        });
    }

    private void saveFoundItemData() {
        String name = etFoundItemName.getText().toString().trim();
        String category = etFoundCategory.getText().toString().trim();
        String description = etFoundDescription.getText().toString().trim();
        String location = etFoundLocation.getText().toString().trim();

        if (name.isEmpty() || category.isEmpty() || description.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitFoundReport.setText("Submitting...");
        btnSubmitFoundReport.setEnabled(false);

        String userId = mAuth.getCurrentUser().getUid();
        String itemId = db.collection("items").document().getId();

        Map<String, Object> itemData = new HashMap<>();
        itemData.put("itemId", itemId);
        itemData.put("userId", userId);
        itemData.put("title", name);
        itemData.put("category", category);
        itemData.put("description", description);
        itemData.put("location", location);
        itemData.put("reportType", "FOUND"); // এখানে FOUND লেখা হলো
        itemData.put("status", "ACTIVE");
        itemData.put("timestamp", System.currentTimeMillis());

        db.collection("items").document(itemId).set(itemData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ReportFoundActivity.this, "Found Post Submitted!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(ReportFoundActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            btnSubmitFoundReport.setText("Submit Found Report");
                            btnSubmitFoundReport.setEnabled(true);
                        }
                    }
                });
    }
}