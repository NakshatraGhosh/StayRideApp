package com.example.stayride.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.stayride.R;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.Locale;

public final class Ui {

    private Ui() {
    }

    public static String money(int amount) {
        NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("en-IN"));
        return "\u20B9" + nf.format(amount);
    }

    public static void wireDropdown(android.widget.AutoCompleteTextView drop) {
        if (drop == null) return;
        drop.setOnClickListener(v -> drop.showDropDown());
        drop.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) drop.showDropDown();
        });
        drop.setOnTouchListener((v, event) -> {
            if (!drop.isPopupShowing()) drop.showDropDown();
            return false;
        });
    }

    public static void toast(Context c, String msg) {
        android.widget.Toast.makeText(c, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    public static void longToast(Context c, String msg) {
        android.widget.Toast.makeText(c, msg, android.widget.Toast.LENGTH_LONG).show();
    }

    public static void snack(View view, String msg) {
        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show();
    }

    public static void pill(TextView tv, String label, int bgColor, int fgColor) {
        tv.setText(label);
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(tv.getResources().getDimensionPixelSize(R.dimen.pill_radius));
        d.setColor(ContextCompat.getColor(tv.getContext(), bgColor));
        tv.setBackground(d);
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), fgColor));
    }

    public static void cardTint(View v, int colorRes) {
        v.setBackgroundColor(ContextCompat.getColor(v.getContext(), colorRes));
    }

    public static int color(View v, int res) {
        return ContextCompat.getColor(v.getContext(), res);
    }

    public static String bookingStatus(String status) {
        switch (status) {
            case "CONFIRMED": return "Confirmed";
            case "CHECKED_IN": return "Checked in";
            case "CHECKED_OUT": return "Checked out";
            case "CANCELLED": return "Cancelled";
            default: return status;
        }
    }

    public static void styleBookingStatus(TextView tv, String status) {
        switch (status) {
            case "CONFIRMED": pill(tv, "CONFIRMED", R.color.status_blue_bg, R.color.status_blue); break;
            case "CHECKED_IN": pill(tv, "CHECKED IN", R.color.status_green_bg, R.color.status_green); break;
            case "CHECKED_OUT": pill(tv, "CHECKED OUT", R.color.status_grey_bg, R.color.status_grey); break;
            case "CANCELLED": pill(tv, "CANCELLED", R.color.status_red_bg, R.color.status_red); break;
            default: pill(tv, status, R.color.status_grey_bg, R.color.status_grey);
        }
    }

    public static void styleRoomStatus(TextView tv, String status) {
        switch (status) {
            case "AVAILABLE": pill(tv, "AVAILABLE", R.color.status_green_bg, R.color.status_green); break;
            case "OCCUPIED": pill(tv, "OCCUPIED", R.color.status_blue_bg, R.color.status_blue); break;
            case "RESERVED": pill(tv, "RESERVED", R.color.status_orange_bg, R.color.status_orange); break;
            case "MAINTENANCE": pill(tv, "MAINTENANCE", R.color.status_red_bg, R.color.status_red); break;
            default: pill(tv, status, R.color.status_grey_bg, R.color.status_grey);
        }
    }

    public static void styleTripStatus(TextView tv, String status) {
        switch (status) {
            case "NEW_REQUEST": pill(tv, "NEW REQUEST", R.color.status_orange_bg, R.color.status_orange); break;
            case "FINDING_DRIVER": pill(tv, "FINDING DRIVER", R.color.status_purple_bg, R.color.status_purple); break;
            case "DRIVER_ASSIGNED": pill(tv, "DRIVER ASSIGNED", R.color.status_blue_bg, R.color.status_blue); break;
            case "CONFIRMED": pill(tv, "CONFIRMED", R.color.status_green_bg, R.color.status_green); break;
            case "TRIP_STARTED": pill(tv, "TRIP STARTED", R.color.status_blue_bg, R.color.status_blue); break;
            case "COMPLETED": pill(tv, "COMPLETED", R.color.status_grey_bg, R.color.status_grey); break;
            case "CANCELLED": pill(tv, "CANCELLED", R.color.status_red_bg, R.color.status_red); break;
            default: pill(tv, status, R.color.status_grey_bg, R.color.status_grey);
        }
    }

    public static void styleCampaignStatus(TextView tv, String status) {
        switch (status) {
            case "CREATED": pill(tv, "CREATED", R.color.status_blue_bg, R.color.status_blue); break;
            case "SENDING": pill(tv, "SENDING", R.color.status_orange_bg, R.color.status_orange); break;
            case "COMPLETED": pill(tv, "COMPLETED", R.color.status_green_bg, R.color.status_green); break;
            default: pill(tv, status, R.color.status_grey_bg, R.color.status_grey);
        }
    }

    public static String tripStatusLabel(String status) {
        switch (status) {
            case "NEW_REQUEST": return "New Request";
            case "FINDING_DRIVER": return "Finding Driver";
            case "DRIVER_ASSIGNED": return "Driver Assigned";
            case "CONFIRMED": return "Confirmed";
            case "TRIP_STARTED": return "Trip Started";
            case "COMPLETED": return "Completed";
            case "CANCELLED": return "Cancelled";
            default: return status;
        }
    }

    public static int statusColor(String status) {
        switch (status) {
            case "COMPLETED":
            case "CHECKED_IN":
            case "AVAILABLE":
                return Color.parseColor("#1E9E5A");
            case "NEW_REQUEST":
            case "RESERVED":
                return Color.parseColor("#E07B00");
            case "CANCELLED":
            case "MAINTENANCE":
                return Color.parseColor("#D64545");
            default:
                return Color.parseColor("#2D6CDF");
        }
    }
}
