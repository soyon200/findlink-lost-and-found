package com.example.findlink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.findlink.MainActivity;
import com.example.findlink.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegName, etRegEmail, etRegPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;

    // ফায়ারবেস ভেরিয়েবল
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // ফায়ারবেস ইনিশিয়ালাইজ করা
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // XML এর আইডিগুলো খুঁজে বের করা
        etRegName = findViewById(R.id.etRegName);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // লগইন পেজে যাওয়ার বাটন
        tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // রেজিস্টার বাটনে ক্লিক করার লজিক
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etRegName.getText().toString().trim();
                String email = etRegEmail.getText().toString().trim();
                String password = etRegPassword.getText().toString().trim();

                // ইনপুট ঠিক আছে কিনা চেক করা
                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ফায়ারবেসে অ্যাকাউন্ট তৈরি করা
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // অ্যাকাউন্ট তৈরি সফল হলে ইউজারের আইডি বের করা
                                    String userId = mAuth.getCurrentUser().getUid();

                                    // ডাটাবেজে রাখার জন্য ইউজারের তথ্য ম্যাপ (Map) করা
                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("userId", userId);
                                    userMap.put("name", name);
                                    userMap.put("email", email);
                                     userMap.put("role", "USER"); // ডিফল্টভাবে সবাই সাধারণ ইউজার
                                     userMap.put("createdAt", System.currentTimeMillis());

                                    // Firestore ডাটাবেজে "users" কালেকশনে তথ্য সেভ করা
                                    db.collection("users").document(userId).set(userMap)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                                        // সফল হলে মেইন স্ক্রিনে (Home) নিয়ে যাওয়া
                                                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                                        finish();
                                                    }
                                                }
                                            });
                                } else {
                                    Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }
}