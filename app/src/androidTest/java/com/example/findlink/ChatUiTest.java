package com.example.findlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.findlink.activities.ChatActivity;
import com.example.findlink.activities.InboxActivity;
import com.example.findlink.util.ChatRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * শুধু UI যাচাই: সিড করা কথোপকথন দিয়ে চ্যাট স্ক্রিন ও ইনবক্সের স্ক্রিনশট।
 * প্রতিটি ধাপ লগে লেখা হয়, তাই কোথায় আটকায় বোঝা যায়।
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ChatUiTest {

    private static final String TAG = "ChatUiTest";
    private static final String PASSWORD = "Test1234!";

    @Test
    public void captureChatAndInbox() throws Exception {
        step("start");
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        auth.signOut();

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String stamp = String.valueOf(System.currentTimeMillis());
        String email = "fl.ui." + stamp + "@findlink.test";
        String otherUid = "finder-" + stamp;

        FirebaseUser me = await(auth.createUserWithEmailAndPassword(email, PASSWORD)).getUser();
        assertNotNull(me);
        await(db.collection("users").document(me.getUid()).set(userDoc("Ayesha Test", email)));
        await(db.collection("users").document(otherUid).set(userDoc("Rahim Test", "rahim@findlink.test")));
        step("users ready");

        String itemId = db.collection("items").document().getId();
        Map<String, Object> item = new HashMap<>();
        item.put("itemId", itemId);
        item.put("userId", me.getUid());
        item.put("title", "Black umbrella with wooden handle");
        item.put("category", "Accessories");
        item.put("location", "Central Library, 2nd floor");
        item.put("description", "Folded black umbrella with a wooden handle and a small blue tag.");
        item.put("reportType", "LOST");
        item.put("status", "ACTIVE");
        item.put("phone", "+8801712345678");
        item.put("timestamp", System.currentTimeMillis());
        await(db.collection("items").document(itemId).set(item));
        step("item saved");

        String chatId = ChatRepository.buildChatId(itemId, me.getUid(), otherUid);
        ensureChatBlocking(itemId, me.getUid(), otherUid);
        step("chat ready");

        sendBlocking(chatId, otherUid, "Rahim Test",
                "Hi, I found your umbrella near the library entrance.", ChatRepository.TYPE_CHAT);
        sendBlocking(chatId, otherUid, "Rahim Test",
                "It has a small blue tag on the handle.", ChatRepository.TYPE_CHAT);
        sendBlocking(chatId, me.getUid(), "Ayesha Test",
                "Oh that's mine! Is it still at the library desk?", ChatRepository.TYPE_CHAT);
        step("messages seeded");

        Intent chatIntent = new Intent(context, ChatActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(ChatActivity.EXTRA_ITEM_ID, itemId)
                .putExtra(ChatActivity.EXTRA_OTHER_USER_ID, otherUid)
                .putExtra(ChatActivity.EXTRA_ITEM_TITLE, "Black umbrella with wooden handle")
                .putExtra(ChatActivity.EXTRA_POST_OWNER_ID, me.getUid());

        step("launching chat");
        try (ActivityScenario<ChatActivity> scenario = ActivityScenario.launch(chatIntent)) {
            step("chat launched");
            sleep(3000);

            final int[] messageCount = new int[1];
            scenario.onActivity(activity -> {
                TextView name = activity.findViewById(R.id.tvChatName);
                TextView title = activity.findViewById(R.id.tvChatItemTitle);
                RecyclerView list = activity.findViewById(R.id.recyclerViewMessages);
                View empty = activity.findViewById(R.id.layoutEmptyChat);

                assertEquals("Rahim Test", name.getText().toString());
                assertEquals("Black umbrella with wooden handle", title.getText().toString());
                assertNotNull(list.getAdapter());
                messageCount[0] = list.getAdapter().getItemCount();
                assertEquals("Seeded messages are not bound to the list", 3, messageCount[0]);
                assertEquals(View.GONE, empty.getVisibility());

                captureView(activity, "15_chat");
            });
            step("chat captured with " + messageCount[0] + " messages");

            scenario.onActivity(activity -> {
                EditText input = activity.findViewById(R.id.etMessage);
                input.setText("Great — I'll come by at 5 PM.");
                activity.findViewById(R.id.btnSend).performClick();
            });
            step("send clicked");
            sleep(3000);

            scenario.onActivity(activity -> {
                RecyclerView list = activity.findViewById(R.id.recyclerViewMessages);
                assertEquals("Sent message did not appear in the thread", 4,
                        list.getAdapter().getItemCount());
                captureView(activity, "16_chat_sent");
            });
            step("chat after send captured");
        }
        step("chat closed");

        step("launching inbox");
        try (ActivityScenario<InboxActivity> inbox = ActivityScenario.launch(InboxActivity.class)) {
            step("inbox launched");
            sleep(3000);
            inbox.onActivity(activity -> {
                RecyclerView list = activity.findViewById(R.id.recyclerViewInbox);
                assertNotNull("Inbox adapter missing", list.getAdapter());
                assertTrue("Conversation was not listed in the inbox",
                        list.getAdapter().getItemCount() >= 1);
                captureView(activity, "17_inbox_chat");
            });
            step("inbox captured");
        }
        // টেস্ট শেষে টেস্ট-অ্যাকাউন্ট থেকে লগআউট, যাতে ফোনে আসল ইউজার লগইন করতে পারে
        auth.signOut();
        step("done");
    }

    /**
     * অ্যাক্টিভিটির নিজের ভিউ থেকে ছবি আঁকা হয় — ফোনে অন্য অ্যাপ/স্ক্রিন
     * সামনে থাকলেও সঠিক স্ক্রিনশট পাওয়া যায়।
     */
    private void captureView(android.app.Activity activity, String name) {
        View root = activity.getWindow().getDecorView();
        if (root.getWidth() == 0 || root.getHeight() == 0) {
            return;
        }
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(),
                Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        root.draw(canvas);

        File dir = new File(activity.getExternalFilesDir(null), "screenshots");
        if (!dir.exists() && !dir.mkdirs()) {
            bitmap.recycle();
            return;
        }
        File file = new File(dir, name + ".png");
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            Log.w(TAG, "Could not save screenshot " + name, e);
        } finally {
            bitmap.recycle();
        }
    }

    private void step(String name) {
        Log.i(TAG, "STEP: " + name);
        System.out.println("ChatUiTest STEP: " + name);
    }

    private void ensureChatBlocking(String itemId, String ownerId, String otherId) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] error = new String[1];
        ChatRepository.ensureChat(itemId, "Black umbrella with wooden handle", null, ownerId,
                ownerId, "Ayesha Test", otherId, "Rahim Test", new ChatRepository.ChatCallback() {
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

    private void capture(String name) throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> { });
        sleep(300);

        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        if (bitmap == null) {
            return;
        }

        File dir = new File(
                InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),
                "screenshots");
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }

        File file = new File(dir, name + ".png");
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } finally {
            bitmap.recycle();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
