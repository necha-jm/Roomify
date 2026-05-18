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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MonthlyTrendsFragment extends Fragment {

    private LineChart lineChart;
    private TextView tvInsight;
    private List<Map<String, Object>> monthlyTrends = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_monthly_trends, container, false);

        lineChart = view.findViewById(R.id.lineChart);
        tvInsight = view.findViewById(R.id.tvInsight);

        setupChart();

        return view;
    }

    private void setupChart() {
        // Check if fragment is attached before configuring chart
        if (!isAdded() || getContext() == null) {
            return;
        }

        // Configure Line Chart
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        // Configure X Axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#E0E0E0"));

        // Configure Left Y Axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setAxisMinimum(0f);

        // Configure Right Y Axis
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Configure Legend
        Legend legend = lineChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);

        // Animate
        lineChart.animateX(1500);
        lineChart.animateY(1500);
    }

    public void updateData(List<Map<String, Object>> trends) {
        // FIX: Check if fragment is attached before updating
        if (!isAdded() || getContext() == null) {
            // Store data for later if needed
            this.monthlyTrends = trends;
            return;
        }

        this.monthlyTrends = trends;
        updateLineChart();
        updateInsight();
    }

    private void updateLineChart() {
        // FIX: Check if fragment is attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (monthlyTrends == null || monthlyTrends.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("No monthly booking data available");
            return;
        }

        // Prepare data for the chart
        ArrayList<Entry> bookingEntries = new ArrayList<>();
        ArrayList<Entry> revenueEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        // Reverse to show chronological order (oldest to newest)
        List<Map<String, Object>> reversedTrends = new ArrayList<>(monthlyTrends);
        java.util.Collections.reverse(reversedTrends);

        for (int i = 0; i < reversedTrends.size(); i++) {
            Map<String, Object> trend = reversedTrends.get(i);

            long bookingCount = ((Number) trend.get("bookingCount")).longValue();
            double totalRevenue = ((Number) trend.get("totalRevenue")).doubleValue() / 1000; // Convert to thousands
            String month = (String) trend.get("month");

            bookingEntries.add(new Entry(i, bookingCount));
            revenueEntries.add(new Entry(i, (float) totalRevenue));
            labels.add(formatMonthLabel(month));
        }

        // Create Booking Count DataSet
        LineDataSet bookingDataSet = new LineDataSet(bookingEntries, "Bookings");

        // FIX: Safe context usage
        if (getContext() != null) {
            bookingDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary_color));
            bookingDataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.primary_color));
            bookingDataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.primary_color));
        } else {
            // Fallback colors if context is not available
            bookingDataSet.setColor(Color.parseColor("#2196F3"));
            bookingDataSet.setCircleColor(Color.parseColor("#2196F3"));
            bookingDataSet.setFillColor(Color.parseColor("#2196F3"));
        }

        bookingDataSet.setLineWidth(2f);
        bookingDataSet.setCircleRadius(4f);
        bookingDataSet.setFillAlpha(50);
        bookingDataSet.setDrawFilled(true);
        bookingDataSet.setValueTextSize(10f);
        bookingDataSet.setValueTextColor(Color.BLACK);

        // Create Revenue DataSet
        LineDataSet revenueDataSet = new LineDataSet(revenueEntries, "Revenue (K$)");
        revenueDataSet.setColor(Color.parseColor("#FF9800"));
        revenueDataSet.setLineWidth(2f);
        revenueDataSet.setCircleColor(Color.parseColor("#FF9800"));
        revenueDataSet.setCircleRadius(4f);
        revenueDataSet.setDrawFilled(false);
        revenueDataSet.setValueTextSize(10f);
        revenueDataSet.setValueTextColor(Color.BLACK);

        // Combine DataSets
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(bookingDataSet);
        dataSets.add(revenueDataSet);

        LineData lineData = new LineData(dataSets);
        lineChart.setData(lineData);

        // Set X Axis Labels
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(45f);

        lineChart.invalidate();
    }

    private String formatMonthLabel(String yearMonth) {
        if (yearMonth == null) return "";
        // yearMonth format: "2024-01"
        try {
            String[] parts = yearMonth.split("-");
            if (parts.length == 2) {
                int month = Integer.parseInt(parts[1]);
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                return monthNames[month - 1] + " '" + parts[0].substring(2);
            }
        } catch (Exception e) {
            return yearMonth;
        }
        return yearMonth;
    }

    private void updateInsight() {
        // FIX: Check if fragment is attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (monthlyTrends == null || monthlyTrends.isEmpty()) {
            tvInsight.setText("No monthly booking data available yet.");
            return;
        }

        // Find peak month
        Map<String, Object> peakMonth = null;
        Map<String, Object> bestRevenueMonth = null;
        long maxBookings = 0;
        double maxRevenue = 0;
        long totalBookings = 0;
        double totalRevenue = 0;

        for (Map<String, Object> trend : monthlyTrends) {
            long bookingCount = ((Number) trend.get("bookingCount")).longValue();
            double revenue = ((Number) trend.get("totalRevenue")).doubleValue();
            totalBookings += bookingCount;
            totalRevenue += revenue;

            if (bookingCount > maxBookings) {
                maxBookings = bookingCount;
                peakMonth = trend;
            }
            if (revenue > maxRevenue) {
                maxRevenue = revenue;
                bestRevenueMonth = trend;
            }
        }

        double averageBookings = monthlyTrends.size() > 0 ? (double) totalBookings / monthlyTrends.size() : 0;
        double averageRevenue = monthlyTrends.size() > 0 ? totalRevenue / monthlyTrends.size() : 0;

        StringBuilder insight = new StringBuilder();
        insight.append("📈 MONTHLY TREND INSIGHTS:\n\n");

        if (peakMonth != null) {
            insight.append("✓ Peak booking month: ").append(peakMonth.get("month"))
                    .append(" (").append(maxBookings).append(" bookings)\n\n");
        }

        if (bestRevenueMonth != null) {
            insight.append("✓ Highest revenue month: ").append(bestRevenueMonth.get("month"))
                    .append(" ($").append(String.format("%,.2f", maxRevenue)).append(")\n\n");
        }

        insight.append("✓ Monthly average: ").append(String.format("%.1f", averageBookings))
                .append(" bookings | $").append(String.format("%,.2f", averageRevenue)).append("\n\n");

        // Calculate trend direction (compare last 3 months with previous 3 months)
        if (monthlyTrends.size() >= 6) {
            long recentTotal = 0;
            long previousTotal = 0;
            int size = monthlyTrends.size();

            for (int i = size - 3; i < size; i++) {
                recentTotal += ((Number) monthlyTrends.get(i).get("bookingCount")).longValue();
            }
            for (int i = size - 6; i < size - 3; i++) {
                previousTotal += ((Number) monthlyTrends.get(i).get("bookingCount")).longValue();
            }

            if (recentTotal > previousTotal) {
                double increase = ((recentTotal - previousTotal) * 100.0 / previousTotal);
                insight.append("✓ Trend: 📈 Increasing (").append(String.format("%.1f", increase))
                        .append("% growth in last 3 months)");
            } else if (recentTotal < previousTotal) {
                double decrease = ((previousTotal - recentTotal) * 100.0 / previousTotal);
                insight.append("✓ Trend: 📉 Decreasing (").append(String.format("%.1f", decrease))
                        .append("% decline in last 3 months)");
            } else {
                insight.append("✓ Trend: ➡ Stable (no significant change)");
            }
        } else if (monthlyTrends.size() >= 2) {
            // Simple comparison between last two months
            long lastMonth = ((Number) monthlyTrends.get(monthlyTrends.size() - 1).get("bookingCount")).longValue();
            long previousMonth = ((Number) monthlyTrends.get(monthlyTrends.size() - 2).get("bookingCount")).longValue();

            if (lastMonth > previousMonth) {
                insight.append("✓ Last month: ↑ ").append(lastMonth - previousMonth)
                        .append(" more bookings than previous month");
            } else if (lastMonth < previousMonth) {
                insight.append("✓ Last month: ↓ ").append(previousMonth - lastMonth)
                        .append(" fewer bookings than previous month");
            } else {
                insight.append("✓ Last month: Same as previous month");
            }
        }

        tvInsight.setText(insight.toString());
    }
}