package com.example.findlink.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.findlink.R;
import com.example.findlink.models.ConversationModel;
import com.example.findlink.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ইনবক্সের কথোপকথন লিস্ট।
 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    public interface OnConversationClick {
        void onConversationClick(ConversationModel conversation);
    }

    private final List<ConversationModel> conversations = new ArrayList<>();
    private final OnConversationClick clickListener;

    public ConversationAdapter(OnConversationClick clickListener) {
        this.clickListener = clickListener;
    }

    public void submit(List<ConversationModel> items) {
        conversations.clear();
        if (items != null) {
            conversations.addAll(items);
        }
        notifyDataSetChanged();
    }

    public int size() {
        return conversations.size();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        ConversationModel conversation = conversations.get(position);

        holder.tvConversationAvatar.setText(conversation.getInitials());
        holder.tvConversationName.setText(conversation.getOtherUserName());
        holder.tvConversationItem.setText(conversation.getItemTitle() == null
                || conversation.getItemTitle().trim().isEmpty()
                ? "Item report" : conversation.getItemTitle());

        String preview = conversation.getLastMessage();
        holder.tvConversationPreview.setText(preview == null || preview.trim().isEmpty()
                ? "Say hello…" : preview);
        holder.tvConversationTime.setText(TimeUtils.formatInboxTime(conversation.getLastMessageAt()));
        holder.dotUnread.setVisibility(conversation.isUnread() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onConversationClick(conversation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {
        final TextView tvConversationAvatar;
        final TextView tvConversationName;
        final TextView tvConversationItem;
        final TextView tvConversationPreview;
        final TextView tvConversationTime;
        final View dotUnread;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConversationAvatar = itemView.findViewById(R.id.tvConversationAvatar);
            tvConversationName = itemView.findViewById(R.id.tvConversationName);
            tvConversationItem = itemView.findViewById(R.id.tvConversationItem);
            tvConversationPreview = itemView.findViewById(R.id.tvConversationPreview);
            tvConversationTime = itemView.findViewById(R.id.tvConversationTime);
            dotUnread = itemView.findViewById(R.id.dotUnread);
        }
    }
}
