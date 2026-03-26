package com.app.roomify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AmenitiesAdapter extends RecyclerView.Adapter<AmenitiesAdapter.AmenityViewHolder> {

    private List<String> amenities;

    public AmenitiesAdapter(List<String> amenities) {
        this.amenities = amenities;
    }

    @NonNull
    @Override
    public AmenityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_amenity, parent, false);
        return new AmenityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AmenityViewHolder holder, int position) {
        String amenity = amenities.get(position);
        holder.tvAmenity.setText(amenity);

        // Set appropriate icon based on amenity type
        setAmenityIcon(holder.ivAmenityIcon, amenity);
    }

    private void setAmenityIcon(ImageView imageView, String amenity) {
        // Map amenity string to icon
        if (amenity.toLowerCase().contains("wifi")) {
            imageView.setImageResource(R.drawable.ic_location);
        } else if (amenity.toLowerCase().contains("parking")) {
            imageView.setImageResource(R.drawable.ic_bed);
        } else if (amenity.toLowerCase().contains("ac") || amenity.toLowerCase().contains("air")) {
            imageView.setImageResource(R.drawable.ic_location);
        } else {
            imageView.setImageResource(R.drawable.ic_bed);
        }
    }

    @Override
    public int getItemCount() {

        return amenities != null ? amenities.size() : 0;
    }

    static class AmenityViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAmenityIcon;
        TextView tvAmenity;

        AmenityViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAmenityIcon = itemView.findViewById(R.id.ivAmenityIcon);
            tvAmenity = itemView.findViewById(R.id.tvAmenity);
        }
    }
}