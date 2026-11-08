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
import com.example.findlink.models.InboxModel;
import java.util.List;

public class InboxAdapter extends RecyclerView.Adapter<InboxAdapter.InboxViewHolder> {
    private List<InboxModel> list;

    public InboxAdapter(List<InboxModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public InboxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inbox, parent, false);
        return new InboxViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InboxViewHolder holder, int position) {
        InboxModel item = list.get(position);
        holder.tvInboxType.setText(item.getType());
        holder.tvInboxContent.setText(item.getContent());
        holder.tvInboxStatus.setText(item.getStatus());

        boolean isClaim = "CLAIM".equalsIgnoreCase(item.getType());
        Context context = holder.itemView.getContext();
        GradientDrawable chip = new GradientDrawable();
        chip.setCornerRadius(8 * context.getResources().getDisplayMetrics().density);
        chip.setColor(context.getColor(isClaim ? R.color.colorSuccessSoft : R.color.colorPrimaryLight));
        holder.tvInboxType.setBackground(chip);
        holder.tvInboxType.setTextColor(context.getColor(isClaim ? R.color.colorSuccess : R.color.colorPrimary));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class InboxViewHolder extends RecyclerView.ViewHolder {
        TextView tvInboxType, tvInboxContent, tvInboxStatus;

        public InboxViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInboxType = itemView.findViewById(R.id.tvInboxType);
            tvInboxContent = itemView.findViewById(R.id.tvInboxContent);
            tvInboxStatus = itemView.findViewById(R.id.tvInboxStatus);
        }
    }
}