package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.stayride.R;
import com.example.stayride.adapter.TripAdapter;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityGuestPageBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class GuestPageActivity extends BaseActivity {

    private static final String[] PICKUPS = {
            "Mountain View Homestay", "NJP Station", "Bagdogra Airport",
            "Gangtok MG Marg", "Darjeeling", "Pelling"
    };
    private static final String[] DROPS = {
            "NJP Station", "Bagdogra Airport", "Darjeeling Sightseeing",
            "Gangtok MG Marg", "Lachung", "Pelling"
    };
    private static final String[] DATES = {"Today", "Tomorrow", "In 2 days", "In 3 days", "In 4 days"};
    private static final String[] TIMES = {
            "06:00 AM", "07:30 AM", "09:00 AM", "10:30 AM", "12:00 PM",
            "02:00 PM", "04:30 PM", "06:00 PM", "08:00 PM"
    };
    private static final String[] VEHICLES = {"SUV", "Sedan", "7-seater", "9-seater"};

    private ActivityGuestPageBinding b;
    private final TripAdapter tripAdapter = new TripAdapter();
    private String lastPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityGuestPageBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.toolbar.setNavigationOnClickListener(v -> finish());

        b.myTrips.setLayoutManager(new LinearLayoutManager(this));
        b.myTrips.setAdapter(tripAdapter);
        tripAdapter.setListener(t -> Ui.toast(this,
                Ui.tripStatusLabel(t.status) + " \u00B7 "
                        + (t.driverName == null ? "driver not assigned yet" : t.driverName)));

        b.editPickup.setSimpleItems(PICKUPS);
        b.editDestination.setSimpleItems(DROPS);
        b.editDate.setSimpleItems(DATES);
        b.editTime.setSimpleItems(TIMES);
        b.editVehicleType.setSimpleItems(VEHICLES);
        Ui.wireDropdown(b.editPickup);
        Ui.wireDropdown(b.editDestination);
        Ui.wireDropdown(b.editDate);
        Ui.wireDropdown(b.editTime);
        Ui.wireDropdown(b.editVehicleType);

        b.editPickup.setText(PICKUPS[0], false);
        b.editDestination.setText(DROPS[0], false);
        b.editDate.setText(DATES[1], false);
        b.editTime.setText(TIMES[2], false);
        b.editVehicleType.setText(VEHICLES[0], false);

        b.editVehicleType.addTextChangedListener(fareWatcher);
        b.requestCab.setOnClickListener(v -> requestCab());
    }

    private final TextWatcher fareWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateEstimate();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        updateEstimate();
    }

    private void refresh() {
        Models.Homestay h = DataStore.get().homestay;
        b.toolbar.setTitle(h.name);
        b.guestName.setText(h.name);
        b.guestLocation.setText(h.location);
        b.guestDescription.setText(h.description);

        DataStore d = DataStore.get();
        b.roomRow.removeAllViews();
        int shown = 0;
        for (Models.Room room : d.rooms) {
            if (!"AVAILABLE".equals(room.status)) continue;
            shown++;
            b.roomRow.addView(roomRow(room));
        }
        if (shown == 0) {
            TextView none = new TextView(this);
            none.setText("No rooms available for these dates.");
            none.setTextSize(13);
            none.setTextColor(Ui.color(none, R.color.text_secondary));
            none.setPadding(0, dp(8), 0, 0);
            b.roomRow.addView(none);
        }
        renderMyTrips();
    }

    private View roomRow(Models.Room room) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card);
        int pad = dp(14);
        row.setPadding(pad, dp(12), pad, dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        row.setLayoutParams(params);

        TextView info = new TextView(this);
        info.setText("Room " + room.number + " \u00B7 " + room.type + "\n" + room.note);
        info.setTextSize(14);
        info.setTextColor(Ui.color(info, R.color.text_primary));
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(infoParams);

        TextView price = new TextView(this);
        price.setText(Ui.money(room.price) + "\nper night");
        price.setTextSize(13);
        price.setGravity(Gravity.END);
        price.setTextColor(Ui.color(price, R.color.brand_primary_dark));
        price.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(info);
        row.addView(price);
        return row;
    }

    private int baseFare(String vehicleType) {
        switch (vehicleType == null ? "" : vehicleType) {
            case "Sedan": return 2200;
            case "7-seater": return 3200;
            case "9-seater": return 4200;
            default: return 3000;
        }
    }

    private void updateEstimate() {
        String vehicle = b.editVehicleType.getText() == null
                ? "" : b.editVehicleType.getText().toString();
        String dateLabel = b.editDate.getText() == null ? "" : b.editDate.getText().toString();
        int extra = dateLabel.startsWith("In 3") ? 400 : dateLabel.startsWith("In 2") ? 300
                : dateLabel.startsWith("In 4") ? 500 : 0;
        int fare = baseFare(vehicle) + extra;
        b.estimate.setText("Estimated fare " + Ui.money(fare)
                + "  \u00B7  final fare confirmed when the driver accepts");
    }

    private void requestCab() {
        String name = text(b.editGuestName.getText());
        String phone = text(b.editGuestPhone.getText());
        if (TextUtils.isEmpty(name)) {
            b.editGuestName.setError("Please enter your name");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            b.inputGuestPhone.setError("Please enter your phone number");
            return;
        }

        DataStore d = DataStore.get();
        Models.CabTrip trip = new Models.CabTrip(d.nextTripId++);
        trip.guestName = name;
        trip.phone = phone;
        trip.pickup = text(b.editPickup.getText());
        trip.destination = text(b.editDestination.getText());
        trip.date = Dates.today() + dateOffset(text(b.editDate.getText()));
        trip.time = text(b.editTime.getText());
        try {
            trip.passengers = Integer.parseInt(text(b.editPassengers.getText()));
        } catch (NumberFormatException e) {
            trip.passengers = 2;
        }
        trip.vehicleType = text(b.editVehicleType.getText());
        trip.estFare = baseFare(trip.vehicleType);
        trip.status = "NEW_REQUEST";
        d.trips.add(0, trip);
        lastPhone = phone;

        refresh();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cab request sent!")
                .setMessage("Your request for a " + trip.vehicleType + " on "
                        + Dates.fmt(trip.date) + " at " + trip.time + " has reached "
                        + "Mountain View Homestay. You will get a call once a driver is assigned.")
                .setPositiveButton("Done", null)
                .show();
    }

    private int dateOffset(String label) {
        if ("Today".equals(label)) return 0;
        if ("Tomorrow".equals(label)) return 1;
        if ("In 2 days".equals(label)) return 2;
        if ("In 3 days".equals(label)) return 3;
        if ("In 4 days".equals(label)) return 4;
        return 1;
    }

    private void renderMyTrips() {
        String phone = lastPhone.isEmpty() ? text(b.editGuestPhone.getText()) : lastPhone;
        List<Models.CabTrip> mine = new ArrayList<>();
        if (!phone.isEmpty()) {
            for (Models.CabTrip t : DataStore.get().trips) {
                if (phone.equals(t.phone)) mine.add(t);
            }
        }
        tripAdapter.submit(mine);
        b.noTrips.setVisibility(mine.isEmpty() ? View.VISIBLE : View.GONE);
        b.myTrips.setVisibility(mine.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String text(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
