package com.example.findlink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.findlink.R;
import com.example.findlink.adapters.ConversationAdapter;
import com.example.findlink.models.ConversationModel;
import com.example.findlink.util.ChatRepository;
import com.example.findlink.util.WindowInsetsUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Inbox: আমার সব কথোপকথন (প্রতিটি রিপোর্ট + সংশ্লিষ্ট ইউজার) রিয়েল-টাইমে।
 */
public class InboxActivity extends AppCompatActivity {

    private RecyclerView recyclerViewInbox;
    private ConversationAdapter conversationAdapter;
    private TextView tvInboxCount;
    private View layoutEmptyInbox;

    private FirebaseAuth mAuth;
    private ListenerRegistration conversationsListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        WindowInsetsUtil.applyTopPadding(findViewById(R.id.inboxTopBar));

        Button btnInboxBack = findViewById(R.id.btnInboxBack);
        btnInboxBack.setOnClickListener(v -> finish());

        tvInboxCount = findViewById(R.id.tvInboxCount);
        layoutEmptyInbox = findViewById(R.id.layoutEmptyInbox);

        recyclerViewInbox = findViewById(R.id.recyclerViewInbox);
        recyclerViewInbox.setHasFixedSize(true);
        recyclerViewInbox.setLayoutManager(new LinearLayoutManager(this));

        conversationAdapter = new ConversationAdapter(this::openConversation);
        recyclerViewInbox.setAdapter(conversationAdapter);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            updateEmptyState();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (conversationsListener != null) {
            conversationsListener.remove();
            conversationsListener = null;
        }
    }

    private void startListening() {
        if (currentUserId == null || conversationsListener != null) {
            return;
        }
        conversationsListener = ChatRepository.listenConversations(currentUserId,
                new ChatRepository.ConversationsCallback() {
                    @Override
                    public void onConversations(List<ConversationModel> conversations) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        conversationAdapter.submit(conversations);
                        int count = conversations.size();
                        int unread = 0;
                        for (ConversationModel conversation : conversations) {
                            if (conversation.isUnread()) {
                                unread++;
                            }
                        }
                        String label = count == 0 ? ""
                                : count + (count == 1 ? " chat" : " chats");
                        if (unread > 0) {
                            label = label + " · " + unread + " new";
                        }
                        tvInboxCount.setText(label);
                        layoutEmptyInbox.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(String message) {
                        if (!isFinishing()) {
                            Toast.makeText(InboxActivity.this,
                                    "Could not load inbox: " + message, Toast.LENGTH_SHORT).show();
                        }
                        updateEmptyState();
                    }
                });
    }

    private void openConversation(ConversationModel conversation) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CHAT_ID, conversation.getChatId());
        intent.putExtra(ChatActivity.EXTRA_ITEM_ID, conversation.getItemId());
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, conversation.getOtherUserId());
        intent.putExtra(ChatActivity.EXTRA_ITEM_TITLE, conversation.getItemTitle());
        intent.putExtra(ChatActivity.EXTRA_ITEM_IMAGE, conversation.getItemImageUrl());
        intent.putExtra(ChatActivity.EXTRA_POST_OWNER_ID, conversation.getPostOwnerId());
        startActivity(intent);
    }

    private void updateEmptyState() {
        conversationAdapter.submit(null);
        tvInboxCount.setText("");
        layoutEmptyInbox.setVisibility(View.VISIBLE);
    }
}
