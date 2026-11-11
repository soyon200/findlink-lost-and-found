package com.example.findlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.findlink.util.ChatRepository;
import com.example.findlink.util.ClaimCode;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * LOST পোস্ট ছাড়া দাবিদারের ক্ষেত্রে অ্যাপ-বানানো হ্যান্ডওভার কোড কাজ করে কি না।
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ClaimCodeTest {

    private static final String PASSWORD = "Test1234!";

    @Test
    public void handoverCodeVerifiesClaim() throws Exception {
        assertTrue(ClaimCode.isValid("482913"));
        assertFalse(ClaimCode.isValid("48291"));
        assertFalse(ClaimCode.isValid("abcdef"));

        assertTrue(ClaimCode.matches("482913", "482913"));
        assertFalse(ClaimCode.matches("482913", "111111"));
        assertFalse(ClaimCode.matches(null, "482913"));
        assertFalse(ClaimCode.matches("482913", null));

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        auth.signOut();

        String stamp = String.valueOf(System.currentTimeMillis());
        FirebaseUser finder = Tasks.await(auth.createUserWithEmailAndPassword(
                "fl.fnd." + stamp + "@findlink.test", PASSWORD)).getUser();
        FirebaseUser owner = Tasks.await(auth.createUserWithEmailAndPassword(
                "fl.own." + stamp + "@findlink.test", PASSWORD)).getUser();

        String itemId = db.collection("items").document().getId();
        String code = ClaimCode.generate();

        Map<String, Object> item = new HashMap<>();
        item.put("itemId", itemId);
        item.put("userId", finder.getUid());
        item.put("title", "Blue water bottle");
        item.put("reportType", "FOUND");
        item.put("status", "ACTIVE");
        item.put("claimCode", code);
        item.put("timestamp", System.currentTimeMillis());
        Tasks.await(db.collection("items").document(itemId).set(item));

        DocumentSnapshot stored = Tasks.await(db.collection("items").document(itemId).get());
        assertEquals(code, stored.getString("claimCode"));
        assertTrue(ClaimCode.matches(stored.getString("claimCode"), code));

        // দাবিদার (LOST পোস্ট নেই) হ্যান্ডওভার কোড দিয়ে verified claim পাঠাচ্ছেন
        String chatId = ChatRepository.buildChatId(itemId, finder.getUid(), owner.getUid());
        CountDownLatch latch = new CountDownLatch(1);
        ChatRepository.loadUserName(owner.getUid(), name ->
                ChatRepository.ensureChat(itemId, "Blue water bottle", null, finder.getUid(),
                        owner.getUid(), name, finder.getUid(), null, new ChatRepository.ChatCallback() {
                            @Override
                            public void onReady(String readyChatId) {
                                ChatRepository.sendMessage(readyChatId, owner.getUid(), name,
                                        "Claim request: It has a blue cap and a sticker.",
                                        ChatRepository.TYPE_CLAIM, true,
                                        new ChatRepository.SendCallback() {
                                            @Override
                                            public void onSent() {
                                                latch.countDown();
                                            }

                                            @Override
                                            public void onError(String message) {
                                                latch.countDown();
                                            }
                                        });
                            }

                            @Override
                            public void onError(String message) {
                                latch.countDown();
                            }
                        }));
        assertTrue(latch.await(30, TimeUnit.SECONDS));

        QuerySnapshot messages = Tasks.await(db.collection("chats").document(chatId)
                .collection("messages").get());
        assertEquals(1, messages.size());
        assertTrue(messages.getDocuments().get(0).getBoolean("verified"));
        assertEquals("CLAIM", messages.getDocuments().get(0).getString("type"));

        DocumentSnapshot chat = Tasks.await(db.collection("chats").document(chatId).get());
        assertEquals(owner.getUid(), chat.getString("verifiedOwnerId"));
    }
}
