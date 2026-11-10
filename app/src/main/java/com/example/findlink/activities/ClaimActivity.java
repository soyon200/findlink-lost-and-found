package com.example.findlink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.findlink.R;
import com.example.findlink.util.ChatRepository;
import com.example.findlink.util.WindowInsetsUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * একটি FOUND রিপোর্টের মালিকানা দাবি।
 *
 * এখন প্রমাণের সাথে মালিকের নিজের সিকিউরিটি কোডও লাগে। কোডটি দাবিদার নিজের
 * LOST পোস্টে সেট করেছিলেন; অ্যাপ প্রতিটি LOST পোস্টের হ্যাশের সাথে মিলিয়ে
 * দেখে। মিললে claim টি VERIFIED হিসেবে চ্যাটে যায় — পেয়ে যাওয়া ব্যক্তি
 * তখন নিশ্চিত হতে পারেন যে জিনিসটি আসল মালিককেই দেওয়া হচ্ছে।
 */
public class ClaimActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "itemId";
    public static final String EXTRA_ITEM_TITLE = "itemTitle";
    public static final String EXTRA_ITEM_IMAGE = "itemImageUrl";
    public static final String EXTRA_POST_OWNER_ID = "postOwnerId";

    private EditText etProofAnswer;
    private Button btnSubmitClaim, btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_claim);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        etProofAnswer = findViewById(R.id.etProofAnswer);
        btnSubmitClaim = findViewById(R.id.btnSubmitClaim);
        Button btnAskFinder = findViewById(R.id.btnAskFinder);
        btnBack = findViewById(R.id.btnBack);

        WindowInsetsUtil.applyTopPadding(findViewById(R.id.claimTopBar));

        btnBack.setOnClickListener(v -> finish());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        btnSubmitClaim.setOnClickListener(v -> submitClaim());

        String ownerId = getIntent().getStringExtra(EXTRA_POST_OWNER_ID);
        boolean canMessage = !TextUtils.isEmpty(ownerId);
        btnAskFinder.setVisibility(canMessage ? Button.VISIBLE : Button.GONE);
        btnAskFinder.setOnClickListener(v -> askFinder());
    }

    /**
     * FOUND পোস্টে কোড লাগে না — প্রমাণ লিখে সরাসরি ফাইন্ডারের সাথে কথা বলা যায়।
     */
    private void askFinder() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }
        String myUid = mAuth.getCurrentUser().getUid();
        String itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        String itemTitle = getIntent().getStringExtra(EXTRA_ITEM_TITLE);
        String itemImageUrl = getIntent().getStringExtra(EXTRA_ITEM_IMAGE);
        String ownerId = getIntent().getStringExtra(EXTRA_POST_OWNER_ID);

        if (TextUtils.isEmpty(itemId) || TextUtils.isEmpty(ownerId) || ownerId.equals(myUid)) {
            Toast.makeText(this, "Cannot open the chat for this item", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatRepository.loadUserName(myUid, myName ->
                ChatRepository.ensureChat(itemId, itemTitle, itemImageUrl, ownerId,
                        myUid, myName, ownerId, null, new ChatRepository.ChatCallback() {
                            @Override
                            public void onReady(String chatId) {
                                Intent intent = new Intent(ClaimActivity.this, ChatActivity.class);
                                intent.putExtra(ChatActivity.EXTRA_CHAT_ID, chatId);
                                intent.putExtra(ChatActivity.EXTRA_ITEM_ID, itemId);
                                intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, ownerId);
                                intent.putExtra(ChatActivity.EXTRA_ITEM_TITLE, itemTitle);
                                intent.putExtra(ChatActivity.EXTRA_ITEM_IMAGE, itemImageUrl);
                                intent.putExtra(ChatActivity.EXTRA_POST_OWNER_ID, ownerId);
                                 intent.putExtra(ChatActivity.EXTRA_PREFILL,
                                         "Hi, I think this is my item. Can you tell me a bit more about it so we can confirm?");
                                startActivity(intent);
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(ClaimActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                        }));
    }

    /**
     * FOUND পোস্টে কোড লাগে না — শুধু প্রমাণ লিখে দাবি পাঠানো হয়, প্রকাশ্য চ্যাটে কথা হয়।
     */
    private void submitClaim() {
        String answer = etProofAnswer.getText().toString().trim();
        if (answer.isEmpty()) {
            Toast.makeText(this, "Please write your proof", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        String myUid = mAuth.getCurrentUser().getUid();
        String itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        String itemTitle = getIntent().getStringExtra(EXTRA_ITEM_TITLE);
        String itemImageUrl = getIntent().getStringExtra(EXTRA_ITEM_IMAGE);
        String ownerId = getIntent().getStringExtra(EXTRA_POST_OWNER_ID);

        if (TextUtils.isEmpty(itemId) || TextUtils.isEmpty(ownerId) || ownerId.equals(myUid)) {
            Toast.makeText(this, "This item cannot be claimed", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitClaim.setEnabled(false);
        btnSubmitClaim.setText("Submitting…");

        sendClaim(myUid, itemId, itemTitle, itemImageUrl, ownerId, answer, false);
    }

    private void submitClaimData(String answer, boolean verified) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }
        String myUid = mAuth.getCurrentUser().getUid();
        String itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        String itemTitle = getIntent().getStringExtra(EXTRA_ITEM_TITLE);
        String itemImageUrl = getIntent().getStringExtra(EXTRA_ITEM_IMAGE);
        String ownerId = getIntent().getStringExtra(EXTRA_POST_OWNER_ID);

        if (TextUtils.isEmpty(itemId) || TextUtils.isEmpty(ownerId) || ownerId.equals(myUid)) {
            Toast.makeText(this, "This item cannot be claimed", Toast.LENGTH_SHORT).show();
            return;
        }
        sendClaim(myUid, itemId, itemTitle, itemImageUrl, ownerId, answer, verified);
    }

    private void sendClaim(String myUid, String itemId, String itemTitle, String itemImageUrl,
                           String ownerId, String answer, boolean verified) {
        ChatRepository.loadUserName(myUid, myName ->
                ChatRepository.ensureChat(itemId, itemTitle, itemImageUrl, ownerId,
                        myUid, myName, ownerId, null, new ChatRepository.ChatCallback() {
                            @Override
                            public void onReady(String chatId) {
                                ChatRepository.sendMessage(chatId, myUid, myName,
                                        "Claim request: " + answer, ChatRepository.TYPE_CLAIM,
                                        verified, new ChatRepository.SendCallback() {
                                            @Override
                                            public void onSent() {
                                                Intent intent = new Intent(ClaimActivity.this, ChatActivity.class);
                                                intent.putExtra(ChatActivity.EXTRA_CHAT_ID, chatId);
                                                intent.putExtra(ChatActivity.EXTRA_ITEM_ID, itemId);
                                                intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, ownerId);
                                                intent.putExtra(ChatActivity.EXTRA_ITEM_TITLE, itemTitle);
                                                intent.putExtra(ChatActivity.EXTRA_ITEM_IMAGE, itemImageUrl);
                                                intent.putExtra(ChatActivity.EXTRA_POST_OWNER_ID, ownerId);
                                                startActivity(intent);
                                                finish();
                                            }

                                            @Override
                                            public void onError(String message) {
                                                resetButton();
                                                Toast.makeText(ClaimActivity.this,
                                                        "Claim not sent: " + message, Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }

                            @Override
                            public void onError(String message) {
                                resetButton();
                                Toast.makeText(ClaimActivity.this,
                                        "Could not open chat: " + message, Toast.LENGTH_LONG).show();
                            }
                        }));
    }

    private void resetButton() {
        btnSubmitClaim.setEnabled(true);
        btnSubmitClaim.setText("Submit claim");
    }

    private interface VerifiedCallback {
        void onResult(boolean verified);
    }
}
