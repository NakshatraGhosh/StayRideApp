package com.example.stayride.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stayride.R;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ItemGuestBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Ui;

import java.util.ArrayList;
import java.util.List;

public class GuestAdapter extends RecyclerView.Adapter<GuestAdapter.VH> {

    public interface Listener {
        void onClick(Models.Guest guest);
    }

    private final List<Models.Guest> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Models.Guest> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemGuestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.Guest g = items.get(position);
        holder.b.name.setText(g.name);
        holder.b.phone.setText(g.phone);
        String meta = g.visits + (g.visits == 1 ? " visit \u00B7 " : " visits \u00B7 ")
                + Ui.money(g.totalSpending) + " spent \u00B7 Last: " + Dates.fmt(g.lastVisit);
        holder.b.meta.setText(meta);
        if (g.isRepeat()) {
            Ui.pill(holder.b.badge, "REPEAT", R.color.status_purple_bg, R.color.status_purple);
        } else {
            Ui.pill(holder.b.badge, "NEW", R.color.status_green_bg, R.color.status_green);
        }
        holder.b.card.setOnClickListener(v -> {
            if (listener != null) listener.onClick(g);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemGuestBinding b;

        VH(ItemGuestBinding binding) {
            super(binding.getRoot());
            b = binding;
        }
    }
}
