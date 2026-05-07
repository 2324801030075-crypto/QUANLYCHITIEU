package com.example.quanlychitieu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categoryList;
    private OnCategoryActionListener listener;

    public interface OnCategoryActionListener {
        void onItemClick(Category category);
        void onItemLongClick(Category category);
    }

    public CategoryAdapter(List<Category> categoryList, OnCategoryActionListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);

        holder.tvCategoryName.setText(category.getName());
        holder.tvCategoryAmount.setText(category.getAmount());

        Context context = holder.itemView.getContext();
        String iconName = category.getIconName();

        int resId = 0;
        if (iconName != null && !iconName.isEmpty()) {
            resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        }

        if (resId != 0) {
            holder.imgCategoryIcon.setImageResource(resId);
        } else {
            holder.imgCategoryIcon.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(category);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(category);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCategoryIcon;
        TextView tvCategoryName, tvCategoryAmount;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCategoryIcon = itemView.findViewById(R.id.imgCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvCategoryAmount = itemView.findViewById(R.id.tvCategoryAmount);
        }
    }
}