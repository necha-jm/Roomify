package com.app.roomify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.MyViewHolder> {

    private List<Room> properties;
    private final OnPropertyClickListener listener;

    public interface OnPropertyClickListener {
        void onPropertyClick(Room room);
    }

    public PropertyAdapter(List<Room> properties, OnPropertyClickListener listener) {
        this.properties = properties != null ? properties : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.owner_property_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Room room = properties.get(position);

        if (room != null) {
            holder.tvPropertyTitle.setText(room.getTitle());
            holder.tvPrice.setText(room.getFormattedPrice());
            holder.tvLocation.setText(room.getLocationSummary());

            // Use the new getBookingsCount() method
            holder.tvBookingsCount.setText(room.getBookingsText()); // This will show "No bookings yet", "1 booking", or "X bookings"

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPropertyClick(room);
                }
            });
        }
    }

    private String getSafeString(String value, String defaultValue) {
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    private String getBookingsCountText(int count) {
        if (count == 0) return "No bookings yet";
        if (count == 1) return "1 booking";
        return count + " bookings";
    }

    @Override
    public int getItemCount() {
        return properties != null ? properties.size() : 0;
    }

    public void setRooms(List<Room> propertyList) {
        this.properties.clear();
        this.properties.addAll(propertyList);
        notifyDataSetChanged();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvPropertyTitle, tvLocation, tvPrice, tvBookingsCount;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPropertyTitle = itemView.findViewById(R.id.tvPropertyTitle);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvBookingsCount = itemView.findViewById(R.id.tvBookingsCount);
        }
    }
}