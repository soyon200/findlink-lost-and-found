package com.example.findlink.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.findlink.R;
import com.example.findlink.util.WindowInsetsUtil;

public class ContactActivity extends AppCompatActivity {

    private EditText etContactMessage;
    private Button btnSendMessage, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        etContactMessage = findViewById(R.id.etContactMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnBack = findViewById(R.id.btnBack);

        WindowInsetsUtil.applyTopPadding(findViewById(R.id.contactTopBar));

        // স্ক্রিনের ব্যাক বাটন
        btnBack.setOnClickListener(v -> finish());

        String itemId = getIntent().getStringExtra("itemId");

        btnSendMessage.setOnClickListener(v -> {
            String message = etContactMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(ContactActivity.this, "Please write a message", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ContactActivity.this, "Message Sent!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
