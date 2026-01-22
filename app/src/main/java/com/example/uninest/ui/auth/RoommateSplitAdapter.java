package com.example.uninest.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.model.User;

import java.util.List;

public class RoommateSplitAdapter extends RecyclerView.Adapter<RoommateSplitAdapter.ViewHolder> {

    private List<User> userList;
    private final OnRoommateSelectedListener listener;

    public interface OnRoommateSelectedListener {
        void onSelected(String email, boolean isChecked);
    }

    public RoommateSplitAdapter(List<User> userList, OnRoommateSelectedListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    public void updateList(List<User> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_roommate_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);

        // Use Email since names aren't available
        String email = user.getEmail() != null ? user.getEmail() : "Unknown Email";
        holder.tvEmail.setText(email);

        // Reset listener to null to prevent recycled view trigger bugs
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(false);

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (user.getEmail() != null) {
                listener.onSelected(user.getEmail(), isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmail;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmail = itemView.findViewById(R.id.tvRoommateEmail);
            checkBox = itemView.findViewById(R.id.cbSelectRoommate);
        }
    }
}