package com.example.findlink.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.findlink.R;
import com.example.findlink.models.ChatMessage;
import com.example.findlink.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * চ্যাট বাবল: নিজের মেসেজ ডানে (navy), অন্যদের মেসেজ বাঁয়ে (সাদা)।
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String currentUserId;
    private long otherReadAt;

    public ChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    /**
     * অন্য পক্ষ কত সময় পর্যন্ত পড়েছে — শেষ মেসেজের নিচে "Seen" দেখানোর জন্য।
     */
    public void setOtherReadAt(long readAt) {
        if (this.otherReadAt == readAt) {
            return;
        }
        this.otherReadAt = readAt;
        notifyDataSetChanged();
    }

    public void submit(List<ChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    public int size() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return currentUserId != null && currentUserId.equals(message.getSenderId())
                ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_SENT
                ? R.layout.item_message_sent
                : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.tvMessageText.setText(message.getText());

        String time = TimeUtils.formatChatTime(message.getCreatedAt());
        boolean isMine = currentUserId != null && currentUserId.equals(message.getSenderId());
        boolean isLast = position == messages.size() - 1;
        if (isMine && isLast && message.getCreatedAt() > 0 && otherReadAt >= message.getCreatedAt()) {
            time = time + " · Seen";
        }
        holder.tvMessageTime.setText(time);

        bindClaimTag(holder.tvClaimTag, message);
    }

    /**
     * সিকিউরিটি কোডে মালিকানা প্রমাণিত হলে সবুজ "VERIFIED OWNER" ব্যাজ,
     * না হলে হলুদ "CLAIM • UNVERIFIED" — যিনি জিনিসটি পেয়েছেন তিনি বুঝতে পারবেন।
     */
    private void bindClaimTag(TextView tag, ChatMessage message) {
        if (!message.isClaim()) {
            tag.setVisibility(View.GONE);
            return;
        }
        tag.setVisibility(View.VISIBLE);

        boolean verified = message.isVerified();
        Context context = tag.getContext();
        tag.setText(verified ? "VERIFIED OWNER" : "CLAIM REQUEST");
        tag.setTextColor(context.getColor(verified ? R.color.colorSuccess : R.color.colorWarning));

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(8 * context.getResources().getDisplayMetrics().density);
        background.setColor(context.getColor(verified ? R.color.colorSuccessSoft : R.color.colorWarningSoft));
        tag.setBackground(background);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessageText;
        final TextView tvMessageTime;
        final TextView tvClaimTag;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            tvClaimTag = itemView.findViewById(R.id.tvClaimTag);
        }
    }
}
