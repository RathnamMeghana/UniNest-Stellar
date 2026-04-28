package com.example.uninest.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.model.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter used by Letting Agents to select multiple tenants
 * for targeted push notifications.
 */
public class TenantSelectionAdapter extends RecyclerView.Adapter<TenantSelectionAdapter.ViewHolder> {

    private List<User> tenants = new ArrayList<>();
    private final Set<String> selectedUserIds = new HashSet<>();

    public void setTenants(List<User> tenants) {
        this.tenants = tenants;
        this.selectedUserIds.clear(); // Reset selection when apartment changes
        notifyDataSetChanged();
    }

    public List<String> getSelectedUserIds() {
        return new ArrayList<>(selectedUserIds);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Using Android's built-in multiple choice layout which includes a CheckBox/Checkmark
        View v = LayoutInflater.from(parent.getContext()).inflate(
                android.R.layout.simple_list_item_multiple_choice,
                parent,
                false
        );
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = tenants.get(position);
        CheckedTextView checkedView = (CheckedTextView) holder.itemView;

        // Use getFullName or fallback to email if name is empty
        String displayName = (user.getFirstName() != null) ? user.getFullName() : user.getEmail();
        checkedView.setText(displayName);

        // Sync visual state with the selection set
        checkedView.setChecked(selectedUserIds.contains(user.getId()));

        holder.itemView.setOnClickListener(v -> {
            String userId = user.getId();
            if (selectedUserIds.contains(userId)) {
                selectedUserIds.remove(userId);
            } else {
                selectedUserIds.add(userId);
            }
            // Update only the visual state of the clicked item
            checkedView.setChecked(selectedUserIds.contains(userId));
        });
    }

    @Override
    public int getItemCount() {
        return tenants.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}