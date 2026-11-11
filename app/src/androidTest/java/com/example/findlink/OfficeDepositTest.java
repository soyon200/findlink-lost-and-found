package com.example.findlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.findlink.models.Office;
import com.example.findlink.util.DepositCode;
import com.example.findlink.util.OfficeDirectory;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * অফিসে জমা দেওয়ার ডেটা যাচাই: আইটেম AT_OFFICE হয়, অফিসের তথ্য ও
 * ডিপোজিট কোড সেভ হয়, কোডের ফরম্যাট ঠিক থাকে।
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OfficeDepositTest {

    private static final String PASSWORD = "Test1234!";

    @Test
    public void depositMarksItemAtOffice() throws Exception {
        // ১. কোড ফরম্যাট ও ইউনিকনেস
        String sample = DepositCode.generate();
        assertTrue("Bad code format: " + sample, DepositCode.isValid(sample));
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            codes.add(DepositCode.generate());
        }
        assertTrue("Deposit codes repeat too often", codes.size() > 190);

        // ২. অফিস ডিরেক্টরি
        assertTrue("No offices configured", OfficeDirectory.all().size() >= 3);
        Office office = OfficeDirectory.all().get(0);
        assertNotNull(OfficeDirectory.findById(office.getId()));

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        auth.signOut();

        String stamp = String.valueOf(System.currentTimeMillis());
        String email = "fl.office." + stamp + "@findlink.test";
        FirebaseUser finder = await(auth.createUserWithEmailAndPassword(email, PASSWORD)).getUser();
        assertNotNull(finder);

        String itemId = db.collection("items").document().getId();
        Map<String, Object> item = new HashMap<>();
        item.put("itemId", itemId);
        item.put("userId", finder.getUid());
        item.put("title", "Blue water bottle");
        item.put("category", "Accessories");
        item.put("reportType", "FOUND");
        item.put("status", "ACTIVE");
        item.put("timestamp", System.currentTimeMillis());
        await(db.collection("items").document(itemId).set(item));

        // ৩. স্ক্রিন যেভাবে আপডেট করে
        String depositCode = DepositCode.generate();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "AT_OFFICE");
        updates.put("officeId", office.getId());
        updates.put("officeName", office.getName());
        updates.put("officeAddress", office.getAddress());
        updates.put("officeHours", office.getHours());
        updates.put("depositCode", depositCode);
        updates.put("depositedBy", finder.getUid());
        updates.put("depositedAt", System.currentTimeMillis());
        await(db.collection("items").document(itemId).update(updates));

        DocumentSnapshot stored = await(db.collection("items").document(itemId).get());
        assertEquals("AT_OFFICE", stored.getString("status"));
        assertEquals(office.getName(), stored.getString("officeName"));
        assertEquals(office.getAddress(), stored.getString("officeAddress"));
        assertEquals(depositCode, stored.getString("depositCode"));
        assertTrue(DepositCode.isValid(stored.getString("depositCode")));
        assertEquals(finder.getUid(), stored.getString("depositedBy"));
        Long depositedAt = stored.getLong("depositedAt");
        assertNotNull(depositedAt);
        assertTrue(depositedAt > 0);
    }

    private static <T> T await(Task<T> task) throws Exception {
        return Tasks.await(task, 40, TimeUnit.SECONDS);
    }
}
