package com.example.findlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.findlink.util.ClaimVerifier;
import com.example.findlink.util.SecurityCode;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** দাবি যাচাইয়ের তিনটি পথ: হ্যান্ডওভার কোড, নিজের LOST পোস্টের কোড, কোনোটাই না। */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ClaimVerifierTest {

    private static final String PASSWORD = "Test1234!";

    @Test
    public void verifiesByHandoverAndByLostReport() throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        auth.signOut();

        String stamp = String.valueOf(System.currentTimeMillis());
        FirebaseUser finder = Tasks.await(auth.createUserWithEmailAndPassword(
                "fl.vf." + stamp + "@findlink.test", PASSWORD)).getUser();
        FirebaseUser claimer = Tasks.await(auth.createUserWithEmailAndPassword(
                "fl.vc." + stamp + "@findlink.test", PASSWORD)).getUser();

        // পাওয়া জিনিস — অ্যাপ-বানানো হ্যান্ডওভার কোড
        String itemId = db.collection("items").document().getId();
        Map<String, Object> found = new HashMap<>();
        found.put("itemId", itemId);
        found.put("userId", finder.getUid());
        found.put("title", "Brown leather wallet");
        found.put("reportType", "FOUND");
        found.put("status", "ACTIVE");
        found.put("claimCode", "482913");
        found.put("timestamp", System.currentTimeMillis());
        Tasks.await(db.collection("items").document(itemId).set(found));

        Result ok = verify(itemId, claimer.getUid(), "482913", null);
        assertTrue(ok.verified);
        assertEquals(ClaimVerifier.MATCHED_HANDOVER, ok.matchedBy);

        Result wrong = verify(itemId, claimer.getUid(), "111111", null);
        assertTrue(!wrong.verified);
        assertEquals(ClaimVerifier.NO_MATCH, wrong.matchedBy);

        // দাবিদারের নিজের LOST পোস্ট + সিকিউরিটি কোড
        String lostId = db.collection("items").document().getId();
        Map<String, Object> lost = new HashMap<>();
        lost.put("itemId", lostId);
        lost.put("userId", claimer.getUid());
        lost.put("title", "Wallet");
        lost.put("reportType", "LOST");
        lost.put("status", "ACTIVE");
        lost.put("securityHash", SecurityCode.hash(lostId, "7319"));
        lost.put("timestamp", System.currentTimeMillis());
        Tasks.await(db.collection("items").document(lostId).set(lost));

        Result byLostReport = verify(itemId, claimer.getUid(), null, "7319");
        assertTrue(byLostReport.verified);
        assertEquals(ClaimVerifier.MATCHED_SECURITY, byLostReport.matchedBy);

        Result badLost = verify(itemId, claimer.getUid(), null, "0000");
        assertTrue(!badLost.verified);
    }

    private Result verify(String itemId, String uid, String handover, String security)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean verified = new AtomicBoolean(false);
        AtomicReference<String> matchedBy = new AtomicReference<>("");
        ClaimVerifier.verify(itemId, uid, handover, security, (isVerified, by) -> {
            verified.set(isVerified);
            matchedBy.set(by);
            latch.countDown();
        });
        latch.await(20, TimeUnit.SECONDS);
        return new Result(verified.get(), matchedBy.get());
    }

    private static class Result {
        final boolean verified;
        final String matchedBy;

        Result(boolean verified, String matchedBy) {
            this.verified = verified;
            this.matchedBy = matchedBy;
        }
    }
}
