package com.example.findlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.findlink.util.ChatRepository;
import com.example.findlink.util.SecurityCode;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * সিকিউরিটি কোড সিস্টেমের যাচাই:
 * ১. কোড ফরম্যাট ও হ্যাশিং
 * ২. নিজের LOST পোস্টের হ্যাশের সাথে সঠিক কোড মেলে, ভুল কোড মেলে না
 * ৩. VERIFIED / UNVERIFIED claim মেসেজ ঠিকভাবে সেভ হয়
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SecurityCodeTest {

    private static final String PASSWORD = "Test1234!";

    @Test
    public void securityCodeVerifiesOwnership() throws Exception {
        // ১. ফরম্যাট
        assertTrue(SecurityCode.isValid("4821"));
        assertTrue(SecurityCode.isValid("123456"));
        assertFalse(SecurityCode.isValid("123"));
        assertFalse(SecurityCode.isValid("abcdef"));
        assertFalse(SecurityCode.isValid("12 34"));

        // ২. হ্যাশ — একই itemId + কোডে একই হ্যাশ, ভিন্ন কোডে ভিন্ন
        String itemId = "item-abc";
        String hash = SecurityCode.hash(itemId, "4821");
        assertEquals(hash, SecurityCode.hash(itemId, "4821"));
        assertTrue(SecurityCode.matches(hash, itemId, "4821"));
        assertFalse(SecurityCode.matches(hash, itemId, "1111"));
        // ভিন্ন itemId হলে হ্যাশ আলাদা (salt কাজ করছে)
        assertFalse(SecurityCode.matches(hash, "item-xyz", "4821"));

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        auth.signOut();

        String stamp = String.valueOf(System.currentTimeMillis());
        String email = "fl.sec." + stamp + "@findlink.test";
        FirebaseUser user = await(auth.createUserWithEmailAndPassword(email, PASSWORD)).getUser();
        assertNotNull(user);
        await(db.collection("users").document(user.getUid()).set(userDoc("Owner Test", email)));

        // ৩. LOST পোস্ট + হ্যাশ সেভ
        String lostItemId = db.collection("items").document().getId();
        String secretCode = "4821";
        Map<String, Object> lostItem = new HashMap<>();
        lostItem.put("itemId", lostItemId);
        lostItem.put("userId", user.getUid());
        lostItem.put("title", "Blue backpack");
        lostItem.put("category", "Bags");
        lostItem.put("reportType", "LOST");
        lostItem.put("status", "ACTIVE");
        lostItem.put("securityHash", SecurityCode.hash(lostItemId, secretCode));
        await(db.collection("items").document(lostItemId).set(lostItem));

        DocumentSnapshot stored = await(db.collection("items").document(lostItemId).get());
        String storedHash = stored.getString("securityHash");
        assertNotNull(storedHash);
        assertFalse("Code must not be stored in plain text", storedHash.contains(secretCode));

        // ৪. ClaimActivity যেভাবে যাচাই করে: নিজের সব পোস্ট ঘুরে হ্যাশ মেলানো
        QuerySnapshot mine = await(db.collection("items")
                .whereEqualTo("userId", user.getUid()).get());
        assertTrue("Correct code should verify ownership",
                anyMatches(mine, secretCode));
        assertFalse("Wrong code must not verify ownership",
                anyMatches(mine, "1111"));

        // ৫. VERIFIED claim মেসেজ সেভ হয়
        String otherUid = "finder-" + stamp;
        ensureChatBlocking(lostItemId, user.getUid(), otherUid);
        String chatId = ChatRepository.buildChatId(lostItemId, user.getUid(), otherUid);
        sendBlocking(chatId, user.getUid(), "Owner Test",
                "Claim request: It has a blue keychain inside.", true);

        QuerySnapshot messages = await(db.collection("chats").document(chatId)
                .collection("messages").get());
        assertEquals(1, messages.size());
        DocumentSnapshot claim = messages.getDocuments().get(0);
        assertEquals("CLAIM", claim.getString("type"));
        assertEquals(Boolean.TRUE, claim.getBoolean("verified"));
    }

    private boolean anyMatches(QuerySnapshot snapshots, String code) {
        for (QueryDocumentSnapshot doc : snapshots) {
            String storedHash = doc.getString("securityHash");
            String lostItemId = doc.getString("itemId");
            if (storedHash != null && SecurityCode.matches(storedHash, lostItemId, code)) {
                return true;
            }
        }
        return false;
    }

    private void ensureChatBlocking(String itemId, String ownerId, String otherId) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] error = new String[1];
        ChatRepository.ensureChat(itemId, "Blue backpack", null, ownerId,
                ownerId, "Owner Test", otherId, "Finder Test", new ChatRepository.ChatCallback() {
                    @Override
                    public void onReady(String chatId) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        error[0] = message;
                        latch.countDown();
                    }
                });
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertNull(error[0]);
    }

    private void sendBlocking(String chatId, String senderId, String senderName,
                              String text, boolean verified) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] error = new String[1];
        ChatRepository.sendMessage(chatId, senderId, senderName, text,
                ChatRepository.TYPE_CLAIM, verified, new ChatRepository.SendCallback() {
                    @Override
                    public void onSent() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        error[0] = message;
                        latch.countDown();
                    }
                });
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertNull(error[0]);
    }

    private Map<String, Object> userDoc(String name, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        return user;
    }

    private static <T> T await(Task<T> task) throws Exception {
        return Tasks.await(task, 40, TimeUnit.SECONDS);
    }
}
