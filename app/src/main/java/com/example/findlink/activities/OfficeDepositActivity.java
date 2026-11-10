package com.example.findlink.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.findlink.R;
import com.example.findlink.models.Office;
import com.example.findlink.util.DepositCode;
import com.example.findlink.util.OfficeDirectory;
import com.example.findlink.util.WindowInsetsUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * মালিককে খুঁজে না পেলে পাওয়া জিনিস Lost & Found ডেস্কে জমা দেওয়ার স্ক্রিন।
 * জমা দিলে আইটেমটি AT_OFFICE হয়ে যায় এবং একটি ডিপোজিট কোড (রিসিপ্ট) তৈরি হয়।
 */
public class OfficeDepositActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "itemId";
    public static final String EXTRA_ITEM_TITLE = "itemTitle";

    private final List<MaterialCardView> officeCards = new ArrayList<>();
    private final List<Office> offices = new ArrayList<>();
    private int selectedIndex = 0;

    private TextView tvOfficeItemTitle;
    private Button btnConfirmDeposit;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String itemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_office_deposit);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        WindowInsetsUtil.applyTopPadding(findViewById(R.id.officeDepositTopBar));

        tvOfficeItemTitle = findViewById(R.id.tvOfficeItemTitle);
        btnConfirmDeposit = findViewById(R.id.btnConfirmDeposit);
        Button btnOfficeBack = findViewById(R.id.btnOfficeBack);
        btnOfficeBack.setOnClickListener(v -> finish());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        String itemTitle = getIntent().getStringExtra(EXTRA_ITEM_TITLE);
        if (TextUtils.isEmpty(itemId)) {
            Toast.makeText(this, "This report cannot be deposited", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tvOfficeItemTitle.setText(TextUtils.isEmpty(itemTitle) ? "Item report" : itemTitle);

        buildOfficeList();
        selectOffice(0);

        btnConfirmDeposit.setOnClickListener(v -> deposit());
    }

    private void buildOfficeList() {
        View container = findViewById(R.id.layoutOfficeList);
        LayoutInflater inflater = LayoutInflater.from(this);
        offices.addAll(OfficeDirectory.all());

        for (int i = 0; i < offices.size(); i++) {
            Office office = offices.get(i);
            View row = inflater.inflate(R.layout.item_office_choice,
                    (android.view.ViewGroup) container, false);

            ((TextView) row.findViewById(R.id.tvOfficeName)).setText(office.getName());
            ((TextView) row.findViewById(R.id.tvOfficeAddress)).setText(office.getAddress());
            ((TextView) row.findViewById(R.id.tvOfficeHours)).setText(office.getHours());

            final int index = i;
            row.setOnClickListener(v -> selectOffice(index));

            ((android.view.ViewGroup) container).addView(row);
            officeCards.add((MaterialCardView) row);
        }
    }

    private void selectOffice(int index) {
        if (index < 0 || index >= officeCards.size()) {
            return;
        }
        selectedIndex = index;
        int accent = getColor(R.color.colorAccent);
        int divider = getColor(R.color.colorDivider);
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < officeCards.size(); i++) {
            MaterialCardView card = officeCards.get(i);
            boolean selected = i == index;
            card.setStrokeWidth(selected ? Math.round(2 * density) : Math.round(1 * density));
            card.setStrokeColor(selected ? accent : divider);
            ImageView check = card.findViewById(R.id.ivOfficeCheck);
            check.setVisibility(selected ? View.VISIBLE : View.GONE);
        }
    }

    private void deposit() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedIndex < 0 || selectedIndex >= offices.size()) {
            Toast.makeText(this, "Choose a desk first", Toast.LENGTH_SHORT).show();
            return;
        }

        Office office = offices.get(selectedIndex);
        String depositCode = DepositCode.generate();
        String finderId = mAuth.getCurrentUser().getUid();

        btnConfirmDeposit.setEnabled(false);
        btnConfirmDeposit.setText("Depositing…");

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "AT_OFFICE");
        updates.put("officeId", office.getId());
        updates.put("officeName", office.getName());
        updates.put("officeAddress", office.getAddress());
        updates.put("officeHours", office.getHours());
        updates.put("depositCode", depositCode);
        updates.put("depositedBy", finderId);
        updates.put("depositedAt", System.currentTimeMillis());

        db.collection("items").document(itemId).update(updates)
                .addOnSuccessListener(aVoid -> showReceipt(office, depositCode))
                .addOnFailureListener(e -> {
                    btnConfirmDeposit.setEnabled(true);
                    btnConfirmDeposit.setText("Deposit at this desk");
                    Toast.makeText(OfficeDepositActivity.this,
                            "Could not deposit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showReceipt(Office office, String depositCode) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Deposited successfully")
                .setMessage("Item: " + tvOfficeItemTitle.getText()
                        + "\n\nDesk: " + office.getName()
                        + "\nAddress: " + office.getAddress()
                        + "\nHours: " + office.getHours()
                        + "\n\nDeposit code: " + depositCode
                        + "\n\nKeep this code as your receipt. The office will search the owner's "
                        + "report and hand the item over only after the owner proves ownership "
                        + "with their security code and a photo ID.")
                .setPositiveButton("Done", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

}
