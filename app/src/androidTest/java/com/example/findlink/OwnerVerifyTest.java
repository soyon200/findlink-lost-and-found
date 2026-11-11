package com.example.findlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.findlink.util.ChatRepository;
import com.example.findlink.util.SecurityCode;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * চ্যাটে কোড দিয়ে মালিক যাচাই আসল Firebase-এ কাজ করে কি না।
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OwnerVerifyTest {

    private static final String PASSWORD = "Test1234!";

    @Test
    public void inChatVerificationAcceptsOnlyTheOwnersCode() throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        auth.signOut();

        String stamp = String.valueOf(System.currentTimeMillis());
        FirebaseUser owner = Tasks.await(auth.createUserWithEmailAndPassword(
                "fl.owner." + stamp + "@findlink.test", PASSWORD)).getUser();
        FirebaseUser finder = Tasks.await(auth.createUserWithEmailAndPassword(
                "fl.finder." + stamp + "@findlink.test", PASSWORD)).getUser();
        assertTrue(owner != null && finder != null);

        String itemId = db.collection("items").document().getId();
        Map<String, Object> item = new HashMap<>();
        item.put("itemId", itemId);
        item.put("userId", owner.getUid());
        item.put("title", "Grey backpack");
        item.put("category", "Bags");
        item.put("location", "Cafeteria");
        item.put("description", "Grey backpack with a laptop sleeve.");
        item.put("reportType", "LOST");
        item.put("status", "ACTIVE");
        item.put("timestamp", System.currentTimeMillis());
        item.put("securityHash", SecurityCode.hash(itemId, "731945"));
        Tasks.await(db.collection("items").document(itemId).set(item));

        // সঠিক কোড
        assertTrue("correct code must verify", awaitVerify(owner.getUid(), "731945"));
        // ভুল কোড
        assertFalse("wrong code must not verify", awaitVerify(owner.getUid(), "111111"));
        // অন্য কারও কোডে মিলবে না
        assertFalse(awaitVerify(finder.getUid(), "731945"));

        // যাচাই চিহ্ন চ্যাটে সেভ হয়
        String chatId = ChatRepository.buildChatId(itemId, owner.getUid(), finder.getUid());
        ChatRepository.markOwnerVerified(chatId, owner.getUid(), "Owner Test");
        Thread.sleep(2500);

        DocumentReference chatRef = db.collection("chats").document(chatId);
        DocumentSnapshot chat = Tasks.await(chatRef.get());
        // চ্যাট ডকুমেন্ট না থাকলে markOwnerVerified কিছু করে না — এখানে আগে তৈরি করি
        if (!chat.exists()) {
            Map<String, Object> chatDoc = new HashMap<>();
            chatDoc.put("chatId", chatId);
            chatDoc.put("itemId", itemId);
            chatDoc.put("participants", java.util.Arrays.asList(owner.getUid(), finder.getUid()));
            Tasks.await(chatRef.set(chatDoc));
            ChatRepository.markOwnerVerified(chatId, owner.getUid(), "Owner Test");
            Thread.sleep(2500);
            chat = Tasks.await(chatRef.get());
        }
        assertEquals(owner.getUid(), chat.getString("verifiedOwnerId"));
    }

    private boolean awaitVerify(String ownerUid, String code) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);
        ChatRepository.verifyOwnerCode(ownerUid, code, verified -> {
            result.set(verified);
            latch.countDown();
        });
        assertTrue(latch.await(20, TimeUnit.SECONDS));
        return result.get();
    }
}
