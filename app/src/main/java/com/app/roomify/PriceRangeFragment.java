package com.app.roomify;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.app.roomify.models.PriceRangeAnalytics;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class PriceRangeFragment extends Fragment {

    private BarChart barChart;
    private PieChart pieChart;
    private TextView tvInsight;
    private List<PriceRangeAnalytics> priceRanges = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_price_range, container, false);

        barChart = view.findViewById(R.id.barChart);
        pieChart = view.findViewById(R.id.pieChart);
        tvInsight = view.findViewById(R.id.tvInsight);

        setupCharts();

        return view;
    }

    private void setupCharts() {
        // Setup Bar Chart
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setPinchZoom(false);

        // Setup X Axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        // Setup Y Axis
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisRight().setEnabled(false);

        // Setup Legend
        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);

        // Setup Pie Chart
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(android.R.color.transparent);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setUsePercentValues(true);

        Legend pieLegend = pieChart.getLegend();
        pieLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        pieLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieLegend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
    }

    public void updateData(List<PriceRangeAnalytics> data) {
        this.priceRanges = data;
        updateBarChart();
        updatePieChart();
        updateInsight();
    }

    private void updateBarChart() {
        if (priceRanges.isEmpty()) return;

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < priceRanges.size(); i++) {
            PriceRangeAnalytics pr = priceRanges.get(i);
            entries.add(new BarEntry(i, pr.getBookingCount()));
            labels.add(pr.getPriceRange());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Number of Bookings");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        barChart.setData(barData);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setLabelRotationAngle(45f);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void updatePieChart() {
        if (priceRanges.isEmpty()) return;

        ArrayList<PieEntry> entries = new ArrayList<>();
        int totalBookings = 0;

        for (PriceRangeAnalytics pr : priceRanges) {
            entries.add(new PieEntry(pr.getBookingCount(), pr.getPriceRange()));
            totalBookings += pr.getBookingCount();
        }

        PieDataSet dataSet = new PieDataSet(entries, "Booking Distribution");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void updateInsight() {
        if (priceRanges.isEmpty()) {
            tvInsight.setText("No booking data available yet.");
            return;
        }

        PriceRangeAnalytics mostBooked = priceRanges.get(0);
        StringBuilder insight = new StringBuilder();
        insight.append("📊 KEY INSIGHTS:\n\n");
        insight.append("✓ Most booked price range: ").append(mostBooked.getPriceRange())
                .append(" (").append(mostBooked.getBookingCount()).append(" bookings)\n\n");
        insight.append("✓ Average price in top range: $")
                .append(String.format("%.2f", mostBooked.getAveragePrice()));

        if (priceRanges.size() > 1) {
            PriceRangeAnalytics second = priceRanges.get(1);
            insight.append("\n\n✓ Second most popular: ").append(second.getPriceRange())
                    .append(" (").append(second.getBookingCount()).append(" bookings)");
        }

        tvInsight.setText(insight.toString());
    }
}