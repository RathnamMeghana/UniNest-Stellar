package com.example.uninest.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.uninest.R;
import com.example.uninest.model.User;

import java.util.ArrayList;
import java.util.List;

public class BillSplittingAdapter extends RecyclerView.Adapter<BillSplittingAdapter.ViewHolder> {
    private List<User> users;
    private String currentUserId;
    private List<String> selectedIds = new ArrayList<>();
    private OnCheckListener listener;
    public interface OnCheckListener { void onChecked(String id, boolean check); }

    public BillSplittingAdapter(List<User> users, String currentUserId, OnCheckListener listener) {
        this.users = users;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updateList(List<User> users) { this.users = users; notifyDataSetChanged(); }

    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_split_roommate_select, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);

        if (holder.checkBox != null) {
            String displayName = user.getFullName();
            if (user.getId() != null && user.getId().equals(currentUserId)) {
                displayName = "Me";
            }
            holder.checkBox.setText(displayName);

            // 1. Remove listener before changing state
            holder.checkBox.setOnCheckedChangeListener(null);

            // 2. Set the state based on our saved list
            holder.checkBox.setChecked(selectedIds.contains(user.getId()));

            // 3. Re-add listener
            holder.checkBox.setOnCheckedChangeListener((view, isChecked) -> {
                if (isChecked) {
                    if (!selectedIds.contains(user.getId())) selectedIds.add(user.getId());
                } else {
                    selectedIds.remove(user.getId());
                }
                listener.onChecked(user.getId(), isChecked);
            });
        }
    }
    @Override public int getItemCount() { return users != null ? users.size() : 0; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cbRoommate);
        }
    }
}