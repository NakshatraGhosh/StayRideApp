package com.example.stayride.util;

import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class Charts {

    public static final int PERIOD_7D = 0;
    public static final int PERIOD_30D = 1;
    public static final int PERIOD_6M = 2;
    public static final int PERIOD_12M = 3;

    public static final int METRIC_REVENUE = 0;
    public static final int METRIC_BOOKINGS = 1;
    public static final int METRIC_CAB_RIDES = 2;
    public static final int METRIC_COMMISSION = 3;

    public static class Series {
        public final List<String> labels = new ArrayList<>();
        public final List<Integer> values = new ArrayList<>();
        public int total;
        public int peak;
        public int changePct;
        public boolean money;

        public boolean hasChange() {
            return previousTotal > 0;
        }

        private int previousTotal;
    }

    private Charts() {
    }

    public static String periodLabel(int period) {
        switch (period) {
            case PERIOD_7D: return "Last 7 days";
            case PERIOD_6M: return "Last 6 months";
            case PERIOD_12M: return "Last 12 months";
            default: return "Last 30 days";
        }
    }

    public static Series build(int metric, int period) {
        Series s = new Series();
        s.money = metric == METRIC_REVENUE || metric == METRIC_COMMISSION;

        long todayEnd = Dates.today() + Dates.DAY;
        long start;
        int buckets;
        BucketMode mode;
        switch (period) {
            case PERIOD_7D:
                start = Dates.today() - 6 * Dates.DAY;
                buckets = 7;
                mode = BucketMode.DAY;
                break;
            case PERIOD_30D:
                start = Dates.today() - 29 * Dates.DAY;
                buckets = 5;
                mode = BucketMode.SIX_DAYS;
                break;
            case PERIOD_6M:
                start = monthStart(-5);
                buckets = 6;
                mode = BucketMode.MONTH;
                break;
            default:
                start = monthStart(-11);
                buckets = 12;
                mode = BucketMode.MONTH;
                break;
        }

        long window = todayEnd - start;
        long prevStart = start - window;
        long[] edges = new long[buckets + 1];
        edges[0] = start;
        for (int i = 1; i <= buckets; i++) {
            if (mode == BucketMode.MONTH) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(start);
                c.add(Calendar.MONTH, i);
                edges[i] = c.getTimeInMillis();
            } else {
                edges[i] = start + (long) i * window / buckets;
            }
        }
        edges[buckets] = todayEnd;

        int[] values = new int[buckets];
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        for (int i = 0; i < buckets; i++) {
            long at = edges[i];
            String label;
            if (mode == BucketMode.MONTH) {
                label = monthFormat.format(new Date(at));
            } else if (period == PERIOD_7D) {
                label = dayFormat.format(new Date(at));
            } else {
                label = dateFormat.format(new Date(at));
            }
            s.labels.add(label);
            values[i] = (int) total(metric, edges[i], edges[i + 1]);
        }

        for (int i = 0; i < buckets; i++) {
            s.values.add(values[i]);
            s.total += values[i];
            s.peak = Math.max(s.peak, values[i]);
        }
        s.previousTotal = (int) total(metric, prevStart, start);
        if (s.previousTotal > 0) {
            s.changePct = Math.round((s.total - s.previousTotal) * 100f / s.previousTotal);
        } else {
            s.changePct = s.total > 0 ? 100 : 0;
        }
        return s;
    }

    private static long monthStart(int monthOffset) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(Dates.today());
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MONTH, monthOffset);
        return c.getTimeInMillis();
    }

    private static long total(int metric, long from, long to) {
        DataStore d = DataStore.get();
        long sum = 0;
        if (metric == METRIC_REVENUE || metric == METRIC_BOOKINGS) {
            for (Models.Booking b : d.bookings) {
                if ("CANCELLED".equals(b.status)) continue;
                if (b.checkIn >= from && b.checkIn < to) {
                    sum += metric == METRIC_REVENUE ? b.amount : 1;
                }
            }
        } else {
            for (Models.CabTrip t : d.trips) {
                if (t.isCancelled()) continue;
                if (t.date >= from && t.date < to) {
                    if (metric == METRIC_CAB_RIDES) {
                        sum += 1;
                    } else if (t.isCompleted()) {
                        sum += t.commission;
                    }
                }
            }
        }
        return sum;
    }

    private enum BucketMode { DAY, SIX_DAYS, MONTH }
}
