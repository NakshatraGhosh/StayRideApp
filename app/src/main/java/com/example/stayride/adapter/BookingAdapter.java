package com.example.stayride.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stayride.R;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ItemBookingBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Ui;

import java.util.ArrayList;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.VH> {

    public interface Listener {
        void onClick(Models.Booking booking);
    }

    private final List<Models.Booking> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Models.Booking> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemBookingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.Booking b = items.get(position);
        holder.b.name.setText(b.guestName);
        holder.b.route.setText("Room " + b.roomNumber + " \u00B7 " + b.guests + (b.guests == 1 ? " guest" : " guests"));
        holder.b.dates.setText(Dates.fmt(b.checkIn) + " \u2192 " + Dates.fmt(b.checkOut) + " \u00B7 " + b.nights() + "N");
        holder.b.amount.setText(Ui.money(b.amount));
        Ui.styleBookingStatus(holder.b.status, b.status);
        if (b.paid) {
            Ui.pill(holder.b.payment, "PAID", R.color.status_green_bg, R.color.status_green);
        } else {
            Ui.pill(holder.b.payment, "UNPAID", R.color.status_orange_bg, R.color.status_orange);
        }
        holder.b.card.setOnClickListener(v -> {
            if (listener != null) listener.onClick(b);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemBookingBinding b;

        VH(ItemBookingBinding binding) {
            super(binding.getRoot());
            b = binding;
        }
    }
}
