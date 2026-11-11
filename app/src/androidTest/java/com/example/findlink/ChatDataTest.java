package com.example.findlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.findlink.util.ChatRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * শুধু ডেটা লেয়ার যাচাই (কোনো UI/স্ক্রিনশট নয়) — কোথায় আটকায় তা দ্রুত ধরার জন্য।
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ChatDataTest {

    private static final String TAG = "ChatDataTest";
    private static final String PASSWORD = "Test1234!";

    @Test
    public void twoUsersExchangeMessages() throws Exception {
        step("start");
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        auth.signOut();

        InstrumentationRegistry.getInstrumentation().getTargetContext();
        String stamp = String.valueOf(System.currentTimeMillis());
        String emailA = "fl.a." + stamp + "@findlink.test";
        String emailB = "fl.b." + stamp + "@findlink.test";

        step("create A");
        FirebaseUser userA = await(auth.createUserWithEmailAndPassword(emailA, PASSWORD)).getUser();
        assertNotNull(userA);
        await(db.collection("users").document(userA.getUid()).set(userDoc("Ayesha Test", emailA)));

        step("create item");
        String itemId = db.collection("items").document().getId();
        Map<String, Object> item = new HashMap<>();
        item.put("itemId", itemId);
        item.put("userId", userA.getUid());
        item.put("title", "Black umbrella with wooden handle");
        item.put("category", "Accessories");
        item.put("location", "Central Library, 2nd floor");
        item.put("description", "Folded black umbrella with a wooden handle and a small blue tag.");
        item.put("reportType", "LOST");
        item.put("status", "ACTIVE");
        item.put("phone", "+8801712345678");
        item.put("timestamp", System.currentTimeMillis());
        await(db.collection("items").document(itemId).set(item));

        step("create B");
        FirebaseUser userB = await(auth.createUserWithEmailAndPassword(emailB, PASSWORD)).getUser();
        assertNotNull(userB);
        await(db.collection("users").document(userB.getUid()).set(userDoc("Rahim Test", emailB)));

        step("ensure chat");
        String chatId = ChatRepository.buildChatId(itemId, userA.getUid(), userB.getUid());
        ensureChatBlocking(itemId, userA.getUid(), userB.getUid());

        step("B sends");
        sendBlocking(chatId, userB.getUid(), "Rahim Test",
                "Hi, I found your umbrella near the library entrance.", ChatRepository.TYPE_CHAT);

        step("A reads");
        auth.signOut();
        await(auth.signInWithEmailAndPassword(emailA, PASSWORD));
        QuerySnapshot afterB = await(db.collection("chats").document(chatId)
                .collection("messages").get());
        assertEquals(1, afterB.size());

        step("A replies");
        sendBlocking(chatId, userA.getUid(), "Ayesha Test",
                "Thanks! I can pick it up tomorrow.", ChatRepository.TYPE_CHAT);

        QuerySnapshot all = await(db.collection("chats").document(chatId)
                .collection("messages").orderBy("createdAt").get());
        assertEquals(2, all.size());
        step("done");
    }

    private void step(String name) {
        Log.i(TAG, "STEP: " + name);
        System.out.println("ChatDataTest STEP: " + name);
    }

    private void ensureChatBlocking(String itemId, String ownerId, String otherId) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] error = new String[1];
        ChatRepository.ensureChat(itemId, "Black umbrella with wooden handle", null, ownerId,
                otherId, "Rahim Test", ownerId, "Ayesha Test", new ChatRepository.ChatCallback() {
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
        assertNull(error[0]);
    }

    private void sendBlocking(String chatId, String senderId, String senderName,
                              String text, String type) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] error = new String[1];
        ChatRepository.sendMessage(chatId, senderId, senderName, text, type,
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
