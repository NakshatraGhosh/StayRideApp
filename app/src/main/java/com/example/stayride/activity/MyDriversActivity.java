package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.stayride.R;
import com.example.stayride.adapter.DriverAdapter;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityMyDriversBinding;
import com.example.stayride.databinding.DialogDriverFormBinding;
import com.example.stayride.util.Sim;
import com.example.stayride.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class MyDriversActivity extends BaseActivity {

    private ActivityMyDriversBinding b;
    private final DriverAdapter adapter = new DriverAdapter(true);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMyDriversBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("My Drivers");

        b.list.setLayoutManager(new LinearLayoutManager(this));
        b.list.setAdapter(adapter);
        adapter.setListener(new DriverAdapter.Listener() {
            @Override
            public void onDriverClick(Models.Driver driver) {
                showForm(driver);
            }

            @Override
            public void onEdit(Models.Driver driver) {
                showForm(driver);
            }

            @Override
            public void onDelete(Models.Driver driver) {
                confirmDelete(driver);
            }

            @Override
            public void onActiveChanged(Models.Driver driver, boolean active) {
                driver.active = active;
                Ui.toast(MyDriversActivity.this, driver.name + (active ? " activated" : " deactivated"));
                refresh();
            }
        });

        b.fab.setOnClickListener(v -> showForm(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        DataStore d = DataStore.get();
        adapter.submit(d.drivers);
        int active = 0;
        int available = 0;
        for (Models.Driver driver : d.drivers) {
            if (driver.active) active++;
            if (driver.active && driver.available) available++;
        }
        b.count.setText(d.drivers.size() + " drivers \u00B7 " + active + " active \u00B7 "
                + available + " available now");
        b.empty.setVisibility(d.drivers.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void showForm(Models.Driver existing) {
        DialogDriverFormBinding f = DialogDriverFormBinding.inflate(LayoutInflater.from(this));
        f.editType.setSimpleItems(new String[]{"SUV", "Sedan", "7-seater", "9-seater"});
        Ui.wireDropdown(f.editType);
        if (existing != null) {
            f.editName.setText(existing.name);
            f.editPhone.setText(existing.phone);
            f.editVehicle.setText(existing.vehicle);
            f.editType.setText(existing.vehicleType, false);
            f.editSeats.setText(String.valueOf(existing.seats));
            f.editArea.setText(existing.area);
            f.availableCheck.setChecked(existing.available);
            f.tNjp.setChecked(existing.supports("NJP"));
            f.tBagdogra.setChecked(existing.supports("Bagdogra"));
            f.tSightseeing.setChecked(existing.supports("Sightseeing"));
            f.tLocal.setChecked(existing.supports("Local"));
        } else {
            f.editType.setText("SUV", false);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? "Add your driver" : "Edit driver")
                .setView(f.getRoot())
                .setPositiveButton(existing == null ? "Add driver" : "Save",
                        (dialog, which) -> save(existing, f))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void save(Models.Driver existing, DialogDriverFormBinding f) {
        String name = text(f.editName.getText());
        String phone = text(f.editPhone.getText());
        String vehicle = text(f.editVehicle.getText());
        if (TextUtils.isEmpty(name)) {
            f.editName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            f.inputPhone.setError("Phone is required");
            return;
        }
        if (TextUtils.isEmpty(vehicle)) {
            f.editVehicle.setError("Vehicle is required");
            return;
        }
        int seats = 4;
        try {
            seats = Integer.parseInt(text(f.editSeats.getText()));
        } catch (NumberFormatException ignored) {
        }

        List<String> trips = new ArrayList<>();
        for (int id : f.tripGroup.getCheckedChipIds()) {
            if (id == R.id.tNjp) trips.add("NJP");
            else if (id == R.id.tBagdogra) trips.add("Bagdogra");
            else if (id == R.id.tSightseeing) trips.add("Sightseeing");
            else if (id == R.id.tLocal) trips.add("Local");
        }

        String type = f.editType.getText() == null ? "SUV" : f.editType.getText().toString().trim();
        if (type.isEmpty()) type = "SUV";
        String area = text(f.editArea.getText());

        DataStore d = DataStore.get();
        if (existing == null) {
            Models.Driver driver = new Models.Driver(d.nextDriverId++, name);
            driver.phone = phone;
            driver.vehicle = vehicle;
            driver.vehicleType = type;
            driver.seats = seats;
            driver.area = area.isEmpty() ? "Gangtok" : area;
            driver.trips = trips;
            driver.available = f.availableCheck.isChecked();
            driver.active = true;
            driver.ownerDriver = true;
            driver.rating = 4.5f;
            d.drivers.add(driver);
            Ui.toast(this, name + " added to your driver team");
        } else {
            existing.name = name;
            existing.phone = phone;
            existing.vehicle = vehicle;
            existing.vehicleType = type;
            existing.seats = seats;
            existing.area = area;
            existing.trips = trips;
            existing.available = f.availableCheck.isChecked();
            Ui.toast(this, name + " updated");
        }
        refresh();
    }

    private void confirmDelete(Models.Driver driver) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove " + driver.name + "?")
                .setMessage("He will no longer be offered to guests for cab bookings.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    DataStore.get().drivers.remove(driver);
                    Ui.toast(this, driver.name + " removed");
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String text(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
