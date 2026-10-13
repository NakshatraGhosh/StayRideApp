package com.example.stayride.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stayride.R;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ItemDriverBinding;
import com.example.stayride.util.Ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DriverAdapter extends RecyclerView.Adapter<DriverAdapter.VH> {

    public interface Listener {
        void onDriverClick(Models.Driver driver);

        void onEdit(Models.Driver driver);

        void onDelete(Models.Driver driver);

        void onActiveChanged(Models.Driver driver, boolean active);

        void onBook(Models.Driver driver);
    }

    private final List<Models.Driver> items = new ArrayList<>();
    private final boolean ownerMode;
    private Listener listener;
    private Map<Integer, Double> distances = new HashMap<>();

    public DriverAdapter(boolean ownerMode) {
        this.ownerMode = ownerMode;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Models.Driver> data) {
        items.clear();
        items.addAll(data);
        distances = new HashMap<>();
        notifyDataSetChanged();
    }

    public void setDistances(Map<Integer, Double> data) {
        distances = data == null ? new HashMap<>() : data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemDriverBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.Driver d = items.get(position);
        holder.b.name.setText(d.name);
        holder.b.rating.setText(String.format(Locale.getDefault(), "%.1f", d.rating));
        holder.b.vehicle.setText(d.vehicle + " \u00B7 " + d.vehicleType + " \u00B7 " + d.seats + " seats");
        holder.b.area.setText(d.area);
        StringBuilder trips = new StringBuilder();
        for (int i = 0; i < d.trips.size(); i++) {
            if (i > 0) trips.append(" \u00B7 ");
            trips.append(d.trips.get(i));
        }
        holder.b.trips.setText(trips.toString());

        Double dist = distances.get(d.id);
        if (dist != null) {
            holder.b.distance.setVisibility(View.VISIBLE);
            holder.b.distance.setText(String.format(Locale.getDefault(), "%.1f km away", dist));
        } else {
            holder.b.distance.setVisibility(View.GONE);
        }

        if (d.available && d.active) {
            Ui.pill(holder.b.status, "AVAILABLE", R.color.status_green_bg, R.color.status_green);
        } else if (!d.active) {
            Ui.pill(holder.b.status, "INACTIVE", R.color.status_grey_bg, R.color.status_grey);
        } else {
            Ui.pill(holder.b.status, "ON TRIP", R.color.status_orange_bg, R.color.status_orange);
        }

        if (ownerMode) {
            holder.b.activeSwitch.setVisibility(View.VISIBLE);
            holder.b.btnEdit.setVisibility(View.VISIBLE);
            holder.b.btnDelete.setVisibility(View.VISIBLE);
            holder.b.activeSwitch.setOnCheckedChangeListener(null);
            holder.b.activeSwitch.setChecked(d.active);
            holder.b.activeSwitch.setOnCheckedChangeListener((btn, checked) -> {
                if (listener != null) listener.onActiveChanged(d, checked);
            });
            holder.b.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(d);
            });
            holder.b.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(d);
            });
        } else {
            holder.b.activeSwitch.setVisibility(View.GONE);
            holder.b.btnEdit.setVisibility(View.GONE);
            holder.b.btnDelete.setVisibility(View.GONE);
        }

        holder.b.btnBook.setOnClickListener(v -> {
            if (listener != null) listener.onBook(d);
        });

        holder.b.card.setOnClickListener(v -> {
            if (listener != null) listener.onDriverClick(d);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemDriverBinding b;

        VH(ItemDriverBinding binding) {
            super(binding.getRoot());
            b = binding;
        }
    }
}
