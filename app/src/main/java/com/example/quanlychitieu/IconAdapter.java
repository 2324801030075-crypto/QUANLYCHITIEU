package com.example.quanlychitieu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {

    private List<String> iconNames;
    private OnIconClickListener listener;

    public interface OnIconClickListener {
        void onIconClick(String iconName);
    }

    public IconAdapter(List<String> iconNames, OnIconClickListener listener) {
        this.iconNames = iconNames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon, parent, false);
        return new IconViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
        String iconName = iconNames.get(position);
        Context context = holder.itemView.getContext();

        int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        if (resId != 0) {
            holder.imgIcon.setImageResource(resId);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onIconClick(iconName);
        });
    }

    @Override
    public int getItemCount() {
        return iconNames.size();
    }

    public static class IconViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        public IconViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgIconItem);
        }
    }
}
