package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.example.stayride.R;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityNewBookingBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Ui;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewBookingActivity extends BaseActivity {

    private ActivityNewBookingBinding b;
    private final List<Models.Guest> guestOptions = new ArrayList<>();
    private final List<Models.Room> roomOptions = new ArrayList<>();
    private Models.Guest selectedGuest;
    private Models.Room selectedRoom;
    private long checkIn = -1;
    private long checkOut = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityNewBookingBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setTitle("New booking");

        DataStore d = DataStore.get();

        guestOptions.clear();
        guestOptions.addAll(d.guests);
        List<String> guestNames = new ArrayList<>();
        for (Models.Guest g : guestOptions) guestNames.add(g.name);
        b.guestPicker.setSimpleItems(guestNames.toArray(new String[0]));
        Ui.wireDropdown(b.guestPicker);
        b.guestPicker.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= guestOptions.size()) return;
            selectedGuest = guestOptions.get(position);
            b.editPhone.setText(selectedGuest.phone);
            b.inputGuest.setError(null);
            b.inputPhone.setError(null);
        });
        b.guestPicker.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String typed = s == null ? "" : s.toString().trim();
                if (selectedGuest == null || !typed.equalsIgnoreCase(selectedGuest.name)) {
                    selectedGuest = null;
                }
            }
        });

        int presetGuestId = getIntent().getIntExtra("guest_id", -1);
        if (presetGuestId > 0) {
            Models.Guest preset = d.guest(presetGuestId);
            if (preset != null) {
                selectedGuest = preset;
                b.guestPicker.setText(preset.name, false);
                b.editPhone.setText(preset.phone);
            }
        }

        List<String> roomNames = new ArrayList<>();
        roomOptions.clear();
        for (Models.Room r : d.rooms) {
            roomOptions.add(r);
            roomNames.add("Room " + r.number + " \u00B7 " + r.type + " \u00B7 "
                    + Ui.money(r.price) + " \u00B7 " + r.status);
        }
        b.roomPicker.setSimpleItems(roomNames.toArray(new String[0]));
        Ui.wireDropdown(b.roomPicker);
        b.roomPicker.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= roomOptions.size()) return;
            selectedRoom = roomOptions.get(position);
            b.inputRoom.setError(null);
            computeAmount();
        });

        b.sourcePicker.setSimpleItems(new String[]{"StayRide link", "Walk-in", "Phone", "WhatsApp", "Repeat booking"});
        b.sourcePicker.setText("StayRide link", false);
        Ui.wireDropdown(b.sourcePicker);

        b.btnDates.setOnClickListener(v -> openDatePicker());
        b.btnSave.setOnClickListener(v -> save());
    }

    private void openDatePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select check-in and check-out")
                .setSelection(new Pair<>(checkIn > 0 ? checkIn : Dates.today(),
                        checkOut > 0 ? checkOut : Dates.plusDays(2)))
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && selection.first != null && selection.second != null) {
                checkIn = selection.first;
                checkOut = Math.max(selection.second, selection.first + Dates.DAY);
                b.datesLabel.setText("Check-in " + Dates.fmtLong(checkIn)
                        + "\nCheck-out " + Dates.fmtLong(checkOut)
                        + "  \u00B7  " + Dates.nights(checkIn, checkOut) + " night(s)");
                b.btnDates.setText("Change dates");
                computeAmount();
            }
        });
        picker.show(getSupportFragmentManager(), "dates");
    }

    private void computeAmount() {
        if (selectedRoom == null || checkIn < 0) return;
        int amount = selectedRoom.price * Dates.nights(checkIn, checkOut);
        b.editAmount.setText(String.valueOf(amount));
    }

    private Models.Guest findGuest(String name) {
        for (Models.Guest g : guestOptions) {
            if (g.name.equalsIgnoreCase(name)) return g;
        }
        return null;
    }

    private void save() {
        String name = text(b.guestPicker.getText());
        String phone = text(b.editPhone.getText());

        if (name.isEmpty()) {
            b.inputGuest.setError("Guest name is required");
            return;
        }
        b.inputGuest.setError(null);
        if (checkIn < 0) {
            Ui.toast(this, "Please select your stay dates");
            return;
        }
        if (selectedRoom == null) {
            b.inputRoom.setError("Please select a room");
            return;
        }
        b.inputRoom.setError(null);

        Models.Guest guest = findGuest(name);
        if (guest == null && phone.isEmpty()) {
            b.inputPhone.setError("Phone number is required for a new guest");
            return;
        }
        b.inputPhone.setError(null);

        int amount;
        try {
            amount = Integer.parseInt(text(b.editAmount.getText()));
        } catch (NumberFormatException e) {
            b.inputAmount.setError("Enter a valid amount");
            return;
        }
        b.inputAmount.setError(null);
        int count;
        try {
            count = Integer.parseInt(text(b.editCount.getText()));
        } catch (NumberFormatException e) {
            count = 1;
        }

        Integer guestId = guest == null ? null : guest.id;
        String source = b.sourcePicker.getText() == null ? "Walk-in" : b.sourcePicker.getText().toString();
        if (source.isEmpty()) source = "Walk-in";

        DataStore.get().createBooking(guestId, name, phone, "", selectedRoom.number, checkIn, checkOut,
                Math.max(1, count), amount, source, () -> {
                    Ui.toast(this, "Booking created for " + name + " \u00B7 Room " + selectedRoom.number);
                    finish();
                });
    }

    private String text(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
