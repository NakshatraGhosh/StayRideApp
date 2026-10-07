package com.example.stayride.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stayride.data.Models;
import com.example.stayride.databinding.ItemRoomBinding;
import com.example.stayride.util.Ui;

import java.util.ArrayList;
import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.VH> {

    public interface Listener {
        void onClick(Models.Room room);
    }

    private final List<Models.Room> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Models.Room> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemRoomBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.Room room = items.get(position);
        holder.b.number.setText("Room " + room.number);
        holder.b.meta.setText(room.type + " \u00B7 " + room.floor);
        holder.b.price.setText(Ui.money(room.price) + " / night");
        Ui.styleRoomStatus(holder.b.status, room.status);
        holder.b.card.setOnClickListener(v -> {
            if (listener != null) listener.onClick(room);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemRoomBinding b;

        VH(ItemRoomBinding binding) {
            super(binding.getRoot());
            b = binding;
        }
    }
}
