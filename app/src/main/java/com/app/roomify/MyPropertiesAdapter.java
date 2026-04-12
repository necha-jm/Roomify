package com.app.roomify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MyPropertiesAdapter extends RecyclerView.Adapter<MyPropertiesAdapter.PropertyViewHolder> {

    private List<Room> properties;
    private final OnPropertyClickListener clickListener;
    private final OnPropertyDeleteListener deleteListener;

    public interface OnPropertyClickListener {
        void onPropertyClick(Room room);
    }

    public interface OnPropertyDeleteListener {
        void onPropertyDelete(Room room);
    }

    public MyPropertiesAdapter(List<Room> properties,
                               OnPropertyClickListener clickListener,
                               OnPropertyDeleteListener deleteListener) {
        this.properties = properties != null ? properties : List.of();
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_property, parent, false);
        return new PropertyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        Room room = properties.get(position);

        if (room == null) return;

        // Set basic info with null safety
        String title = room.getTitle();
        holder.tvTitle.setText(title != null ? title : "Untitled Property");

        // Safe price formatting
        String price = room.getFormattedPrice();
        holder.tvPrice.setText(price != null ? price : "$0");

        // Safe location - THIS FIXES YOUR ERROR
        String location = room.getLocationSummary();
        holder.tvLocation.setText(location != null ? location : "Location not specified");

        // Set availability badge
        if (room.isAvailable()) {
            holder.tvAvailability.setText("Available");
            holder.tvAvailability.setTextColor(holder.itemView.getContext().getColor(R.color.green_success));
            holder.ivAvailability.setImageResource(R.drawable.ic_check_circle);
            holder.ivAvailability.setColorFilter(holder.itemView.getContext().getColor(R.color.green_success));
        } else {
            holder.tvAvailability.setText("Not Available");
            holder.tvAvailability.setTextColor(holder.itemView.getContext().getColor(R.color.red_error));
            holder.ivAvailability.setImageResource(R.drawable.ic_close);
            holder.ivAvailability.setColorFilter(holder.itemView.getContext().getColor(R.color.red_error));
        }

        // Set bookings count (if available)
        if (holder.tvBookingsCount != null) {
            int bookingsCount = room.getBookingsCount();
            holder.tvBookingsCount.setText(bookingsCount + " booking" + (bookingsCount != 1 ? "s" : ""));
        }

        // Load image safely
        if (room.hasImages()) {
            String imageUrl = room.getFirstImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    Glide.with(holder.itemView.getContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_room_placeholder)
                            .error(R.drawable.ic_room_placeholder)
                            .into(holder.ivPropertyImage);
                } catch (Exception e) {
                    holder.ivPropertyImage.setImageResource(R.drawable.ic_room_placeholder);
                }
            } else {
                holder.ivPropertyImage.setImageResource(R.drawable.ic_room_placeholder);
            }
        } else {
            holder.ivPropertyImage.setImageResource(R.drawable.ic_room_placeholder);
        }

        // Set click listeners
        holder.cardView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPropertyClick(room);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onPropertyDelete(room);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            // Navigate to edit activity
            // Intent intent = new Intent(holder.itemView.getContext(), EditRoomActivity.class);
            // intent.putExtra("room_id", room.getId());
            // holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return properties != null ? properties.size() : 0;
    }

    public void updateList(List<Room> newList) {
        this.properties = newList != null ? newList : List.of();
        notifyDataSetChanged();
    }

    static class PropertyViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivPropertyImage, ivAvailability, btnDelete, btnEdit;
        TextView tvTitle, tvPrice, tvLocation, tvBookingsCount, tvAvailability;

        public PropertyViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivPropertyImage = itemView.findViewById(R.id.ivPropertyImage);
            ivAvailability = itemView.findViewById(R.id.ivAvailability);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvBookingsCount = itemView.findViewById(R.id.tvBookingsCount);
            tvAvailability = itemView.findViewById(R.id.tvAvailability);
        }
    }
}