package com.example.findlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.findlink.util.ChatRepository;
import com.google.android.gms.tasks.Task;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * চ্যাটের নির্ভরযোগ্যতা: চ্যাট আবার খুললেও শেষ মেসেজ মুছে যায় না,
 * অপঠিত গণনা ঠিক আসে, পড়লে শূন্য হয়, আর verified claim চ্যাটে চিহ্নিত থাকে।
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ChatImprovementTest {

    private static final String PASSWORD = "Test1234!";

    @Test
    public void chatSummaryAndReadStateStayCorrect() throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        auth.signOut();

        String stamp = String.valueOf(System.currentTimeMillis());
        FirebaseUser a = await(auth.createUserWithEmailAndPassword(
                "fl.imp.a." + stamp + "@findlink.test", PASSWORD)).getUser();
        FirebaseUser b = await(auth.createUserWithEmailAndPassword(
                "fl.imp.b." + stamp + "@findlink.test", PASSWORD)).getUser();
        assertNotNull(a);
        assertNotNull(b);

        String itemId = db.collection("items").document().getId();
        Map<String, Object> item = new HashMap<>();
        item.put("itemId", itemId);
        item.put("userId", a.getUid());
        item.put("title", "Black wallet");
        item.put("category", "Accessories");
        item.put("reportType", "LOST");
        item.put("status", "ACTIVE");
        item.put("timestamp", System.currentTimeMillis());
        await(db.collection("items").document(itemId).set(item));

        String chatId = ChatRepository.buildChatId(itemId, a.getUid(), b.getUid());

        // ---- B প্রথমে চ্যাট খোলে ও মেসেজ দেয়
        ensureChatBlocking(itemId, a.getUid(), b.getUid(), a.getUid(), "Rahim");
        sendBlocking(chatId, b.getUid(), "Rahim", "I found your wallet.", false,
                ChatRepository.TYPE_CHAT);

        DocumentSnapshot afterSend = await(chatDoc(db, chatId));
        assertEquals("I found your wallet.", afterSend.getString("lastMessage"));

        // ---- A পরে চ্যাট খোলে: শেষ মেসেজ মুছে যাওয়া যাবে না
        ensureChatBlocking(itemId, a.getUid(), a.getUid(), b.getUid(), "Ayesha");
        DocumentSnapshot afterReopen = await(chatDoc(db, chatId));
        assertEquals("Chat summary was wiped when the chat was reopened",
                "I found your wallet.", afterReopen.getString("lastMessage"));

        // ---- A-এর জন্য অপঠিত, পড়ার পরে শূন্য
        ChatRepository.markRead(chatId, a.getUid());
        waitForRead(db, chatId, a.getUid());
        assertEquals(0, unreadCount(a.getUid()));

        // ---- verified claim চ্যাটে চিহ্নিত হয়
        sendBlocking(chatId, a.getUid(), "Ayesha", "Claim request: it has a blue stripe.",
                true, ChatRepository.TYPE_CLAIM);
        DocumentSnapshot claimed = await(chatDoc(db, chatId));
        assertEquals(a.getUid(), claimed.getString("verifiedOwnerId"));
        assertEquals("Claim request: it has a blue stripe.", claimed.getString("lastMessage"));

        QuerySnapshot messages = await(db.collection("chats").document(chatId)
                .collection("messages").orderBy("createdAt").get());
        assertEquals(2, messages.size());
        assertEquals(Boolean.TRUE, messages.getDocuments().get(1).getBoolean("verified"));
    }

    // ------------------------------------------------------------------ helpers

    private Task<DocumentSnapshot> chatDoc(FirebaseFirestore db, String chatId) {
        return db.collection("chats").document(chatId).get();
    }

    private void waitForRead(FirebaseFirestore db, String chatId, String uid) throws Exception {
        for (int i = 0; i < 20; i++) {
            DocumentSnapshot doc = await(chatDoc(db, chatId));
            Long readAt = doc.getLong("lastRead." + uid);
            if (readAt != null && readAt > 0) {
                return;
            }
            Thread.sleep(250);
        }
    }

    private int unreadCount(String uid) throws Exception {
        final AtomicInteger result = new AtomicInteger(-1);
        final CountDownLatch latch = new CountDownLatch(1);
        ChatRepository.countUnread(uid, unread -> {
            result.set(unread);
            latch.countDown();
        });
        assertTrue("countUnread timed out", latch.await(20, TimeUnit.SECONDS));
        return result.get();
    }

    private void ensureChatBlocking(String itemId, String ownerId, String meUid,
                                    String otherId, String myName) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] error = new String[1];
        ChatRepository.ensureChat(itemId, "Black wallet", null, ownerId,
                meUid, myName, otherId, null, new ChatRepository.ChatCallback() {
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
        assertTrue("ensureChat timed out", latch.await(30, TimeUnit.SECONDS));
        assertNull("ensureChat failed: " + error[0], error[0]);
    }

    private void sendBlocking(String chatId, String senderId, String senderName,
                              String text, boolean verified, String type) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] error = new String[1];
        ChatRepository.sendMessage(chatId, senderId, senderName, text, type, verified,
                new ChatRepository.SendCallback() {
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
        assertTrue("sendMessage timed out", latch.await(30, TimeUnit.SECONDS));
        assertNull("sendMessage failed: " + error[0], error[0]);
    }

    private static <T> T await(Task<T> task) throws Exception {
        return Tasks.await(task, 40, TimeUnit.SECONDS);
    }
}
