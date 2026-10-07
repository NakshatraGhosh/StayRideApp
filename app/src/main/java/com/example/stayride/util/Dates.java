package com.example.stayride.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class Dates {

    public static final long DAY = 86400000L;

    private Dates() {
    }

    public static long today() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long plusDays(int days) {
        return today() + days * DAY;
    }

    public static String fmt(long millis) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(millis));
    }

    public static String fmtLong(long millis) {
        return new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(new Date(millis));
    }

    public static String monthLabel(long millis) {
        return new SimpleDateFormat("MMM", Locale.getDefault()).format(new Date(millis));
    }

    public static String monthYear(long millis) {
        return new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date(millis));
    }

    public static int nights(long checkIn, long checkOut) {
        return Math.max(1, (int) TimeUnit.MILLISECONDS.toDays(checkOut - checkIn));
    }

    public static boolean isToday(long millis) {
        return millis >= today() && millis < today() + DAY;
    }

    public static boolean isPast(long millis) {
        return millis < today();
    }
}
