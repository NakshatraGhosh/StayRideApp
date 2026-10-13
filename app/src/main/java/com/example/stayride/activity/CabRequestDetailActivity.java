package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stayride.R;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityCabRequestDetailBinding;
import com.example.stayride.databinding.ItemAssignDriverBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Sim;
import com.example.stayride.util.Ui;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class CabRequestDetailActivity extends BaseActivity {

    private ActivityCabRequestDetailBinding b;
    private Models.CabTrip trip;
    private int tripId;
    private String searchMode = "idle";
    private String overrideStatus;
    private Runnable pending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityCabRequestDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Cab request");

        tripId = getIntent().getIntExtra("trip_id", -1);
        trip = DataStore.get().trip(tripId);
        if (trip == null) {
            finish();
            return;
        }

        if ("FINDING_DRIVER".equals(trip.status)) {
            beginOwnSearch();
        } else if ("NEW_REQUEST".equals(trip.status)) {
            searchMode = "idle";
        }
        refresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pending != null) Sim.cancel(pending);
        b = null;
    }

    private String status() {
        return overrideStatus != null ? overrideStatus : trip.status;
    }

    private void refresh() {
        if (b == null) return;
        trip = DataStore.get().trip(tripId);
        if (trip == null) {
            finish();
            return;
        }
        String status = status();

        b.guestName.setText(trip.guestName);
        b.phone.setText(trip.phone);
        b.pickup.setText(trip.pickup);
        b.destination.setText(trip.destination);
        b.when.setText(Dates.fmtLong(trip.date) + " \u00B7 " + trip.time);
        b.pax.setText(trip.passengers + " passengers \u00B7 " + trip.vehicleType);
        if (trip.isCompleted()) {
            b.estFare.setText(Ui.money(trip.fare));
        } else {
            b.estFare.setText("Est. " + Ui.money(trip.estFare));
        }
        b.driverTag.setText(trip.driverName == null
                ? "No driver assigned"
                : trip.driverName + (trip.ownerDriver ? " (your driver)" : " (StayRide)"));
        Ui.styleTripStatus(b.status, status);

        int step = stepOf(status);
        b.stepLabel.setText("Step " + Math.max(step, 1) + " of 6 \u00B7 " + Ui.tripStatusLabel(status));
        b.progress.setProgress(Math.max(step, 1) * 100 / 6);

        renderDriverCard();
        renderCommission();
        renderSearchArea();
        renderActions();
    }

    private int stepOf(String status) {
        switch (status) {
            case "NEW_REQUEST": return 1;
            case "FINDING_DRIVER": return 2;
            case "DRIVER_ASSIGNED": return 3;
            case "CONFIRMED": return 4;
            case "TRIP_STARTED": return 5;
            case "COMPLETED": return 6;
            default: return 0;
        }
    }

    private void renderDriverCard() {
        boolean hasDriver = trip.driverId != null;
        b.driverCard.setVisibility(hasDriver ? View.VISIBLE : View.GONE);
        if (hasDriver) {
            b.driverName.setText(trip.driverName + (trip.ownerDriver ? "  \u00B7  Your driver" : "  \u00B7  StayRide network"));
            Models.Driver d = DataStore.get().driverAny(trip.driverId);
            if (d != null) {
                b.driverMeta.setText(d.vehicle + " \u00B7 " + d.vehicleType + " \u00B7 " + d.seats
                        + " seats \u00B7 " + d.phone + "\n" + d.area);
            } else {
                b.driverMeta.setText("Driver details unavailable");
            }
        }
    }

    private void renderCommission() {
        if (trip.isCompleted()) {
            b.commissionCard.setVisibility(View.VISIBLE);
            b.commissionTitle.setText("Commission earned: " + Ui.money(trip.commission));
            b.commissionMeta.setText("Trip fare " + Ui.money(trip.fare)
                    + " \u00B7 12% StayRide commission \u00B7 "
                    + (trip.commissionPaid ? "Paid to you" : "Pending payout"));
        } else {
            b.commissionCard.setVisibility(View.GONE);
        }
    }

    private void renderSearchArea() {
        b.driversList.removeAllViews();
        String status = status();
        if (!"FINDING_DRIVER".equals(status)) {
            b.searchLabel.setVisibility(View.GONE);
            if (!"COMPLETED".equals(status) && !"CANCELLED".equals(status)) {
                banner("Accept the request to start finding a driver.",
                        R.color.brand_primary_light, R.color.brand_primary_dark);
            } else {
                b.banner.setVisibility(View.GONE);
            }
            if ("DRIVER_ASSIGNED".equals(status)
                    || "CONFIRMED".equals(status)
                    || "TRIP_STARTED".equals(status)) {
                banner("Driver assigned \u00B7 guest confirmation is simulated in this demo.",
                        R.color.status_blue_bg, R.color.status_blue);
            }
            if ("COMPLETED".equals(status)) {
                banner("Trip completed. Your commission has been added below.",
                        R.color.status_green_bg, R.color.status_green);
            }
            if ("CANCELLED".equals(status)) {
                banner("This request was cancelled.", R.color.status_red_bg, R.color.status_red);
            }
            return;
        }

        if ("loading_own".equals(searchMode)) {
            banner("Checking your own drivers first\u2026", R.color.brand_primary_light, R.color.brand_primary_dark);
            b.searchLabel.setVisibility(View.GONE);
        } else if ("loading_network".equals(searchMode)) {
            banner("Finding a driver from the StayRide network\u2026", R.color.status_purple_bg,
                    R.color.status_purple);
            b.searchLabel.setVisibility(View.GONE);
        } else if ("own".equals(searchMode)) {
            banner("Your driver is available.", R.color.status_green_bg, R.color.status_green);
            List<Models.Driver> own = ownMatches();
            b.searchLabel.setVisibility(View.VISIBLE);
            b.searchLabel.setText("Your drivers (" + own.size() + ")");
            for (Models.Driver d : own) addDriverRow(d, true);
        } else {
            banner("Searching 50+ StayRide drivers around you\u2026", R.color.status_purple_bg,
                    R.color.status_purple);
            List<Models.Driver> network = networkMatches();
            b.searchLabel.setVisibility(View.VISIBLE);
            b.searchLabel.setText("StayRide drivers near you (" + network.size() + ")");
            for (Models.Driver d : network) addDriverRow(d, false);
        }
    }

    private void banner(String text, int bg, int fg) {
        b.banner.setVisibility(View.VISIBLE);
        b.banner.setText(text);
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(12 * getResources().getDisplayMetrics().density);
        d.setColor(Ui.color(b.banner, bg));
        b.banner.setBackground(d);
        b.banner.setTextColor(Ui.color(b.banner, fg));
    }

    private List<Models.Driver> ownMatches() {
        List<Models.Driver> exact = DataStore.get().availableOwnerDrivers(trip.vehicleType);
        if (!exact.isEmpty()) return exact;
        return DataStore.get().availableOwnerDrivers(null);
    }

    private List<Models.Driver> networkMatches() {
        List<Models.Driver> exact = DataStore.get().availableNetworkDrivers(trip.vehicleType);
        if (exact.isEmpty()) exact = DataStore.get().availableNetworkDrivers(null);
        return exact.subList(0, Math.min(8, exact.size()));
    }

    private void addDriverRow(Models.Driver driver, boolean own) {
        ItemAssignDriverBinding row = ItemAssignDriverBinding.inflate(
                LayoutInflater.from(this), b.driversList, false);
        row.name.setText(driver.name + (own ? "  (yours)" : ""));
        row.meta.setText(driver.vehicle + " \u00B7 " + driver.seats + " seats \u00B7 "
                + driver.vehicleType + " \u00B7 \u2605 " + driver.rating);
        row.area.setText(driver.area + " \u00B7 " + (own ? driver.phone : String.join(" \u00B7 ", driver.trips)));
        row.btnAssign.setOnClickListener(v -> assignDriver(driver, own));
        b.driversList.addView(row.getRoot());
    }

    private void renderActions() {
        b.actions.removeAllViews();
        switch (status()) {
            case "NEW_REQUEST":
                add("Accept / Find Driver", false, this::accept);
                add("Decline request", true, this::confirmDecline);
                break;
            case "FINDING_DRIVER":
                if ("own".equals(searchMode)) {
                    add("Search StayRide network instead", true,
                            () -> beginNetworkSearch());
                    add("Cancel search", true, this::cancelSearch);
                } else if ("network".equals(searchMode)) {
                    add("Search again", true, () -> beginNetworkSearch());
                    add("Cancel search", true, this::cancelSearch);
                }
                break;
            case "DRIVER_ASSIGNED":
                add("Confirm booking", false, this::confirmBooking);
                add("Change driver", true, this::changeDriver);
                add("Cancel request", true, this::confirmCancel);
                break;
            case "CONFIRMED":
                add("Start trip", false, this::startTrip);
                add("Cancel trip", true, this::confirmCancel);
                break;
            case "TRIP_STARTED":
                add("Complete trip", false, this::completeTrip);
                break;
            case "COMPLETED":
                add("View commission dashboard", true, () ->
                        startActivity(new Intent(this, CommissionActivity.class)));
                break;
            default:
                break;
        }
    }

    private void add(String label, boolean outlined, Runnable action) {
        MaterialButton button = new MaterialButton(this);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
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
        button.setOnClickListener(v -> action.run());
        b.actions.addView(button);
    }

    private void accept() {
        overrideStatus = "FINDING_DRIVER";
        beginOwnSearch();
    }

    private void changeDriver() {
        DataStore.get().reassignTrip(tripId, () -> {
            overrideStatus = null;
            beginOwnSearch();
        });
    }

    private void beginOwnSearch() {
        searchMode = "loading_own";
        refresh();
        if (pending != null) Sim.cancel(pending);
        pending = () -> {
            if (b == null) return;
            List<Models.Driver> own = ownMatches();
            if (own.isEmpty()) {
                beginNetworkSearch();
            } else {
                searchMode = "own";
                refresh();
            }
        };
        Sim.delay(1200, pending);
    }

    private void beginNetworkSearch() {
        searchMode = "loading_network";
        refresh();
        if (pending != null) Sim.cancel(pending);
        pending = () -> {
            if (b == null) return;
            searchMode = "network";
            refresh();
        };
        Sim.delay(1600, pending);
    }

    private void assignDriver(Models.Driver driver, boolean own) {
        DataStore.get().assignTripDriver(tripId, driver.id, () -> {
            overrideStatus = null;
            searchMode = "idle";
            Ui.toast(this, driver.name + " assigned to " + trip.guestName);
            refresh();
        });
    }

    private void confirmBooking() {
        DataStore.get().acceptTrip(tripId, () -> {
            refresh();
            if (b != null) {
                Snackbar.make(b.getRoot(), "Confirmation sent to " + trip.guestName + " (simulated)",
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void startTrip() {
        DataStore.get().startTrip(tripId, () -> {
            Ui.toast(this, "Trip started \u2014 driver notified (demo)");
            refresh();
        });
    }

    private void completeTrip() {
        DataStore.get().completeTrip(tripId, () -> {
            refresh();
            Ui.toast(this, "Commission Earned: " + Ui.money(trip.commission));
        });
    }

    private void cancelSearch() {
        overrideStatus = null;
        searchMode = "idle";
        refresh();
    }

    private void confirmDecline() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Decline this request?")
                .setMessage(trip.guestName + " will be told that no cab is available right now (demo).")
                .setPositiveButton("Decline", (dialog, which) -> {
                    DataStore.get().cancelTrip(tripId, () -> {
                        overrideStatus = null;
                        searchMode = "idle";
                        Ui.toast(this, "Request declined");
                        refresh();
                    });
                })
                .setNegativeButton("Keep request", null)
                .show();
    }

    private void confirmCancel() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel this cab request?")
                .setMessage("The guest will be notified that no cab could be arranged (simulated).")
                .setPositiveButton("Cancel request", (dialog, which) -> {
                    DataStore.get().cancelTrip(tripId, () -> {
                        overrideStatus = null;
                        searchMode = "idle";
                        Ui.toast(this, "Request cancelled");
                        refresh();
                    });
                })
                .setNegativeButton("Keep request", null)
                .show();
    }
}
