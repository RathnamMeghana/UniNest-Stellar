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

    private List<User> users;
    private RoommateCheckListener listener;

    public interface RoommateCheckListener {
        void onChecked(String userId, boolean isChecked); // pass user ID, not email
    }

    public RoommateSplitAdapter(List<User> users, RoommateCheckListener listener) {
        this.users = users;
        this.listener = listener;
    }

    public void updateList(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_roommate_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.checkBox.setText(user.getEmail()); // display email
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(false);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onChecked(user.getId(), isChecked); // pass real user ID
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cbRoommate);
        }
    }
}
