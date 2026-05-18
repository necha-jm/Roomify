package com.app.roomify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder> {

    private List<Room> properties;
    private final OnPropertyClickListener listener;
    private String userRole; // "tenant", "owner", or "dalali"

    public interface OnPropertyClickListener {
        void onPropertyClick(Room room);
        void onMenuClick(Room room, View view);
    }

    public PropertyAdapter(List<Room> properties, OnPropertyClickListener listener, String userRole) {
        this.properties = properties != null ? properties : new ArrayList<>();
        this.listener = listener;
        this.userRole = userRole;
    }

    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // IMPORTANT: Use item_property_card layout, NOT owner_property_item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.owner_property_item, parent, false);
        return new PropertyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        Room room = properties.get(position);

        if (room != null) {
            // Basic info - ALL these methods exist in your Room class
            holder.tvPropertyTitle.setText(room.getTitle());
            holder.tvPrice.setText(room.getFormattedPriceMonthly());
            holder.tvLocation.setText(room.getLocationSummary());
            holder.tvBookingCount.setText(room.getBookingsText());

            // Load image
            if (room.getImages() != null && !room.getImages().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(room.getImages().get(0))
                        .placeholder(R.drawable.ic_apartment)
                        .error(R.drawable.ic_apartment)
                        .into(holder.ivPropertyImage);
            }

            // Handle role-specific UI
            if ("dalali".equalsIgnoreCase(userRole)) {
                // Show commission for dalali
                holder.llCommission.setVisibility(View.VISIBLE);
                holder.tvCommission.setText("Commission: " + room.getCommissionFormatted());

                // Show status badge
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setText(room.getStatusBadge());

                // Set status badge color
                if (room.isStatusAvailable()) {
                    holder.tvStatusBadge.setBackgroundTintList(
                            ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.success_green)
                    );
                } else if (room.isStatusPending()) {
                    holder.tvStatusBadge.setBackgroundTintList(
                            ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.warning_orange)
                    );
                } else if (room.isStatusRented()) {
                    holder.tvStatusBadge.setBackgroundTintList(
                            ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.secondary_blue)
                    );
                }

                // Show featured badge if applicable
                if (room.isFeatured()) {
                    holder.tvFeaturedBadge.setVisibility(View.VISIBLE);
                } else {
                    holder.tvFeaturedBadge.setVisibility(View.GONE);
                }
            } else {
                // Hide dalali-specific views for tenants/owners
                holder.llCommission.setVisibility(View.GONE);
                holder.tvStatusBadge.setVisibility(View.GONE);
                holder.tvFeaturedBadge.setVisibility(View.GONE);
            }

            // Click listeners
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPropertyClick(room);
                }
            });

            holder.ivMenu.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMenuClick(room, v);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return properties != null ? properties.size() : 0;
    }

    public void updateProperties(List<Room> newProperties) {
        this.properties.clear();
        this.properties.addAll(newProperties);
        notifyDataSetChanged();
    }

    static class PropertyViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPropertyImage;
        TextView tvPropertyTitle;
        TextView tvLocation;
        TextView tvPrice;
        TextView tvBookingCount;
        TextView tvCommission;
        TextView tvStatusBadge;
        TextView tvFeaturedBadge;
        ImageView ivMenu;
        LinearLayout llCommission;

        public PropertyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPropertyImage = itemView.findViewById(R.id.ivPropertyImage);
            tvPropertyTitle = itemView.findViewById(R.id.tvPropertyTitle);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvBookingCount = itemView.findViewById(R.id.tvBookingCount);
            tvCommission = itemView.findViewById(R.id.tvCommission);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvFeaturedBadge = itemView.findViewById(R.id.tvFeaturedBadge);
            ivMenu = itemView.findViewById(R.id.ivMenu);
            llCommission = itemView.findViewById(R.id.llCommission);
        }
    }
}