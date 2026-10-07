package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stayride.R;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityAnalyticsBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Ui;
import com.example.stayride.views.BarChartView;
import com.example.stayride.views.PieChartView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsActivity extends BaseActivity {

    private ActivityAnalyticsBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Analytics");
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        DataStore d = DataStore.get();
        long cutoff = Dates.today() - 30 * Dates.DAY;

        int revenue30 = 0;
        int commission30 = 0;
        Map<String, Integer> byType = new LinkedHashMap<>();
        for (Models.CabTrip t : d.trips) {
            if (t.isCancelled()) continue;
            int value = t.fare > 0 ? t.fare : t.estFare;
            if (t.isCompleted() && t.date >= cutoff) {
                revenue30 += t.fare;
                commission30 += t.commission;
            }
            byType.merge(t.vehicleType, value, Integer::sum);
        }
        b.revenue.setText(Ui.money(revenue30));
        b.commission.setText(Ui.money(commission30));

        int[] typeColors = {
                R.color.brand_primary, R.color.brand_accent, R.color.status_blue,
                R.color.status_purple, R.color.status_green, R.color.status_orange
        };
        List<PieChartView.Segment> segments = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> e : byType.entrySet()) {
            segments.add(new PieChartView.Segment(e.getKey(), e.getValue(),
                    Ui.color(b.pie, typeColors[i % typeColors.length])));
            i++;
        }
        b.pie.setSegments(segments);

        List<BarChartView.Bar> bars = new ArrayList<>();
        for (int week = 0; week < 5; week++) {
            long start = Dates.today() + (week - 4) * 7 * Dates.DAY;
            long end = start + 7 * Dates.DAY;
            int sum = 0;
            for (Models.CabTrip t : d.trips) {
                if (t.isCancelled()) continue;
                if (t.date >= start && t.date < end) {
                    sum += t.fare > 0 ? t.fare : t.estFare;
                }
            }
            bars.add(new BarChartView.Bar(Dates.monthLabel(start), sum));
        }
        b.bars.setBars(bars);
    }
}
