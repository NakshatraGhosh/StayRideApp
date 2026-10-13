package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.stayride.R;
import com.example.stayride.adapter.DriverAdapter;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityFindDriverBinding;
import com.example.stayride.databinding.DialogDriverProfileBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.DriverBooking;
import com.example.stayride.util.Geo;
import com.example.stayride.util.Sim;
import com.example.stayride.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FindDriverActivity extends BaseActivity {

    private static final int REQ_LOCATION = 1001;
    private static final long SEARCH_MILLIS = 4000L;

    private ActivityFindDriverBinding b;
    private final DriverAdapter adapter = new DriverAdapter(false);
    private final List<Long> dates = new ArrayList<>();
    private final List<String> dateLabels = new ArrayList<>();
    private double[] yourLocation;
    private String chosenPickup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityFindDriverBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Find a Driver");

        b.list.setLayoutManager(new LinearLayoutManager(this));
        b.list.setAdapter(adapter);
        adapter.setListener(new DriverAdapter.Listener() {
            @Override
            public void onDriverClick(Models.Driver driver) {
                showProfile(driver);
            }

            @Override
            public void onEdit(Models.Driver driver) {
            }

            @Override
            public void onDelete(Models.Driver driver) {
            }

            @Override
            public void onActiveChanged(Models.Driver driver, boolean active) {
            }

            @Override
            public void onBook(Models.Driver driver) {
                DriverBooking.start(FindDriverActivity.this, driver, chosenPickup);
            }
        });

        b.pickup.setSimpleItems(DriverBooking.pickupItems(
                DataStore.get().homestay.name).toArray(new String[0]));
        Ui.wireDropdown(b.pickup);
        b.pickup.setText(DataStore.get().homestay.name, false);
        b.pickup.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item != null && "Use my current location".equals(item.toString())) {
                useMyLocation();
            }
        });

        for (int i = 0; i < 8; i++) {
            long d = Dates.plusDays(i);
            dates.add(d);
            dateLabels.add(i == 0 ? "Today \u00B7 " + Dates.fmt(d)
                    : i == 1 ? "Tomorrow \u00B7 " + Dates.fmt(d) : Dates.fmt(d));
        }
        b.rideDate.setSimpleItems(dateLabels.toArray(new String[0]));
        Ui.wireDropdown(b.rideDate);
        b.rideDate.setText(dateLabels.get(1), false);

        b.rideTime.setSimpleItems(DriverBooking.TIMES);
        Ui.wireDropdown(b.rideTime);
        b.rideTime.setText(DriverBooking.TIMES[2], false);

        b.btnSearch.setOnClickListener(v -> startSearch());
        b.btnSearchAgain.setOnClickListener(v -> showPanel(Panel.INPUT));

        showPanel(Panel.INPUT);
    }

    private enum Panel {INPUT, SEARCHING, RESULTS}

    private void showPanel(Panel panel) {
        b.inputCard.setVisibility(panel == Panel.INPUT ? View.VISIBLE : View.GONE);
        b.searchPanel.setVisibility(panel == Panel.SEARCHING ? View.VISIBLE : View.GONE);
        b.resultsPanel.setVisibility(panel == Panel.RESULTS ? View.VISIBLE : View.GONE);
        if (panel == Panel.SEARCHING) {
            b.radar.start();
        } else {
            b.radar.stop();
        }
    }

    private void startSearch() {
        chosenPickup = text(b.pickup.getText());
        if (chosenPickup.isEmpty()) chosenPickup = DataStore.get().homestay.name;
        b.searchTitle.setText("Searching for nearby drivers\u2026");
        showPanel(Panel.SEARCHING);
        Sim.delay(SEARCH_MILLIS, this::showResults);
    }

    private void showResults() {
        if (b == null) return;
        DataStore d = DataStore.get();
        double[] target = target();

        List<Models.Driver> result = new ArrayList<>(d.networkDrivers);
        final Map<Integer, Double> dist = new HashMap<>();
        for (Models.Driver driver : result) {
            dist.put(driver.id, Geo.distanceFrom(target, driver));
        }
        Collections.sort(result, (a, c) -> Double.compare(dist.get(a.id), dist.get(c.id)));

        b.count.setText(result.size() + " drivers near " + chosenPickup + " \u00B7 nearest first");
        adapter.submit(result);
        adapter.setDistances(dist);
        b.empty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
        showPanel(Panel.RESULTS);
    }

    private double[] target() {
        String p = chosenPickup == null ? "" : chosenPickup.toLowerCase(Locale.getDefault());
        if (p.contains("current") && yourLocation != null) return yourLocation;
        double[] c = Geo.byName(chosenPickup);
        return c != null ? c : Geo.HOMESTAY;
    }

    private void useMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            readLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }
    }

    private void readLocation() {
        yourLocation = Geo.myLocation(this);
        b.pickup.setText("Current location", false);
        Ui.toast(this, yourLocation != null
                ? "Using your current location"
                : "Using homestay location (GPS unavailable)");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_LOCATION) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            readLocation();
        } else {
            b.pickup.setText(DataStore.get().homestay.name, false);
            Ui.toast(this, "Location permission denied \u00B7 using homestay pickup");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DataStore.get().sync(this::noop);
    }

    private void noop() {
    }

    private void showProfile(Models.Driver driver) {
        DialogDriverProfileBinding binding = DialogDriverProfileBinding.inflate(getLayoutInflater());
        binding.avatarText.setText(initials(driver.name));
        binding.name.setText(driver.name);
        binding.rating.setText(String.format(Locale.getDefault(), "\u2605 %.1f rating", driver.rating));
        binding.vehicle.setText(driver.vehicle + " \u00B7 " + driver.vehicleType + " \u00B7 "
                + driver.seats + " seats");
        binding.area.setText(driver.area);
        StringBuilder trips = new StringBuilder();
        for (int i = 0; i < driver.trips.size(); i++) {
            if (i > 0) trips.append(" \u00B7 ");
            trips.append(driver.trips.get(i));
        }
        binding.trips.setText(trips.length() == 0 ? "Not set" : trips.toString());
        binding.phone.setText(driver.phone);
        if (driver.available) {
            Ui.pill(binding.availability, "AVAILABLE NOW",
                    R.color.status_green_bg, R.color.status_green);
        } else {
            Ui.pill(binding.availability, "ON A TRIP",
                    R.color.status_orange_bg, R.color.status_orange);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(binding.getRoot())
                .create();
        binding.btnRequest.setText("Book now");
        binding.btnRequest.setOnClickListener(v -> {
            dialog.dismiss();
            DriverBooking.start(this, driver, chosenPickup);
        });
        binding.btnCall.setOnClickListener(v -> Ui.toast(this,
                "Calling " + driver.name + " \u00B7 " + driver.phone + " (demo)"));
        binding.btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.getDefault());
    }

    private String text(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
