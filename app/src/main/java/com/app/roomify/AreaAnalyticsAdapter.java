package com.app.roomify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.roomify.models.AreaBookingAnalytics;

import java.util.ArrayList;
import java.util.List;

public class AreaAnalyticsAdapter extends RecyclerView.Adapter<AreaAnalyticsAdapter.ViewHolder> {

    private List<AreaBookingAnalytics> areas = new ArrayList<>();

    public AreaAnalyticsAdapter(List<AreaBookingAnalytics> areas) {
        this.areas = areas;
    }

    public void updateData(List<AreaBookingAnalytics> newAreas) {
        this.areas = newAreas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_area_analytics, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AreaBookingAnalytics area = areas.get(position);

        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvAreaName.setText(area.getArea());
        holder.tvBookingCount.setText(area.getBookingCount() + " bookings");
        holder.tvAveragePrice.setText("$" + String.format("%.2f", area.getAveragePrice()));
        holder.progressBar.setProgress((int) area.getOccupancyRate());
        holder.tvOccupancyRate.setText(String.format("%.1f%%", area.getOccupancyRate()));

        // Set medal color for top 3
        if (position == 0) {
            holder.tvRank.setBackgroundResource(R.drawable.circle_gold);
        } else if (position == 1) {
            holder.tvRank.setBackgroundResource(R.drawable.circle_silver);
        } else if (position == 2) {
            holder.tvRank.setBackgroundResource(R.drawable.circle_bronze);
        } else {
            holder.tvRank.setBackgroundResource(R.drawable.circle_gray);
        }
    }

    @Override
    public int getItemCount() {
        return areas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvAreaName, tvBookingCount, tvAveragePrice, tvOccupancyRate;
        ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvAreaName = itemView.findViewById(R.id.tvAreaName);
            tvBookingCount = itemView.findViewById(R.id.tvBookingCount);
            tvAveragePrice = itemView.findViewById(R.id.tvAveragePrice);
            tvOccupancyRate = itemView.findViewById(R.id.tvOccupancyRate);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}