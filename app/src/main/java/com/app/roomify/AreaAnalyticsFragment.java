package com.app.roomify;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.app.roomify.models.AreaBookingAnalytics;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AreaAnalyticsFragment extends Fragment {

    private HorizontalBarChart horizontalBarChart;
    private BarChart occupancyChart;
    private RecyclerView recyclerView;
    private TextView tvInsight;
    private AreaAnalyticsAdapter adapter;
    private List<AreaBookingAnalytics> areas = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_area_analytics, container, false);

        horizontalBarChart = view.findViewById(R.id.horizontalBarChart);
        occupancyChart = view.findViewById(R.id.occupancyChart);
        recyclerView = view.findViewById(R.id.recyclerView);
        tvInsight = view.findViewById(R.id.tvInsight);

        setupCharts();
        setupRecyclerView();

        return view;
    }

    private void setupCharts() {
        // Setup Horizontal Bar Chart (Top Booked Areas)
        horizontalBarChart.getDescription().setEnabled(false);
        horizontalBarChart.setDrawGridBackground(false);
        horizontalBarChart.setDrawBarShadow(false);
        horizontalBarChart.setPinchZoom(true);
        horizontalBarChart.setScaleEnabled(true);
        horizontalBarChart.setDragEnabled(true);

        // X Axis for Horizontal Bar Chart
        XAxis xAxis = horizontalBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(45f);

        // Y Axis
        YAxis leftAxis = horizontalBarChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = horizontalBarChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Legend
        Legend legend = horizontalBarChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);

        // Setup Occupancy Chart
        occupancyChart.getDescription().setEnabled(false);
        occupancyChart.setDrawGridBackground(false);
        occupancyChart.setDrawBarShadow(false);
        occupancyChart.setPinchZoom(true);
        occupancyChart.setScaleEnabled(true);

        XAxis occXAxis = occupancyChart.getXAxis();
        occXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        occXAxis.setGranularity(1f);
        occXAxis.setDrawGridLines(false);
        occXAxis.setLabelRotationAngle(45f);

        YAxis occLeftAxis = occupancyChart.getAxisLeft();
        occLeftAxis.setDrawGridLines(true);
        occLeftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        occLeftAxis.setAxisMinimum(0f);
        occLeftAxis.setAxisMaximum(100f);
        occLeftAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f%%", value);
            }
        });

        occupancyChart.getAxisRight().setEnabled(false);

        // Animate charts
        horizontalBarChart.animateY(1500);
        occupancyChart.animateY(1500);
    }

    private void setupRecyclerView() {
        adapter = new AreaAnalyticsAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    public void updateData(List<AreaBookingAnalytics> data) {
        this.areas = data;
        updateHorizontalBarChart();
        updateOccupancyChart();
        updateRecyclerView();
        updateInsight();
    }

    private void updateHorizontalBarChart() {
        if (areas == null || areas.isEmpty()) {
            horizontalBarChart.clear();
            horizontalBarChart.setNoDataText("No area data available");
            return;
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int chartItems = Math.min(areas.size(), 10);

        for (int i = 0; i < chartItems; i++) {
            AreaBookingAnalytics area = areas.get(i);
            entries.add(new BarEntry(i, area.getBookingCount()));
            String areaName = area.getArea();
            if (areaName.length() > 20) {
                areaName = areaName.substring(0, 17) + "...";
            }
            labels.add(areaName);
        }

        // Reverse for horizontal bar chart (highest at top)
        java.util.Collections.reverse(entries);
        java.util.Collections.reverse(labels);

        BarDataSet dataSet = new BarDataSet(entries, "Number of Bookings");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        horizontalBarChart.setData(barData);
        horizontalBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        horizontalBarChart.invalidate();
    }

    private void updateOccupancyChart() {
        if (areas == null || areas.isEmpty()) {
            occupancyChart.clear();
            occupancyChart.setNoDataText("No occupancy data available");
            return;
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int chartItems = Math.min(areas.size(), 10);

        for (int i = 0; i < chartItems; i++) {
            AreaBookingAnalytics area = areas.get(i);
            entries.add(new BarEntry(i, (float) area.getOccupancyRate()));
            String areaName = area.getArea();
            if (areaName.length() > 20) {
                areaName = areaName.substring(0, 17) + "...";
            }
            labels.add(areaName);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Occupancy Rate (%)");
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.1f%%", value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        occupancyChart.setData(barData);
        occupancyChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        occupancyChart.invalidate();
    }

    private void updateRecyclerView() {
        adapter.updateData(areas);
    }

    private void updateInsight() {
        if (areas == null || areas.isEmpty()) {
            tvInsight.setText("No area booking data available yet.");
            return;
        }

        AreaBookingAnalytics topArea = areas.get(0);

        double totalBookings = 0;
        double totalOccupancy = 0;
        for (AreaBookingAnalytics area : areas) {
            totalBookings += area.getBookingCount();
            totalOccupancy += area.getOccupancyRate();
        }
        double avgOccupancy = areas.size() > 0 ? totalOccupancy / areas.size() : 0;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(1);

        StringBuilder insight = new StringBuilder();
        insight.append("📍 AREA INSIGHTS:\n\n");

        insight.append("✓ Most booked area: ").append(topArea.getArea()).append("\n");
        insight.append("   • ").append(topArea.getBookingCount()).append(" bookings\n");
        insight.append("   • Average price: ").append(currencyFormat.format(topArea.getAveragePrice())).append("\n");
        insight.append("   • Occupancy: ").append(String.format("%.1f", topArea.getOccupancyRate())).append("%\n\n");

        if (areas.size() > 1) {
            AreaBookingAnalytics secondArea = areas.get(1);
            double ratio = topArea.getBookingCount() > 0 ?
                    (double) secondArea.getBookingCount() / topArea.getBookingCount() * 100 : 0;
            insight.append("✓ Top area has ").append(String.format("%.0f", 100 - ratio))
                    .append("% more bookings than #").append(areas.size() > 2 ? "2" : "next").append("\n\n");
        }

        insight.append("✓ Average occupancy across areas: ").append(String.format("%.1f", avgOccupancy)).append("%\n\n");

        // Find area with best value (lowest price but high occupancy)
        AreaBookingAnalytics bestValueArea = null;
        double bestValueScore = 0;
        for (AreaBookingAnalytics area : areas) {
            double valueScore = area.getOccupancyRate() / area.getAveragePrice();
            if (valueScore > bestValueScore) {
                bestValueScore = valueScore;
                bestValueArea = area;
            }
        }

        if (bestValueArea != null && !bestValueArea.getArea().equals(topArea.getArea())) {
            insight.append("✓ Best value area: ").append(bestValueArea.getArea()).append("\n");
            insight.append("   • ").append(String.format("%.1f", bestValueArea.getOccupancyRate()))
                    .append("% occupancy at ").append(currencyFormat.format(bestValueArea.getAveragePrice()));
        }

        tvInsight.setText(insight.toString());
    }

    // ==================== INNER ADAPTER CLASS ====================

    private class AreaAnalyticsAdapter extends RecyclerView.Adapter<AreaAnalyticsAdapter.ViewHolder> {

        private List<AreaBookingAnalytics> areas = new ArrayList<>();
        private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "US"));

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

            // Set medal colors for top 3
            if (position == 0) {
                holder.tvRank.setBackgroundResource(R.drawable.circle_gold);
            } else if (position == 1) {
                holder.tvRank.setBackgroundResource(R.drawable.circle_silver);
            } else if (position == 2) {
                holder.tvRank.setBackgroundResource(R.drawable.circle_bronze);
            } else {
                holder.tvRank.setBackgroundResource(R.drawable.circle_gray);
            }

            holder.tvAreaName.setText(area.getArea());
            holder.tvBookingCount.setText(area.getBookingCount() + " booking" + (area.getBookingCount() != 1 ? "s" : ""));
            holder.tvAveragePrice.setText(currencyFormat.format(area.getAveragePrice()));
            holder.progressBar.setProgress((int) area.getOccupancyRate());
            holder.tvOccupancyRate.setText(String.format("%.1f%%", area.getOccupancyRate()));

            // Set color for progress bar
            if (area.getOccupancyRate() >= 70) {
                holder.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            } else if (area.getOccupancyRate() >= 40) {
                holder.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFC107")));
            } else {
                holder.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
            }
        }

        @Override
        public int getItemCount() {
            return areas.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
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
}