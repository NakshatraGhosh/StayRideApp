package com.example.stayride.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stayride.R;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ItemCampaignBinding;
import com.example.stayride.util.Ui;

import java.util.ArrayList;
import java.util.List;

public class CampaignAdapter extends RecyclerView.Adapter<CampaignAdapter.VH> {

    public interface Listener {
        void onClick(Models.Campaign campaign);
    }

    private final List<Models.Campaign> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Models.Campaign> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemCampaignBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.Campaign c = items.get(position);
        holder.b.title.setText(c.title);
        holder.b.audience.setText(c.audience + " \u00B7 " + c.recipients + " recipients");
        switch (c.status) {
            case "CREATED":
                holder.b.progress.setText("Tap to start sending on WhatsApp");
                break;
            case "SENDING":
                holder.b.progress.setText("Sending \u2026 " + c.sent + "/" + c.recipients);
                break;
            default:
                holder.b.progress.setText("Sent " + c.sent + " \u00B7 Failed " + c.failed);
                break;
        }
        Ui.styleCampaignStatus(holder.b.status, c.status);
        holder.b.card.setOnClickListener(v -> {
            if (listener != null) listener.onClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemCampaignBinding b;

        VH(ItemCampaignBinding binding) {
            super(binding.getRoot());
            b = binding;
        }
    }
}
