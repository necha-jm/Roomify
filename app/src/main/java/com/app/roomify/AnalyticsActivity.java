package com.app.roomify;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.app.roomify.models.AreaBookingAnalytics;
import com.app.roomify.models.PriceRangeAnalytics;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyticsActivity extends AppCompatActivity {

    private static final String TAG = "AnalyticsActivity";

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private AnalyticsPagerAdapter pagerAdapter;
    private APIInterface apiInterface;
    private TokenManager tokenManager;

    private List<PriceRangeAnalytics> priceRanges = new ArrayList<>();
    private List<AreaBookingAnalytics> areas = new ArrayList<>();
    private List<Map<String, Object>> monthlyTrends = new ArrayList<>();
    private List<Map<String, Object>> topRooms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        tokenManager = new TokenManager(this);
        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        initializeViews();
        loadAllAnalytics();
    }

    private void initializeViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        pagerAdapter = new AnalyticsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Price Ranges");
                            break;
                        case 1:
                            tab.setText("Top Areas");
                            break;
                        case 2:
                            tab.setText("Monthly Trends");
                            break;
                        case 3:
                            tab.setText("Top Rooms");
                            break;
                    }
                }
        ).attach();
    }

    private void loadAllAnalytics() {
        loadPriceRangeAnalytics();
        loadAreaBookingAnalytics();
        loadMonthlyTrends();
        loadTopRooms();
    }

    private void loadPriceRangeAnalytics() {
        showLoading(true);
        String token = tokenManager.getAuthHeader();

        Call<Map<String, Object>> call = apiInterface.getMostFrequentPriceRanges(token);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    if (result.containsKey("success") && (boolean) result.get("success")) {
                        parsePriceRanges(result);
                        updatePriceRangeCharts();
                    } else {
                        String message = result.containsKey("message") ? (String) result.get("message") : "Failed to load price ranges";
                        Toast.makeText(AnalyticsActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Response error: " + response.code());
                    Toast.makeText(AnalyticsActivity.this, "Error loading price ranges", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Error loading price ranges", t);
                Toast.makeText(AnalyticsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAreaBookingAnalytics() {
        String token = tokenManager.getAuthHeader();

        Call<Map<String, Object>> call = apiInterface.getMostBookedAreas(token);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    if (result.containsKey("success") && (boolean) result.get("success")) {
                        parseAreas(result);
                        updateAreaCharts();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error loading areas", t);
            }
        });
    }

    private void loadMonthlyTrends() {
        String token = tokenManager.getAuthHeader();

        Call<Map<String, Object>> call = apiInterface.getMonthlyTrends(token);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    if (result.containsKey("success") && (boolean) result.get("success")) {
                        parseMonthlyTrends(result);
                        updateTrendsChart();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error loading trends", t);
            }
        });
    }

    private void loadTopRooms() {
        String token = tokenManager.getAuthHeader();

        Call<Map<String, Object>> call = apiInterface.getTopRevenueRooms(token);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    if (result.containsKey("success") && (boolean) result.get("success")) {
                        parseTopRooms(result);
                        updateTopRooms();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error loading top rooms", t);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void parsePriceRanges(Map<String, Object> result) {
        priceRanges.clear();

        try {
            Object dataObj = result.get("data");
            if (dataObj instanceof List) {
                List<?> dataList = (List<?>) dataObj;
                Gson gson = new Gson();
                for (Object item : dataList) {
                    String json = gson.toJson(item);
                    PriceRangeAnalytics priceRange = gson.fromJson(json, PriceRangeAnalytics.class);
                    priceRanges.add(priceRange);
                }
            }
            Log.d(TAG, "Parsed " + priceRanges.size() + " price ranges");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing price ranges", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseAreas(Map<String, Object> result) {
        areas.clear();

        try {
            Object dataObj = result.get("data");
            if (dataObj instanceof List) {
                List<?> dataList = (List<?>) dataObj;
                Gson gson = new Gson();
                for (Object item : dataList) {
                    String json = gson.toJson(item);
                    AreaBookingAnalytics area = gson.fromJson(json, AreaBookingAnalytics.class);
                    areas.add(area);
                }
            }
            Log.d(TAG, "Parsed " + areas.size() + " areas");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing areas", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseMonthlyTrends(Map<String, Object> result) {
        monthlyTrends.clear();

        try {
            Object dataObj = result.get("data");
            if (dataObj instanceof List) {
                monthlyTrends = (List<Map<String, Object>>) dataObj;
            }
            Log.d(TAG, "Parsed " + monthlyTrends.size() + " monthly trends");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing monthly trends", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseTopRooms(Map<String, Object> result) {
        topRooms.clear();

        try {
            Object dataObj = result.get("data");
            if (dataObj instanceof List) {
                topRooms = (List<Map<String, Object>>) dataObj;
            }
            Log.d(TAG, "Parsed " + topRooms.size() + " top rooms");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing top rooms", e);
        }
    }

    private void updatePriceRangeCharts() {
        PriceRangeFragment fragment = (PriceRangeFragment) pagerAdapter.getFragment(0);
        if (fragment != null && !priceRanges.isEmpty()) {
            fragment.updateData(priceRanges);
        }
    }

    private void updateAreaCharts() {
        AreaAnalyticsFragment fragment = (AreaAnalyticsFragment) pagerAdapter.getFragment(1);
        if (fragment != null && !areas.isEmpty()) {
            fragment.updateData(areas);
        }
    }

    private void updateTrendsChart() {
        // Check if activity is valid
        if (isFinishing() || isDestroyed()) {
            return;
        }

        MonthlyTrendsFragment fragment = (MonthlyTrendsFragment) pagerAdapter.getFragment(2);
        if (fragment != null && !monthlyTrends.isEmpty()) {
            fragment.updateData(monthlyTrends);
        }
    }

    private void updateTopRooms() {
        // Check if activity is not finishing before updating
        if (isFinishing() || isDestroyed()) {
            return;
        }

        TopRoomsFragment fragment = (TopRoomsFragment) pagerAdapter.getFragment(3);
        if (fragment != null && !topRooms.isEmpty()) {
            fragment.updateData(topRooms);
        }
    }

    private void showLoading(boolean show) {
        // You can implement a progress dialog or overlay here
        // For now, just log
        Log.d(TAG, "Loading: " + show);
    }
}