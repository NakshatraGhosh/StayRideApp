package com.example.stayride.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.stayride.R;
import com.example.stayride.activity.CabRequestDetailActivity;
import com.example.stayride.activity.CampaignActivity;
import com.example.stayride.activity.CommissionActivity;
import com.example.stayride.activity.FindDriverActivity;
import com.example.stayride.activity.HomestayProfileActivity;
import com.example.stayride.activity.MyDriversActivity;
import com.example.stayride.adapter.TripAdapter;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.FragmentCabsBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Sim;
import com.example.stayride.util.Ui;

import java.util.List;
import java.util.Random;

public class CabsFragment extends Fragment {

    private FragmentCabsBinding b;
    private final TripAdapter adapter = new TripAdapter();
    private boolean requestsTab = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentCabsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        b.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.list.setAdapter(adapter);
        adapter.setListener(trip -> {
            Intent i = new Intent(requireContext(), CabRequestDetailActivity.class);
            i.putExtra("trip_id", trip.id);
            startActivity(i);
        });

        b.btnFindDriver.setOnClickListener(v -> startActivity(new Intent(requireContext(), FindDriverActivity.class)));
        b.btnMyDrivers.setOnClickListener(v -> startActivity(new Intent(requireContext(), MyDriversActivity.class)));
        b.btnCommission.setOnClickListener(v -> startActivity(new Intent(requireContext(), CommissionActivity.class)));

        b.tabRequests.setOnClickListener(v -> {
            requestsTab = true;
            refresh();
        });
        b.tabTrips.setOnClickListener(v -> {
            requestsTab = false;
            refresh();
        });

        b.simulate.setOnClickListener(v -> simulateRequest());

        b.toggle.check(R.id.tabRequests);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        DataStore d = DataStore.get();
        List<Models.CabTrip> list;
        if (requestsTab) {
            list = d.tripsWithStatus("NEW_REQUEST", "FINDING_DRIVER", "DRIVER_ASSIGNED");
            b.count.setText(list.size() + " open requests \u00B7 "
                    + d.tripsWithStatus("NEW_REQUEST").size() + " new");
            b.empty.setText("No open cab requests. Guests book through your StayRide link.");
        } else {
            list = d.tripsWithStatus("CONFIRMED", "TRIP_STARTED", "COMPLETED");
            b.count.setText(list.size() + " trips \u00B7 " + d.completedTrips() + " completed \u00B7 "
                    + Ui.money(d.commissionEarned()) + " commission");
            b.empty.setText("No trips yet. Accept a request to get started.");
        }
        java.util.Collections.sort(list, (x, y) -> Long.compare(y.date, x.date));
        adapter.submit(list);
        b.empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void simulateRequest() {
        String[][] routes = {
                {"Bagdogra Airport", "Mountain View Homestay"},
                {"NJP Station", "Mountain View Homestay"},
                {"Mountain View Homestay", "Bagdogra Airport"},
                {"Mountain View Homestay", "NJP Station"},
                {"Mountain View Homestay", "Darjeeling Sightseeing"},
                {"Mountain View Homestay", "Gangtok Local Market"}
        };
        String[] names = {"Rohit Ghale", "Sneha Kapoor", "Abhishek Limbu", "Farhan Sheikh", "Divya Menon"};
        String[] types = {"SUV", "Sedan", "7-seater", "9-seater"};
        Random r = new Random();
        String[] route = routes[r.nextInt(routes.length)];

        DataStore d = DataStore.get();
        Models.CabTrip trip = new Models.CabTrip(d.nextTripId++);
        trip.guestName = names[r.nextInt(names.length)];
        trip.phone = "+91 9" + (700000000 + r.nextInt(99999999));
        trip.pickup = route[0];
        trip.destination = route[1];
        trip.date = Dates.plusDays(r.nextInt(5) + 1);
        trip.time = (7 + r.nextInt(11)) + ":" + (r.nextInt(2) == 0 ? "00" : "30") + " AM";
        trip.passengers = 2 + r.nextInt(7);
        trip.vehicleType = types[r.nextInt(types.length)];
        trip.estFare = (1800 + r.nextInt(30) * 100);
        trip.status = "NEW_REQUEST";
        d.trips.add(trip);

        Ui.toast(requireContext(), "New cab request from " + trip.guestName);
        refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
