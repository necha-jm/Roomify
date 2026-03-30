package com.app.roomify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

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
        this.properties = properties;
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

        if (room != null) {
            // Set basic info
            holder.tvTitle.setText(room.getTitle());
            holder.tvPrice.setText(room.getFormattedPrice());
            holder.tvLocation.setText(room.getLocationSummary());
            holder.tvBookingsCount.setText(room.getBookingsText());

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

            // Set placeholder image if no image
            if (room.hasImages()) {
                // Use Glide or Picasso to load image
                // Glide.with(holder.itemView.getContext()).load(room.getFirstImageUrl()).into(holder.ivPropertyImage);
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
    }

    @Override
    public int getItemCount() {
        return properties != null ? properties.size() : 0;
    }

    public void updateList(List<Room> newList) {
        this.properties = newList;
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