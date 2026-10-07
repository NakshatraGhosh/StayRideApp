package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stayride.R;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityCommissionBinding;
import com.example.stayride.databinding.ItemCommissionBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Ui;

import java.util.ArrayList;
import java.util.List;

public class CommissionActivity extends BaseActivity {

    private ActivityCommissionBinding b;
    private final CommissionAdapter adapter = new CommissionAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityCommissionBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Commission");

        b.list.setLayoutManager(new LinearLayoutManager(this));
        b.list.setAdapter(adapter);
        adapter.onToggle = trip -> {
            trip.commissionPaid = !trip.commissionPaid;
            Ui.toast(this, trip.commissionPaid
                    ? "Marked as paid \u2014 " + Ui.money(trip.commission)
                    : "Moved back to pending \u2014 " + Ui.money(trip.commission));
            refresh();
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        DataStore d = DataStore.get();
        b.balance.setText(Ui.money(d.commissionPending()));
        b.summary.setText("Earned " + Ui.money(d.commissionEarned()) + " \u00B7 Paid "
                + Ui.money(d.commissionPaid()) + " \u00B7 12% platform fee on every completed fare");

        List<Models.CabTrip> rows = new ArrayList<>();
        for (Models.CabTrip t : d.trips) {
            if (t.isCompleted() && t.commission > 0) rows.add(t);
        }
        adapter.submit(rows);
        b.empty.setVisibility(rows.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private static class CommissionAdapter extends RecyclerView.Adapter<CommissionAdapter.VH> {

        interface OnToggle {
            void onToggle(Models.CabTrip trip);
        }

        OnToggle onToggle;
        private final List<Models.CabTrip> items = new ArrayList<>();

        void submit(List<Models.CabTrip> rows) {
            items.clear();
            items.addAll(rows);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemCommissionBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Models.CabTrip t = items.get(position);
            ItemCommissionBinding ib = holder.binding;
            ib.title.setText(t.guestName + " \u00B7 " + t.route());
            ib.meta.setText(Dates.fmt(t.date) + " \u00B7 " + t.vehicleType + " \u00B7 fare "
                    + Ui.money(t.fare));
            ib.commission.setText("+" + Ui.money(t.commission));
            Ui.pill(ib.state, t.commissionPaid ? "PAID" : "PENDING",
                    t.commissionPaid ? R.color.status_green_bg : R.color.status_orange_bg,
                    t.commissionPaid ? R.color.status_green : R.color.status_orange);
            ib.getRoot().setOnClickListener(v -> {
                if (onToggle != null) onToggle.onToggle(t);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ItemCommissionBinding binding;

            VH(ItemCommissionBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
