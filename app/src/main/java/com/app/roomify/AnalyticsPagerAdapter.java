package com.app.roomify;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AnalyticsPagerAdapter extends FragmentStateAdapter {

    private final PriceRangeFragment priceRangeFragment = new PriceRangeFragment();
    private final AreaAnalyticsFragment areaAnalyticsFragment = new AreaAnalyticsFragment();
    private final Fragment[] fragments = {
            priceRangeFragment,
            areaAnalyticsFragment,
            new MonthlyTrendsFragment(),
            new TopRoomsFragment()
    };

    public AnalyticsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments[position];
    }

    @Override
    public int getItemCount() {
        return fragments.length;
    }

    public Fragment getFragment(int position) {
        return fragments[position];
    }
}