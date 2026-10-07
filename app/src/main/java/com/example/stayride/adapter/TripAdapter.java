package com.example.stayride.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stayride.data.Models;
import com.example.stayride.databinding.ItemTripBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Ui;

import java.util.ArrayList;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.VH> {

    public interface Listener {
        void onClick(Models.CabTrip trip);
    }

    private final List<Models.CabTrip> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Models.CabTrip> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemTripBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.CabTrip t = items.get(position);
        holder.b.name.setText(t.guestName);
        holder.b.route.setText(t.route());
        holder.b.meta.setText(Dates.fmt(t.date) + " \u00B7 " + t.time + " \u00B7 "
                + t.passengers + " passengers \u00B7 " + t.vehicleType);
        if (t.isCompleted()) {
            holder.b.fare.setText("Fare " + Ui.money(t.fare) + " \u00B7 Commission " + Ui.money(t.commission));
        } else {
            holder.b.fare.setText("Est. " + Ui.money(t.estFare));
        }
        holder.b.driver.setText(t.driverName == null ? "No driver yet" : t.driverName
                + (t.ownerDriver ? " (yours)" : ""));
        Ui.styleTripStatus(holder.b.status, t.status);
        holder.b.card.setOnClickListener(v -> {
            if (listener != null) listener.onClick(t);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemTripBinding b;

        VH(ItemTripBinding binding) {
            super(binding.getRoot());
            b = binding;
        }
    }
}
