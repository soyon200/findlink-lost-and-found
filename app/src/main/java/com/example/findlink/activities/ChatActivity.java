package com.example.findlink.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.findlink.R;
import com.example.findlink.adapters.ChatAdapter;
import com.example.findlink.util.SecurityCode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.findlink.models.ChatMessage;
import com.example.findlink.util.ChatRepository;
import com.example.findlink.util.WindowInsetsUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * দুই ইউজারের মধ্যে চলমান কথোপকথন।
 * মেসেজ রিয়েল-টাইমে আসে, তাই অন্য প্রান্ত থেকে reply দিলে সাথে সাথে দেখা যায়।
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "itemId";
    public static final String EXTRA_OTHER_USER_ID = "otherUserId";
    public static final String EXTRA_ITEM_TITLE = "itemTitle";
    public static final String EXTRA_ITEM_IMAGE = "itemImageUrl";
    public static final String EXTRA_POST_OWNER_ID = "postOwnerId";
    public static final String EXTRA_PREFILL = "prefill";
    /** ইনবক্স থেকে খুললে ঠিক যে থ্রেডটি খোলা হবে — দুই পক্ষ একই চ্যাট দেখবে। */
    public static final String EXTRA_CHAT_ID = "chatId";

    private RecyclerView recyclerViewMessages;
    private ChatAdapter chatAdapter;
    private EditText etMessage;
    private Button btnSend;
    private View layoutEmptyChat;
    private TextView tvChatName, tvChatInitials, tvChatItemTitle, tvChatVerified;
    private TextView tvVerifyTitle, tvVerifyHint;
    private View cardVerifyBanner;
    private ImageView ivVerifyIcon;
    private Button btnVerifyOwner, btnVerifyInline;

    private FirebaseAuth mAuth;

    private String currentUserId;
    private String otherUserId;
    private String chatId;
    private String itemId;
    private String myName = ChatRepository.FALLBACK_NAME;

    private ListenerRegistration messagesListener;
    private ListenerRegistration chatStateListener;
    private boolean sending;
    private boolean chatReady;
    // কোড-সংক্রান্ত UI শুধু LOST রিপোর্টের চ্যাটে দেখানো হয়
    private boolean codeUiAllowed;
    private boolean lastVerified;
    private boolean lastVerifiedOther;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        WindowInsetsUtil.applyTopPadding(findViewById(R.id.chatTopBar));
        WindowInsetsUtil.applySystemBarsAndImePadding(findViewById(R.id.layoutComposer));

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        layoutEmptyChat = findViewById(R.id.layoutEmptyChat);
        tvChatName = findViewById(R.id.tvChatName);
        tvChatInitials = findViewById(R.id.tvChatInitials);
        tvChatItemTitle = findViewById(R.id.tvChatItemTitle);
        tvChatVerified = findViewById(R.id.tvChatVerified);
        btnVerifyOwner = findViewById(R.id.btnVerifyOwner);
        btnVerifyInline = findViewById(R.id.btnVerifyInline);
        cardVerifyBanner = findViewById(R.id.cardVerifyBanner);
        ivVerifyIcon = findViewById(R.id.ivVerifyIcon);
        tvVerifyTitle = findViewById(R.id.tvVerifyTitle);
        tvVerifyHint = findViewById(R.id.tvVerifyHint);
        Button btnChatBack = findViewById(R.id.btnChatBack);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        String providedChatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        String itemTitle = getIntent().getStringExtra(EXTRA_ITEM_TITLE);
        String itemImageUrl = getIntent().getStringExtra(EXTRA_ITEM_IMAGE);
        String postOwnerId = getIntent().getStringExtra(EXTRA_POST_OWNER_ID);
        String prefill = getIntent().getStringExtra(EXTRA_PREFILL);

        // ইনবক্স থেকে এলে chatId সরাসরি দেওয়া থাকে — তখন দুই পক্ষ নিশ্চিতভাবে একই থ্রেড দেখে
        boolean hasChatId = !TextUtils.isEmpty(providedChatId);
        if (hasChatId) {
            chatId = providedChatId;
        } else {
            if (TextUtils.isEmpty(itemId) || TextUtils.isEmpty(otherUserId)
                    || currentUserId.equals(otherUserId)) {
                Toast.makeText(this, "This conversation is not available", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            chatId = ChatRepository.buildChatId(itemId, currentUserId, otherUserId);
        }

        tvChatItemTitle.setText(TextUtils.isEmpty(itemTitle) ? "About this item" : itemTitle);

        // রিসাইক্লার: নতুন মেসেজ নিচে যোগ হয়
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(currentUserId);
        recyclerViewMessages.setAdapter(chatAdapter);

        btnChatBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
        btnVerifyOwner.setOnClickListener(v -> showVerifyOwnerDialog());
        btnVerifyInline.setOnClickListener(v -> showVerifyOwnerDialog());
        hideVerifyUi();

        // খুব লম্বা মেসেজ আটকানো + কীবোর্ডের Send বাটন দিয়েও পাঠানো
        etMessage.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1000)});
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // চ্যাট ডকুমেন্ট তৈরি না হওয়া পর্যন্ত পাঠানো বন্ধ — মেসেজ হারানোর ঝুঁকি নেই
        btnSend.setEnabled(false);

        if (!TextUtils.isEmpty(prefill)) {
            etMessage.setText(prefill);
            etMessage.setSelection(prefill.length());
        }

        resolveVerifyUi();

        ChatRepository.loadUserName(currentUserId, name -> {
            myName = name;

            if (hasChatId) {
                // থ্রেড আগেই আছে: নাম/অংশগ্রহণকারী মিলিয়ে নিয়ে সাথে সাথে পাঠানো চালু
                chatReady = true;
                btnSend.setEnabled(true);
                startChatStateListener();
                ChatRepository.loadChatPartner(chatId, currentUserId, partnerId -> {
                    if (partnerId != null) {
                        otherUserId = partnerId;
                    }
                    refreshPartnerHeader(itemTitle);
                });
                return;
            }

            openOrCreateChat(itemId, itemTitle, itemImageUrl, postOwnerId, true);
        });
    }

    private void refreshPartnerHeader(String itemTitle) {
        if (TextUtils.isEmpty(otherUserId)) {
            return;
        }
        ChatRepository.loadUserName(otherUserId, name -> {
            tvChatName.setText(name);
            tvChatInitials.setText(ChatRepository.getInitials(name));
        });
    }

    /**
     * চ্যাট ডকুমেন্ট তৈরি/খোলা। ব্যর্থ হলে একবার আবার চেষ্টা করে, তারপর ব্যবহারকারীকে জানায়।
     */
    private void openOrCreateChat(String itemId, String itemTitle, String itemImageUrl,
                                  String postOwnerId, boolean allowRetry) {
        ChatRepository.ensureChat(itemId, itemTitle, itemImageUrl, postOwnerId,
                currentUserId, myName, otherUserId, null, new ChatRepository.ChatCallback() {
                    @Override
                    public void onReady(String readyChatId) {
                        chatReady = true;
                        btnSend.setEnabled(true);
                        startChatStateListener();
                        refreshPartnerHeader(itemTitle);
                    }

                    @Override
                    public void onError(String message) {
                        if (allowRetry) {
                            openOrCreateChat(itemId, itemTitle, itemImageUrl, postOwnerId, false);
                            return;
                        }
                        if (!isFinishing()) {
                            Toast.makeText(ChatActivity.this,
                                    "Could not open chat: " + message + ". Check your connection.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        if (chatStateListener != null) {
            chatStateListener.remove();
            chatStateListener = null;
        }
    }

    /**
     * চ্যাটের লাইভ অবস্থা: অন্য পক্ষ কত পর্যন্ত পড়েছে (Seen) এবং
     * সিকিউরিটি কোডে মালিকানা প্রমাণিত হয়েছে কি না (VERIFIED OWNER ব্যাজ)।
     */
    private void startChatStateListener() {
        if (chatId == null || chatStateListener != null) {
            return;
        }
        chatStateListener = ChatRepository.listenChatState(chatId,
                new ChatRepository.ChatStateCallback() {
                    @Override
                    public void onState(java.util.Map<String, Object> lastRead, String verifiedOwnerId) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        Object read = lastRead.get(otherUserId);
                        long readAt = read instanceof Number ? ((Number) read).longValue() : 0L;
                        chatAdapter.setOtherReadAt(readAt);

                        lastVerified = verifiedOwnerId != null && !verifiedOwnerId.isEmpty();
                        lastVerifiedOther = lastVerified && verifiedOwnerId.equals(otherUserId);
                        applyVerifyUi();
                    }

                    @Override
                    public void onError(String message) {
                        // ব্যাজ না দেখালেও চ্যাট চলবে
                    }
                });
    }

    /**
     * কোন আইটেমের চ্যাট সেটা দেখে ঠিক করে — LOST হলে কোডের ব্যানার/লক দেখাবে,
     * FOUND হলে একদমই দেখাবে না (শুধু সাধারণ মেসেজ)।
     */
    private void resolveVerifyUi() {
        if (TextUtils.isEmpty(itemId)) {
            codeUiAllowed = false;
            hideVerifyUi();
            return;
        }
        FirebaseFirestore.getInstance().collection("items").document(itemId).get()
                .addOnSuccessListener(document -> {
                    codeUiAllowed = document != null && document.exists()
                            && "LOST".equals(document.getString("reportType"));
                    applyVerifyUi();
                })
                .addOnFailureListener(error -> {
                    codeUiAllowed = false;
                    hideVerifyUi();
                });
    }

    /** যাচাই-ব্যানার, লক বাটন ও ব্যাজ — শুধু LOST চ্যাটে সঠিক অবস্থা অনুযায়ী সাজায়। */
    private void applyVerifyUi() {
        if (!codeUiAllowed) {
            hideVerifyUi();
            return;
        }
        tvChatVerified.setVisibility(lastVerifiedOther ? View.VISIBLE : View.GONE);
        bindVerifyBanner(lastVerified, lastVerifiedOther);
        btnVerifyOwner.setVisibility(lastVerified ? View.GONE : View.VISIBLE);
    }

    private void hideVerifyUi() {
        if (cardVerifyBanner != null) {
            cardVerifyBanner.setVisibility(View.GONE);
        }
        if (btnVerifyOwner != null) {
            btnVerifyOwner.setVisibility(View.GONE);
        }
        if (tvChatVerified != null) {
            tvChatVerified.setVisibility(View.GONE);
        }
    }

    /**
     * চ্যাটের ভেতরের ব্যানার — যাচাই হয়েছে কি না সেটা কথোপকথনের মাঝেই দেখা যায়।
     */
    private void bindVerifyBanner(boolean verified, boolean verifiedOther) {
        if (cardVerifyBanner == null) {
            return;
        }
        int color = getColor(verified ? R.color.colorSuccess : R.color.colorWarning);
        cardVerifyBanner.setVisibility(View.VISIBLE);
        cardVerifyBanner.setBackgroundResource(
                verified ? R.drawable.bg_verify_banner_ok : R.drawable.bg_verify_banner);
        ivVerifyIcon.setImageResource(verified ? R.drawable.ic_check : R.drawable.ic_lock);
        ivVerifyIcon.setImageTintList(ColorStateList.valueOf(color));
        tvVerifyTitle.setTextColor(color);
        tvVerifyTitle.setText(verified
                ? (verifiedOther ? "Owner verified by security code" : "You are verified as the owner")
                : "Owner not verified yet");
        tvVerifyHint.setText(verified
                ? "The security code matched the owner's lost report."
                : "Ask for the 4–6 digit security code before handing the item over.");
        btnVerifyInline.setVisibility(verified ? View.GONE : View.VISIBLE);
    }

    /**
     * চ্যাটেই মালিকানা যাচাই: অন্য পক্ষ তাঁর LOST পোস্টে যে কোড দিয়েছিলেন সেটি লিখলে
     * কোডের হ্যাশ মিলিয়ে দেখা হয়। মিললে দুই পক্ষই VERIFIED OWNER ব্যাজ দেখবে।
     */
    private void showVerifyOwnerDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        input.setHint("4–6 digit code");

        FrameLayout box = new FrameLayout(this);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(padding, padding / 2, padding, 0);
        box.addView(input, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Verify the owner")
                .setMessage("Ask the person to tell you the security code they set on their "
                        + "lost report. If the code matches, this chat shows VERIFIED OWNER.")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    if (!SecurityCode.isValid(code)) {
                        Toast.makeText(this, "Use 4–6 digits", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (otherUserId == null || chatId == null) {
                        Toast.makeText(this, "Chat is still opening", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ChatRepository.verifyOwnerCode(otherUserId, code, verified -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (verified) {
                            ChatRepository.markOwnerVerified(chatId, otherUserId,
                                    tvChatName.getText().toString());
                            Toast.makeText(this, "Owner verified", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                    "This code does not match their lost report",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    private void startListening() {
        if (chatId == null || messagesListener != null) {
            return;
        }
        messagesListener = ChatRepository.listenMessages(chatId, new ChatRepository.MessagesCallback() {
            @Override
            public void onMessages(List<ChatMessage> messages) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                int previousSize = chatAdapter.size();
                chatAdapter.submit(messages);
                layoutEmptyChat.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
                if (messages.size() != previousSize) {
                    scrollToBottom();
                }
                if (!messages.isEmpty()) {
                    ChatRepository.markRead(chatId, currentUserId);
                }
            }

            @Override
            public void onError(String message) {
                if (!isFinishing()) {
                    Toast.makeText(ChatActivity.this,
                            "Could not load messages: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendMessage() {
        if (sending || chatId == null) {
            return;
        }
        if (!chatReady) {
            Toast.makeText(this, "Opening the chat — try again in a second", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        sending = true;
        btnSend.setEnabled(false);

        ChatRepository.sendMessage(chatId, currentUserId, myName, text, ChatRepository.TYPE_CHAT,
                new ChatRepository.SendCallback() {
                    @Override
                    public void onSent() {
                        sending = false;
                        btnSend.setEnabled(true);
                        etMessage.setText("");
                        scrollToBottom();
                    }

                    @Override
                    public void onError(String message) {
                        sending = false;
                        btnSend.setEnabled(true);
                        if (!isFinishing()) {
                            Toast.makeText(ChatActivity.this,
                                    "Message not sent: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void scrollToBottom() {
        recyclerViewMessages.post(() -> {
            int count = chatAdapter.getItemCount();
            if (count > 0) {
                recyclerViewMessages.scrollToPosition(count - 1);
            }
        });
    }
}
