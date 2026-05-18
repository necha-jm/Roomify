package com.app.roomify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TopRoomsFragment extends Fragment {

    private BarChart barChart;
    private RecyclerView recyclerView;
    private TextView tvInsight;
    private TopRoomsAdapter adapter;
    private List<Map<String, Object>> topRooms = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_top_rooms, container, false);

        barChart = view.findViewById(R.id.barChart);
        recyclerView = view.findViewById(R.id.recyclerView);
        tvInsight = view.findViewById(R.id.tvInsight);

        setupChart();
        setupRecyclerView();

        return view;
    }

    private void setupChart() {
        // Configure Bar Chart
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(true);
        barChart.setDragEnabled(true);

        // Configure X Axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(45f);

        // Configure Y Axis
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "$" + String.format("%.0f", value);
            }
        });

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Configure Legend
        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);

        // Animate
        barChart.animateY(1500);
    }

    private void setupRecyclerView() {
        // Check if context is available before creating adapter
        if (getContext() != null) {
            adapter = new TopRoomsAdapter();
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
        }
    }

    public void updateData(List<Map<String, Object>> rooms) {
        // Check if fragment is attached before updating
        if (!isAdded() || getContext() == null) {
            // Store data for later if needed
            this.topRooms = rooms;
            return;
        }

        this.topRooms = rooms;
        updateBarChart();
        updateRecyclerView();
        updateInsight();
    }

    private void updateBarChart() {
        // Check if fragment is attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (topRooms == null || topRooms.isEmpty()) {
            barChart.clear();
            barChart.setNoDataText("No room data available");
            return;
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        // Take top 5 for bar chart
        int chartItems = Math.min(topRooms.size(), 5);

        for (int i = 0; i < chartItems; i++) {
            Map<String, Object> room = topRooms.get(i);
            double totalRevenue = ((Number) room.get("totalRevenue")).doubleValue();
            String title = (String) room.get("title");

            // Truncate long titles
            if (title.length() > 15) {
                title = title.substring(0, 12) + "...";
            }

            entries.add(new BarEntry(i, (float) totalRevenue));
            labels.add(title);
        }

        // FIX: Use getContext() instead of requireContext() to avoid crash
        Context context = getContext();
        int[] colors;

        if (context != null) {
            // Use context safely
            colors = new int[]{
                    ContextCompat.getColor(context, R.color.primary_color),
                    Color.parseColor("#FF9800"),
                    Color.parseColor("#9C27B0"),
                    Color.parseColor("#4CAF50"),
                    Color.parseColor("#F44336")
            };
        } else {
            // Fallback colors if context is null
            colors = new int[]{
                    Color.parseColor("#2196F3"),  // Default (blue
                    Color.parseColor("#FF9800"),
                    Color.parseColor("#9C27B0"),
                    Color.parseColor("#4CAF50"),
                    Color.parseColor("#F44336")
            };
        }

        BarDataSet dataSet = new BarDataSet(entries, "Total Revenue");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "$" + String.format("%.0f", value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        barChart.setData(barData);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.invalidate();
    }

    private void updateRecyclerView() {
        // Check if adapter exists and fragment is attached
        if (adapter != null && isAdded() && getContext() != null) {
            adapter.updateData(topRooms);
        }
    }

    private void updateInsight() {
        // Check if fragment is attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (topRooms == null || topRooms.isEmpty()) {
            tvInsight.setText("No room revenue data available yet.");
            return;
        }

        Map<String, Object> topRoom = topRooms.get(0);
        String topTitle = (String) topRoom.get("title");
        long topBookingCount = ((Number) topRoom.get("bookingCount")).longValue();
        double topRevenue = ((Number) topRoom.get("totalRevenue")).doubleValue();

        double totalRevenue = 0;
        long totalBookings = 0;
        for (Map<String, Object> room : topRooms) {
            totalRevenue += ((Number) room.get("totalRevenue")).doubleValue();
            totalBookings += ((Number) room.get("bookingCount")).longValue();
        }

        double topPercentage = totalRevenue > 0 ? (topRevenue * 100 / totalRevenue) : 0;

        StringBuilder insight = new StringBuilder();
        insight.append("🏆 TOP ROOMS INSIGHTS:\n\n");
        insight.append("✓ Top earning room: \"").append(topTitle).append("\"\n");
        insight.append("   Revenue: $").append(String.format("%,.2f", topRevenue)).append("\n");
        insight.append("   Bookings: ").append(topBookingCount).append("\n\n");

        insight.append("✓ This room contributes ").append(String.format("%.1f", topPercentage))
                .append("% of total revenue from top ").append(topRooms.size()).append(" rooms\n\n");

        if (topRooms.size() > 1) {
            Map<String, Object> secondRoom = topRooms.get(1);
            double secondRevenue = ((Number) secondRoom.get("totalRevenue")).doubleValue();
            double revenueDifference = topRevenue - secondRevenue;
            insight.append("✓ Gap between #1 and #2: $").append(String.format("%,.2f", revenueDifference));

            if (revenueDifference > 0) {
                insight.append(" (Top room outperforms by ")
                        .append(String.format("%.1f", (revenueDifference * 100 / secondRevenue)))
                        .append("%)\n\n");
            }
        }

        if (topRooms.size() >= 3) {
            Map<String, Object> thirdRoom = topRooms.get(2);
            insight.append("✓ Top 3 rooms generate $")
                    .append(String.format("%,.2f", topRevenue +
                            ((Number) topRooms.get(1).get("totalRevenue")).doubleValue() +
                            ((Number) thirdRoom.get("totalRevenue")).doubleValue()))
                    .append(" in total revenue");
        }

        tvInsight.setText(insight.toString());
    }

    // ==================== INNER ADAPTER CLASS ====================

    private class TopRoomsAdapter extends RecyclerView.Adapter<TopRoomsAdapter.ViewHolder> {

        private List<Map<String, Object>> rooms = new ArrayList<>();
        private int[] rankColors = {
                Color.parseColor("#FFD700"), // Gold
                Color.parseColor("#C0C0C0"), // Silver
                Color.parseColor("#CD7F32"), // Bronze
                Color.parseColor("#9E9E9E"), // Gray
                Color.parseColor("#9E9E9E")  // Gray
        };

        public void updateData(List<Map<String, Object>> newRooms) {
            this.rooms = newRooms;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_top_room, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> room = rooms.get(position);

            String title = (String) room.get("title");
            long bookingCount = ((Number) room.get("bookingCount")).longValue();
            double totalRevenue = ((Number) room.get("totalRevenue")).doubleValue();
            long roomId = ((Number) room.get("roomId")).longValue();

            // Set rank
            holder.tvRank.setText(String.valueOf(position + 1));

            // Set rank background
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            if (position < rankColors.length) {
                drawable.setColor(rankColors[position]);
            } else {
                drawable.setColor(rankColors[rankColors.length - 1]);
            }
            holder.tvRank.setBackground(drawable);

            // Set room info
            holder.tvRoomTitle.setText(title);
            holder.tvBookingCount.setText(bookingCount + " booking" + (bookingCount != 1 ? "s" : ""));
            holder.tvTotalRevenue.setText("$" + String.format("%,.2f", totalRevenue));

            // Set progress bar (revenue relative to top room)
            if (position == 0) {
                holder.progressBar.setProgress(100);
            } else {
                double topRevenue = ((Number) rooms.get(0).get("totalRevenue")).doubleValue();
                int percentage = topRevenue > 0 ? (int) ((totalRevenue * 100) / topRevenue) : 0;
                holder.progressBar.setProgress(percentage);
            }

            // Set medal icon for top 3
            if (position == 0) {
                holder.ivMedal.setImageResource(R.drawable.ic_medal_gold);
                holder.ivMedal.setVisibility(View.VISIBLE);
            } else if (position == 1) {
                holder.ivMedal.setImageResource(R.drawable.ic_medal_silver);
                holder.ivMedal.setVisibility(View.VISIBLE);
            } else if (position == 2) {
                holder.ivMedal.setImageResource(R.drawable.ic_medal_bronze);
                holder.ivMedal.setVisibility(View.VISIBLE);
            } else {
                holder.ivMedal.setVisibility(View.GONE);
            }

            // Set click listener to view room details
            holder.cardView.setOnClickListener(v -> {
                if (getContext() != null) {
                    Intent intent = new Intent(getContext(), RoomDetailsActivity.class);
                    intent.putExtra("room_id", roomId);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return rooms.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvRoomTitle, tvBookingCount, tvTotalRevenue;
            ProgressBar progressBar;
            ImageView ivMedal;
            CardView cardView;

            ViewHolder(View itemView) {
                super(itemView);
                tvRank = itemView.findViewById(R.id.tvRank);
                tvRoomTitle = itemView.findViewById(R.id.tvRoomTitle);
                tvBookingCount = itemView.findViewById(R.id.tvBookingCount);
                tvTotalRevenue = itemView.findViewById(R.id.tvTotalRevenue);
                progressBar = itemView.findViewById(R.id.progressBar);
                ivMedal = itemView.findViewById(R.id.ivMedal);
                cardView = itemView.findViewById(R.id.cardView);
            }
        }
    }
}