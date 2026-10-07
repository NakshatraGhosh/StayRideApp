package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.stayride.R;
import com.example.stayride.adapter.DriverAdapter;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityFindDriverBinding;
import com.example.stayride.databinding.DialogDriverProfileBinding;
import com.example.stayride.databinding.DialogRequestSentBinding;
import com.example.stayride.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FindDriverActivity extends BaseActivity {

    private ActivityFindDriverBinding b;
    private final DriverAdapter adapter = new DriverAdapter(false);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String query = "";

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
        });

        b.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s == null ? "" : s.toString();
                refresh();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        b.cAvailable.setOnCheckedChangeListener((btn, checked) -> refresh());
        b.cSuv.setOnCheckedChangeListener((btn, checked) -> refresh());
        b.cSedan.setOnCheckedChangeListener((btn, checked) -> refresh());
        b.c7.setOnCheckedChangeListener((btn, checked) -> refresh());
        b.c9.setOnCheckedChangeListener((btn, checked) -> refresh());
        b.cNjp.setOnCheckedChangeListener((btn, checked) -> refresh());
        b.cBagdogra.setOnCheckedChangeListener((btn, checked) -> refresh());
        b.cSightseeing.setOnCheckedChangeListener((btn, checked) -> refresh());
        b.cLocal.setOnCheckedChangeListener((btn, checked) -> refresh());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private List<String> activeVehicleTypes() {
        List<String> types = new ArrayList<>();
        if (b.cSuv.isChecked()) types.add("SUV");
        if (b.cSedan.isChecked()) types.add("Sedan");
        if (b.c7.isChecked()) types.add("7-seater");
        if (b.c9.isChecked()) types.add("9-seater");
        return types;
    }

    private List<String> activeTrips() {
        List<String> trips = new ArrayList<>();
        if (b.cNjp.isChecked()) trips.add("NJP");
        if (b.cBagdogra.isChecked()) trips.add("Bagdogra");
        if (b.cSightseeing.isChecked()) trips.add("Sightseeing");
        if (b.cLocal.isChecked()) trips.add("Local");
        return trips;
    }

    private void refresh() {
        DataStore d = DataStore.get();
        List<String> types = activeVehicleTypes();
        List<String> trips = activeTrips();
        String q = query.trim().toLowerCase(Locale.getDefault());
        boolean availableOnly = b.cAvailable.isChecked();

        List<Models.Driver> result = new ArrayList<>();
        for (Models.Driver driver : d.networkDrivers) {
            if (availableOnly && !driver.available) continue;
            if (!types.isEmpty() && !types.contains(driver.vehicleType)) continue;
            boolean tripOk = trips.isEmpty();
            for (String t : trips) {
                if (driver.supports(t)) {
                    tripOk = true;
                    break;
                }
            }
            if (!tripOk) continue;
            if (!q.isEmpty()) {
                String hay = (driver.name + " " + driver.vehicle + " " + driver.area + " "
                        + driver.vehicleType).toLowerCase(Locale.getDefault());
                if (!hay.contains(q)) continue;
            }
            result.add(driver);
        }

        int availableCount = 0;
        for (Models.Driver driver : d.networkDrivers) if (driver.available) availableCount++;
        b.count.setText(d.networkDrivers.size() + " StayRide drivers \u00B7 "
                + availableCount + " available now \u00B7 showing " + result.size());
        adapter.submit(result);
        b.empty.setVisibility(result.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
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
        binding.btnRequest.setOnClickListener(v -> {
            dialog.dismiss();
            showRequestSent(driver);
        });
        binding.btnCall.setOnClickListener(v -> Ui.toast(this,
                "Calling " + driver.name + " \u00B7 " + driver.phone + " (demo)"));
        binding.btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showRequestSent(Models.Driver driver) {
        DialogRequestSentBinding binding = DialogRequestSentBinding.inflate(getLayoutInflater());
        binding.message.setText("Your request was sent to " + driver.name
                + ". He will confirm shortly (demo).");

        binding.check.setTranslationY(28f);
        binding.title.setTranslationY(28f);
        binding.message.setTranslationY(28f);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(binding.getRoot())
                .create();
        binding.btnDone.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d -> handler.removeCallbacksAndMessages(null));
        dialog.show();

        Runnable reveal = () -> {
            binding.check.animate().alpha(1f).translationY(0f).setDuration(300).start();
            binding.title.animate().alpha(1f).translationY(0f)
                    .setStartDelay(120).setDuration(300).start();
            binding.message.animate().alpha(1f).translationY(0f)
                    .setStartDelay(240).setDuration(300).start();
        };

        binding.scene.post(() -> {
            float sceneW = binding.scene.getWidth();
            if (sceneW <= 0) {
                reveal.run();
                return;
            }
            float half = (sceneW + binding.cab.getWidth()) / 2f;
            binding.cab.setTranslationX(-half);
            binding.cab.animate()
                    .translationX(half)
                    .setDuration(1500)
                    .setInterpolator(new LinearInterpolator())
                    .withEndAction(reveal)
                    .start();
        });

        handler.postDelayed(() -> {
            if (dialog.isShowing()) dialog.dismiss();
        }, 4500);
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.getDefault());
    }
}
