package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stayride.R;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityBookingDetailBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Ui;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BookingDetailActivity extends BaseActivity {

    private ActivityBookingDetailBinding b;
    private Models.Booking booking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityBookingDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Booking details");

        int id = getIntent().getIntExtra("booking_id", -1);
        booking = DataStore.get().booking(id);
        if (booking == null) {
            finish();
            return;
        }

        b.btnViewGuest.setOnClickListener(v ->
                startActivity(new Intent(this, GuestDetailActivity.class).putExtra("guest_id", booking.guestId)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        DataStore store = DataStore.get();
        booking = store.booking(booking.id);
        if (booking == null) {
            finish();
            return;
        }

        b.guestName.setText(booking.guestName);
        b.room.setText("Room " + booking.roomNumber + " \u00B7 " + booking.guests
                + (booking.guests == 1 ? " guest" : " guests") + " \u00B7 " + booking.nights() + " night(s)");
        b.dates.setText(Dates.fmtLong(booking.checkIn) + "  \u2192  " + Dates.fmtLong(booking.checkOut));
        b.phone.setText(booking.phone);
        b.amount.setText(Ui.money(booking.amount));
        b.amountMeta.setText(booking.nights() + " nights \u00B7 " + booking.roomNumber);
        b.source.setText("Source: " + booking.source);
        Ui.styleBookingStatus(b.status, booking.status);
        if (booking.paid) {
            Ui.pill(b.payment, "PAID", R.color.status_green_bg, R.color.status_green);
        } else {
            Ui.pill(b.payment, "UNPAID", R.color.status_orange_bg, R.color.status_orange);
        }

        switch (booking.status) {
            case "CONFIRMED":
                b.statusNote.setText("Next step: check the guest in when they arrive. "
                        + "You can also assign a different room.");
                break;
            case "CHECKED_IN":
                b.statusNote.setText("Guest is currently in-house. Remember to settle the payment at check-out.");
                break;
            case "CHECKED_OUT":
                b.statusNote.setText(booking.paid
                        ? "Stay completed. This guest is saved in your guest database for future campaigns."
                        : "Stay completed, payment is still pending. Mark it as paid once received.");
                break;
            default:
                b.statusNote.setText("This booking was cancelled. The room is available again.");
                break;
        }

        rebuildActions();
    }

    private void rebuildActions() {
        b.actions.removeAllViews();
        switch (booking.status) {
            case "CONFIRMED":
                add("Assign / change room", true, this::assignRoom);
                add("Check in guest", false, this::checkIn);
                add("Cancel booking", true, this::confirmCancel);
                break;
            case "CHECKED_IN":
                add("Check out guest", false, this::checkOut);
                if (!booking.paid) add("Mark payment as paid", true, this::markPaid);
                break;
            case "CHECKED_OUT":
                if (!booking.paid) add("Mark payment as paid", false, this::markPaid);
                break;
            default:
                break;
        }
    }

    private void add(String label, boolean outlined, Runnable action) {
        MaterialButton button = new MaterialButton(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        button.setLayoutParams(lp);
        button.setText(label);
        button.setAllCaps(false);
        if (outlined) {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.TRANSPARENT));
            button.setStrokeColorResource(R.color.brand_primary);
            button.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
            button.setTextColor(Ui.color(button, R.color.brand_primary));
        }
        button.setPadding(button.getPaddingLeft(), (int) (12 * getResources().getDisplayMetrics().density),
                button.getPaddingRight(), (int) (12 * getResources().getDisplayMetrics().density));
        button.setOnClickListener(v -> action.run());
        b.actions.addView(button);
    }

    private void assignRoom() {
        DataStore store = DataStore.get();
        int checked = 0;
        final String[] numbers = new String[store.rooms.size()];
        for (int i = 0; i < store.rooms.size(); i++) {
            Models.Room room = store.rooms.get(i);
            numbers[i] = "Room " + room.number + " \u00B7 " + room.type + " \u00B7 "
                    + Ui.money(room.price) + " \u00B7 " + room.status;
            if (room.number.equals(booking.roomNumber)) checked = i;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Assign a room")
                .setSingleChoiceItems(numbers, checked, (dialog, which) -> {
                    Models.Room room = store.rooms.get(which);
                    Models.Room previous = null;
                    for (Models.Room r : store.rooms) {
                        if (r.number.equals(booking.roomNumber)) previous = r;
                    }
                    booking.roomNumber = room.number;
                    if ("AVAILABLE".equals(room.status) && !"CHECKED_IN".equals(booking.status)) {
                        room.status = "RESERVED";
                    }
                    if ("CHECKED_IN".equals(booking.status)) {
                        room.status = "OCCUPIED";
                    }
                    if (previous != null && !previous.number.equals(room.number)
                            && "RESERVED".equals(previous.status)) {
                        previous.status = "AVAILABLE";
                    }
                    Ui.toast(this, "Room " + room.number + " assigned");
                    refresh();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkIn() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Check in " + booking.guestName + "?")
                .setMessage("Room " + booking.roomNumber + " will be marked occupied and the guest will be checked in.")
                .setPositiveButton("Check in", (dialog, which) -> {
                    booking.status = "CHECKED_IN";
                    for (Models.Room r : DataStore.get().rooms) {
                        if (r.number.equals(booking.roomNumber)) r.status = "OCCUPIED";
                    }
                    Ui.toast(this, booking.guestName + " checked in to room " + booking.roomNumber);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkOut() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Check out " + booking.guestName + "?")
                .setMessage("The stay will be closed and the room will become available again.")
                .setPositiveButton("Check out", (dialog, which) -> {
                    booking.status = "CHECKED_OUT";
                    for (Models.Room r : DataStore.get().rooms) {
                        if (r.number.equals(booking.roomNumber) && "OCCUPIED".equals(r.status)) {
                            r.status = "AVAILABLE";
                        }
                    }
                    Models.Guest guest = DataStore.get().guest(booking.guestId);
                    if (guest != null) {
                        guest.visits += 1;
                        guest.totalSpending += booking.amount;
                        guest.lastVisit = booking.checkOut < Dates.today() ? booking.checkOut : Dates.today();
                        if (guest.firstVisit == 0) guest.firstVisit = booking.checkIn;
                    }
                    Ui.toast(this, booking.guestName + " checked out");
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void markPaid() {
        booking.paid = true;
        Ui.toast(this, "Payment of " + Ui.money(booking.amount) + " marked as paid");
        refresh();
    }

    private void confirmCancel() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel this booking?")
                .setMessage("Room " + booking.roomNumber + " will be released and the guest will be notified (demo).")
                .setPositiveButton("Cancel booking", (dialog, which) -> {
                    booking.status = "CANCELLED";
                    booking.paid = false;
                    for (Models.Room r : DataStore.get().rooms) {
                        if (r.number.equals(booking.roomNumber) && "RESERVED".equals(r.status)) {
                            r.status = "AVAILABLE";
                        }
                    }
                    Ui.toast(this, "Booking cancelled");
                    refresh();
                })
                .setNegativeButton("Keep booking", null)
                .show();
    }
}
