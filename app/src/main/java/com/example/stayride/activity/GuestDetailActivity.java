package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;

import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityGuestDetailBinding;
import com.example.stayride.databinding.ItemBookingBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class GuestDetailActivity extends BaseActivity {

    private ActivityGuestDetailBinding b;
    private Models.Guest guest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityGuestDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        int id = getIntent().getIntExtra("guest_id", -1);
        guest = DataStore.get().guest(id);
        if (guest == null) {
            finish();
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Guest profile");
        }

        b.btnEdit.setOnClickListener(v ->
                startActivity(new Intent(this, NewGuestActivity.class).putExtra("guest_id", guest.id)));
        b.btnCreateBooking.setOnClickListener(v ->
                startActivity(new Intent(this, NewBookingActivity.class).putExtra("guest_id", guest.id)));
        b.btnEditNotes.setOnClickListener(v -> editNotes());
        b.consentSwitch.setOnCheckedChangeListener((btn, checked) -> {
            guest.marketingConsent = checked;
            Ui.toast(this, checked
                    ? guest.name + " can receive WhatsApp offers"
                    : guest.name + " will not receive marketing messages");
            refresh();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        DataStore d = DataStore.get();
        guest = d.guest(guest.id);
        if (guest == null) {
            finish();
            return;
        }

        b.name.setText(guest.name);
        b.phone.setText(guest.phone);
        b.email.setText(TextUtils.isEmpty(guest.email) ? "No email on file" : guest.email);
        b.visits.setText(String.valueOf(guest.visits));
        b.spend.setText(Ui.money(guest.totalSpending));
        b.lastVisit.setText(Dates.fmt(guest.lastVisit));
        b.notes.setText(TextUtils.isEmpty(guest.notes) ? "No notes yet. Tap Edit to add one." : guest.notes);
        b.consentLabel.setText(guest.marketingConsent
                ? "Allowed for WhatsApp offers"
                : "Opted out of marketing");
        b.consentSwitch.setOnCheckedChangeListener(null);
        b.consentSwitch.setChecked(guest.marketingConsent);
        b.consentSwitch.setOnCheckedChangeListener((btn, checked) -> {
            guest.marketingConsent = checked;
            Ui.toast(this, checked
                    ? guest.name + " can receive WhatsApp offers"
                    : guest.name + " will not receive marketing messages");
            refresh();
        });

        if (guest.isRepeat()) {
            Ui.pill(b.badge, "REPEAT GUEST", com.example.stayride.R.color.status_purple_bg,
                    com.example.stayride.R.color.status_purple);
        } else {
            Ui.pill(b.badge, "NEW GUEST", com.example.stayride.R.color.status_green_bg,
                    com.example.stayride.R.color.status_green);
        }

        List<Models.Booking> history = d.bookingsOfGuest(guest.id);
        b.history.removeAllViews();
        for (Models.Booking booking : history) {
            ItemBookingBinding row = ItemBookingBinding.inflate(LayoutInflater.from(this), b.history, false);
            row.name.setText("Room " + booking.roomNumber + " \u00B7 " + booking.source);
            row.route.setText(booking.guests + (booking.guests == 1 ? " guest" : " guests")
                    + " \u00B7 " + booking.nights() + " nights");
            row.dates.setText(Dates.fmt(booking.checkIn) + " \u2192 " + Dates.fmt(booking.checkOut));
            row.amount.setText(Ui.money(booking.amount));
            Ui.styleBookingStatus(row.status, booking.status);
            if (booking.paid) {
                Ui.pill(row.payment, "PAID", com.example.stayride.R.color.status_green_bg,
                        com.example.stayride.R.color.status_green);
            } else {
                Ui.pill(row.payment, "UNPAID", com.example.stayride.R.color.status_orange_bg,
                        com.example.stayride.R.color.status_orange);
            }
            row.card.setOnClickListener(v ->
                    startActivity(new Intent(this, BookingDetailActivity.class)
                            .putExtra("booking_id", booking.id)));
            b.history.addView(row.getRoot());
        }
        b.historyCount.setText(history.size() + (history.size() == 1 ? " stay" : " stays"));
        b.emptyHistory.setVisibility(history.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void editNotes() {
        EditText input = new EditText(this);
        input.setHint("Notes");
        input.setText(guest.notes);
        input.setMinLines(3);
        input.setGravity(android.view.Gravity.TOP);
        input.setSelection(input.getText().length());
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad / 2, pad, 0);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Notes for " + guest.name)
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    guest.notes = input.getText().toString().trim();
                    Ui.toast(this, "Notes saved");
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
