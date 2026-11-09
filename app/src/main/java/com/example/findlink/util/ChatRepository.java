package com.example.findlink.util;

import android.util.Log;

import com.example.findlink.models.ChatMessage;
import com.example.findlink.models.ConversationModel;
import com.example.findlink.util.UserProfileSync;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * দুই ইউজারের মধ্যে চলমান কথোপকথনের ডেটা লেয়ার।
 *
 * ডেটা স্ট্রাকচার:
 * chats/{chatId}
 *   participants: [uidA, uidB]
 *   participantNames: { uid: name }
 *   itemId, itemTitle, itemImageUrl, postOwnerId
 *   lastMessage, lastSenderId, lastSenderName, lastMessageAt, createdAt
 *   lastRead: { uid: timestamp }
 * chats/{chatId}/messages/{messageId}
 *   senderId, senderName, text, createdAt, type
 *
 * chatId নির্ধারিত হয় itemId + সাজানো uid জোড়া দিয়ে, তাই একই আইটেমে একই
 * দুইজনের জন্য বারবার নতুন চ্যাট ডকুমেন্ট তৈরি হয় না।
 */
public final class ChatRepository {

    private static final String TAG = "ChatRepository";

    public static final String CHATS = "chats";
    public static final String MESSAGES = "messages";
    public static final String USERS = "users";

    public static final String TYPE_CHAT = "CHAT";
    public static final String TYPE_CLAIM = "CLAIM";

    public static final String FALLBACK_NAME = "FindLink user";

    private ChatRepository() {
    }

    // ------------------------------------------------------------------ callbacks

    public interface ChatCallback {
        void onReady(String chatId);
        void onError(String message);
    }

    public interface SendCallback {
        void onSent();
        void onError(String message);
    }

    public interface NameCallback {
        void onName(String name);
    }

    public interface MessagesCallback {
        void onMessages(List<ChatMessage> messages);
        void onError(String message);
    }

    public interface ConversationsCallback {
        void onConversations(List<ConversationModel> conversations);
        void onError(String message);
    }

    /** চ্যাট ডকুমেন্টের লাইভ অবস্থা: কে পর্যন্ত পড়েছে + মালিকানা যাচাই হয়েছে কি না। */
    public interface ChatStateCallback {
        void onState(Map<String, Object> lastRead, String verifiedOwnerId);
        void onError(String message);
    }

    public interface UnreadCallback {
        void onCount(int unread);
    }

    // ------------------------------------------------------------------ helpers

    /**
     * একই আইটেম + একই দুই ইউজারের জন্য সবসময় একই chatId।
     */
    public static String buildChatId(String itemId, String uidA, String uidB) {
        String a = uidA == null ? "" : uidA;
        String b = uidB == null ? "" : uidB;
        boolean aFirst = a.compareTo(b) <= 0;
        String first = aFirst ? a : b;
        String second = aFirst ? b : a;
        return itemId + "__" + first + "__" + second;
    }

    public static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "FL";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && initials.length() < 2) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return initials.length() == 0 ? "FL" : initials.toString();
    }

    // ------------------------------------------------------------------ chat doc

    /**
     * চ্যাট ডকুমেন্ট না থাকলে তৈরি করে, থাকলে অংশগ্রহণকারীদের নাম হালনাগাদ করে।
     */
    public static void ensureChat(String itemId,
                                  String itemTitle,
                                  String itemImageUrl,
                                  String postOwnerId,
                                  String myUid,
                                  String myName,
                                  String otherUid,
                                  String otherName,
                                  ChatCallback callback) {
        if (itemId == null || itemId.isEmpty() || myUid == null || otherUid == null) {
            callback.onError("Missing chat details");
            return;
        }

        final String chatId = buildChatId(itemId, myUid, otherUid);
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference chatRef = db.collection(CHATS).document(chatId);

        final String safeMyName = myName == null || myName.trim().isEmpty() ? FALLBACK_NAME : myName;
        final String safeOtherName = otherName == null || otherName.trim().isEmpty() ? FALLBACK_NAME : otherName;

        Map<String, Object> names = new HashMap<>();
        names.put(myUid, safeMyName);
        names.put(otherUid, safeOtherName);

        long now = System.currentTimeMillis();
        Map<String, Object> chat = new HashMap<>();
        chat.put("chatId", chatId);
        chat.put("itemId", itemId);
        chat.put("itemTitle", itemTitle == null ? "" : itemTitle);
        chat.put("itemImageUrl", itemImageUrl == null ? "" : itemImageUrl);
        chat.put("postOwnerId", postOwnerId == null ? "" : postOwnerId);
        chat.put("participants", Arrays.asList(myUid, otherUid));
        chat.put("participantNames", names);
        chat.put("lastMessage", "");
        chat.put("lastSenderId", "");
        chat.put("lastSenderName", "");
        chat.put("lastMessageAt", 0L);
        chat.put("createdAt", now);
        chat.put("lastRead", new HashMap<String, Object>());

        Map<String, Object> patch = new HashMap<>();
        patch.put("participantNames." + myUid, safeMyName);
        patch.put("participantNames." + otherUid, safeOtherName);

        // transaction: দুইজন একসাথে চ্যাট খুললেও কেউ অন্যের শেষ মেসেজ মুছে দিতে পারবে না
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(chatRef);
            if (!snapshot.exists()) {
                transaction.set(chatRef, chat);
            } else {
                transaction.update(chatRef, patch);
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onReady(chatId))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "ensureChat failed", e);
                    callback.onError(e.getMessage() == null ? "Could not open chat" : e.getMessage());
                });
    }

    // ------------------------------------------------------------------ messages

    /**
     * মেসেজ সেভ করে এবং চ্যাট ডকুমেন্টের শেষ মেসেজ আপডেট করে।
     */
    public static void sendMessage(String chatId,
                                   String senderId,
                                   String senderName,
                                   String text,
                                   String type,
                                   SendCallback callback) {
        sendMessage(chatId, senderId, senderName, text, type, false, callback);
    }

    /**
     * @param verified CLAIM মেসেজে সিকিউরিটি কোড যাচাই হয়েছে কি না
     */
    public static void sendMessage(String chatId,
                                   String senderId,
                                   String senderName,
                                   String text,
                                   String type,
                                   boolean verified,
                                   SendCallback callback) {
        if (chatId == null || text == null || text.trim().isEmpty()) {
            callback.onError("Message is empty");
            return;
        }

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference chatRef = db.collection(CHATS).document(chatId);
        final DocumentReference messageRef = chatRef.collection(MESSAGES).document();

        final String safeName = senderName == null || senderName.trim().isEmpty() ? FALLBACK_NAME : senderName;
        final long now = System.currentTimeMillis();
        final String messageType = type == null || type.isEmpty() ? TYPE_CHAT : type;

        Map<String, Object> message = new HashMap<>();
        message.put("messageId", messageRef.getId());
        message.put("senderId", senderId);
        message.put("senderName", safeName);
        message.put("text", text.trim());
        message.put("createdAt", now);
        message.put("type", messageType);
        message.put("verified", verified);

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", text.trim());
        updates.put("lastSenderId", senderId);
        updates.put("lastSenderName", safeName);
        updates.put("lastMessageAt", now);
        updates.put("participantNames." + senderId, safeName);
        // সিকিউরিটি কোডে মালিকানা প্রমাণিত হলে চ্যাটেও সেটা চিহ্নিত থাকে
        if (verified && TYPE_CLAIM.equals(messageType)) {
            updates.put("verifiedOwnerId", senderId);
            updates.put("verifiedOwnerName", safeName);
        }

        // মেসেজ + চ্যাটের সারাংশ একসাথে (atomic) — একটার জন্য অন্যটা হারায় না,
        // আর onSent তখনই আসে যখন ইনবক্সেও মেসেজটি দেখা যাবে।
        WriteBatch batch = db.batch();
        batch.set(messageRef, message);
        batch.update(chatRef, updates);
        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSent())
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() == null ? "Could not send message" : e.getMessage()));
    }

    /**
     * রিয়েল-টাইম মেসেজ লিসেনার (পুরনো → নতুন)।
     */
    public static ListenerRegistration listenMessages(String chatId, MessagesCallback callback) {
        return FirebaseFirestore.getInstance()
                .collection(CHATS).document(chatId).collection(MESSAGES)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage() == null ? "Could not load messages"
                                : error.getMessage());
                        return;
                    }
                    List<ChatMessage> messages = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            ChatMessage message = doc.toObject(ChatMessage.class);
                            message.setMessageId(doc.getId());
                            messages.add(message);
                        }
                    }
                    callback.onMessages(messages);
                });
    }

    // ------------------------------------------------------------------ conversations

    /**
     * আমার সব কথোপকথন রিয়েল-টাইমে, সর্বশেষ মেসেজ অনুযায়ী সাজিয়ে।
     * (whereArrayContains + orderBy একসাথে করলে কম্পোজিট ইনডেক্স লাগে, তাই
     *  সর্টিং ক্লায়েন্ট সাইডে করা হচ্ছে।)
     */
    public static ListenerRegistration listenConversations(String uid, ConversationsCallback callback) {
        return FirebaseFirestore.getInstance()
                .collection(CHATS)
                .whereArrayContains("participants", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage() == null ? "Could not load conversations"
                                : error.getMessage());
                        return;
                    }
                    List<ConversationModel> conversations = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            ConversationModel model = toConversation(doc, uid);
                            if (model != null) {
                                conversations.add(model);
                            }
                        }
                    }
                    Collections.sort(conversations, (a, b) ->
                            Long.compare(b.getLastMessageAt(), a.getLastMessageAt()));
                    callback.onConversations(conversations);
                });
    }

    /**
     * মোট অপঠিত কথোপকথনের সংখ্যা (প্রোফাইলের Inbox বাটনে ব্যাজ দেখানোর জন্য)।
     */
    public static void countUnread(String uid, UnreadCallback callback) {
        if (uid == null) {
            callback.onCount(0);
            return;
        }
        FirebaseFirestore.getInstance().collection(CHATS)
                .whereArrayContains("participants", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    int unread = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ConversationModel model = toConversation(doc, uid);
                        if (model != null && model.isUnread()) {
                            unread++;
                        }
                    }
                    callback.onCount(unread);
                })
                .addOnFailureListener(e -> callback.onCount(0));
    }

    private static ConversationModel toConversation(DocumentSnapshot doc, String uid) {
        List<String> participants = readStringList(doc.get("participants"));
        if (participants.size() < 2) {
            return null;
        }
        String otherUid = null;
        for (String participant : participants) {
            if (!uid.equals(participant)) {
                otherUid = participant;
                break;
            }
        }
        if (otherUid == null) {
            return null;
        }

        Map<String, Object> names = readMap(doc.get("participantNames"));
        String otherName = names.get(otherUid) == null ? FALLBACK_NAME : String.valueOf(names.get(otherUid));

        Map<String, Object> lastRead = readMap(doc.get("lastRead"));
        long readAt = readLong(lastRead.get(uid));
        long lastMessageAt = readLong(doc.get("lastMessageAt"));
        String lastSenderId = doc.getString("lastSenderId");

        ConversationModel model = new ConversationModel();
        model.setChatId(doc.getId());
        model.setItemId(doc.getString("itemId"));
        model.setItemTitle(doc.getString("itemTitle"));
        model.setItemImageUrl(doc.getString("itemImageUrl"));
        model.setPostOwnerId(doc.getString("postOwnerId"));
        model.setOtherUserId(otherUid);
        model.setOtherUserName(otherName);
        model.setLastMessage(doc.getString("lastMessage"));
        model.setLastSenderId(lastSenderId);
        model.setLastMessageAt(lastMessageAt);
        model.setUnread(lastMessageAt > readAt && lastSenderId != null
                && !lastSenderId.isEmpty() && !lastSenderId.equals(uid));
        return model;
    }

    public interface PartnerCallback {
        void onPartner(String otherUserId);
    }

    public interface VerifyCallback {
        void onResult(boolean verified);
    }

    /**
     * চ্যাটের অন্য পক্ষ সত্যিই মালিক কি না — তিনি তাঁর LOST পোস্টে যে কোড সেট করেছিলেন,
     * সেটি মিলিয়ে দেখা হয়। কোড কখনো পাঠানো/দেখানো হয় না, শুধু হ্যাশ মেলানো হয়।
     */
    public static void verifyOwnerCode(String ownerUid, String code, VerifyCallback callback) {
        if (ownerUid == null || !SecurityCode.isValid(code)) {
            callback.onResult(false);
            return;
        }
        FirebaseFirestore.getInstance().collection("items")
                .whereEqualTo("userId", ownerUid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    boolean verified = false;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String hash = doc.getString("securityHash");
                        String itemId = doc.getString("itemId");
                        if (hash == null || itemId == null) {
                            continue;
                        }
                        if (SecurityCode.matches(hash, itemId, code)) {
                            verified = true;
                            break;
                        }
                    }
                    callback.onResult(verified);
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }

    /** যাচাই সফল হলে চ্যাটে চিহ্ন রেখে দেওয়া হয়, যাতে ব্যাজ দুই পক্ষই দেখতে পান। */
    public static void markOwnerVerified(String chatId, String ownerUid, String ownerName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("verifiedOwnerId", ownerUid);
        updates.put("verifiedOwnerName", ownerName == null ? FALLBACK_NAME : ownerName);
        FirebaseFirestore.getInstance().collection(CHATS).document(chatId).update(updates)
                .addOnFailureListener(e -> Log.w(TAG, "markOwnerVerified failed", e));
    }

    /**
     * চ্যাট ডকুমেন্ট থেকে অন্য অংশগ্রহণকারীর uid বের করে (কখনো extra হারালেও কাজ করবে)।
     */
    public static void loadChatPartner(String chatId, String myUid, PartnerCallback callback) {
        FirebaseFirestore.getInstance().collection(CHATS).document(chatId).get()
                .addOnSuccessListener(doc -> {
                    String partner = null;
                    if (doc != null && doc.exists()) {
                        for (String uid : readStringList(doc.get("participants"))) {
                            if (!uid.equals(myUid)) {
                                partner = uid;
                                break;
                            }
                        }
                    }
                    callback.onPartner(partner);
                })
                .addOnFailureListener(e -> callback.onPartner(null));
    }

    /**
     * চ্যাট ডকুমেন্টের লাইভ অবস্থা — "Seen" আর verified owner ব্যাজের জন্য।
     */
    public static ListenerRegistration listenChatState(String chatId, ChatStateCallback callback) {
        return FirebaseFirestore.getInstance().collection(CHATS).document(chatId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage() == null ? "Could not load chat"
                                : error.getMessage());
                        return;
                    }
                    Map<String, Object> lastRead = new HashMap<>();
                    String verifiedOwnerId = null;
                    if (document != null && document.exists()) {
                        lastRead = readMap(document.get("lastRead"));
                        verifiedOwnerId = document.getString("verifiedOwnerId");
                    }
                    callback.onState(lastRead, verifiedOwnerId);
                });
    }

    // ------------------------------------------------------------------ read state

    public static void markRead(String chatId, String uid) {
        if (chatId == null || uid == null) {
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastRead." + uid, System.currentTimeMillis());
        FirebaseFirestore.getInstance().collection(CHATS).document(chatId)
                .update(updates)
                .addOnFailureListener(e -> Log.w(TAG, "Could not mark chat as read", e));
    }

    // ------------------------------------------------------------------ names

    /**
     * users/{uid} থেকে নাম আনে; না থাকলে ফলব্যাক নাম।
     */
    public static void loadUserName(String uid, NameCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onName(FALLBACK_NAME);
            return;
        }
        FirebaseFirestore.getInstance().collection(USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = null;
                    String email = null;
                    if (doc != null && doc.exists()) {
                        name = doc.getString("name");
                        email = doc.getString("email");
                    }
                    // নিজের অ্যাকাউন্ট হলে Auth-এর ইমেইলও ব্যবহার করা যায়
                    if (name == null || name.trim().isEmpty()) {
                        com.google.firebase.auth.FirebaseUser me =
                                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                        if (me != null && uid.equals(me.getUid()) && me.getEmail() != null) {
                            email = me.getEmail();
                        }
                    }
                    callback.onName(UserProfileSync.displayName(name, email));
                })
                .addOnFailureListener(e -> callback.onName(FALLBACK_NAME));
    }

    // ------------------------------------------------------------------ parsing

    private static List<String> readStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object entry : (List<?>) value) {
                if (entry != null) {
                    result.add(String.valueOf(entry));
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    private static long readLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}
