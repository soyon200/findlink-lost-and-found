package com.example.findlink.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.findlink.R;
import com.example.findlink.activities.ItemDetailsActivity;
import com.example.findlink.models.ItemModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private List<ItemModel> itemList;
    private Context context;

    public ItemAdapter(List<ItemModel> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ItemModel item = itemList.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvCategory.setText(item.getCategory());
        holder.tvLocation.setText(item.getLocation());
        holder.tvReportType.setText(item.getReportType());

        // Timestamp থেকে Date ও Time ফরমেট করা
        if (item.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault());
            String dateTime = sdf.format(new Date(item.getTimestamp()));
            holder.tvDateTime.setText(dateTime);
        } else {
            holder.tvDateTime.setText("Just now");
        }

        // ছবি থাকলে Glide দিয়ে লোড, না থাকলে পুরো ছবির অংশ লুকানো
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            holder.cardImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(item.getImageUrl())
                    .centerCrop()
                    .placeholder(R.color.colorPrimaryLight)
                    .into(holder.ivItemImage);
        } else {
            holder.cardImage.setVisibility(View.GONE);
        }

        boolean isLost = "LOST".equalsIgnoreCase(item.getReportType());
        GradientDrawable chip = new GradientDrawable();
        chip.setCornerRadius(8 * context.getResources().getDisplayMetrics().density);
        chip.setColor(context.getColor(isLost ? R.color.colorWarningSoft : R.color.colorSuccessSoft));
        holder.tvReportType.setBackground(chip);
        holder.tvReportType.setTextColor(context.getColor(isLost ? R.color.colorWarning : R.color.colorSuccess));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ItemDetailsActivity.class);
            intent.putExtra("title", item.getTitle());
            intent.putExtra("category", item.getCategory());
            intent.putExtra("location", item.getLocation());
            intent.putExtra("description", item.getDescription());
            intent.putExtra("reportType", item.getReportType());
            intent.putExtra("itemId", item.getItemId());
            intent.putExtra("userId", item.getUserId());
            intent.putExtra("phone", item.getPhone());
            intent.putExtra("imageUrl", item.getImageUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvLocation, tvReportType, tvDateTime;
        ImageView ivItemImage;
        View cardImage;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            cardImage = itemView.findViewById(R.id.cardItemImage);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            tvLocation = itemView.findViewById(R.id.tvItemLocation);
            tvReportType = itemView.findViewById(R.id.tvReportType);
            tvDateTime = itemView.findViewById(R.id.tvDateTime); // নতুন ফিল্ড
        }
    }
}

