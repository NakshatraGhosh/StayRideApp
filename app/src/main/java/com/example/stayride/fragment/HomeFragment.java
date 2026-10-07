package com.example.stayride.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stayride.MainActivity;
import com.example.stayride.R;
import com.example.stayride.activity.CabRequestDetailActivity;
import com.example.stayride.activity.CampaignActivity;
import com.example.stayride.activity.FindDriverActivity;
import com.example.stayride.activity.NewBookingActivity;
import com.example.stayride.activity.NewGuestActivity;
import com.example.stayride.adapter.BookingAdapter;
import com.example.stayride.adapter.TripAdapter;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.FragmentHomeBinding;
import com.example.stayride.databinding.ItemQuickActionBinding;
import com.example.stayride.databinding.ItemStatBinding;
import com.example.stayride.util.Charts;
import com.example.stayride.util.Ui;
import com.example.stayride.views.LineChartView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding b;
    private StatAdapter statAdapter;
    private QuickAdapter quickAdapter;
    private final BookingAdapter bookingAdapter = new BookingAdapter();
    private final BookingAdapter upcomingAdapter = new BookingAdapter();
    private final TripAdapter tripAdapter = new TripAdapter();
    private int period = Charts.PERIOD_30D;
    private int metric = Charts.METRIC_REVENUE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentHomeBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        statAdapter = new StatAdapter();
        quickAdapter = new QuickAdapter();

        b.statsGrid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        b.statsGrid.setAdapter(statAdapter);
        b.quickActions.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        b.quickActions.setAdapter(quickAdapter);

        bookingAdapter.setListener(book -> openBooking(book));
        upcomingAdapter.setListener(book -> openBooking(book));
        tripAdapter.setListener(trip -> openTrip(trip));

        b.recentBookings.setAdapter(bookingAdapter);
        b.upcoming.setAdapter(upcomingAdapter);
        b.recentTrips.setAdapter(tripAdapter);

        b.seeBookings.setOnClickListener(v -> ((MainActivity) requireActivity()).selectTab(R.id.nav_rooms));
        b.seeRooms.setOnClickListener(v -> ((MainActivity) requireActivity()).selectTab(R.id.nav_rooms));
        b.seeCabs.setOnClickListener(v -> ((MainActivity) requireActivity()).selectTab(R.id.nav_cabs));

        b.btnPeriod.setText(Charts.periodLabel(period));
        b.btnPeriod.setOnClickListener(this::showPeriodMenu);
        b.metricChips.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipBookings) metric = Charts.METRIC_BOOKINGS;
            else if (checkedId == R.id.chipCabRides) metric = Charts.METRIC_CAB_RIDES;
            else if (checkedId == R.id.chipCommission) metric = Charts.METRIC_COMMISSION;
            else metric = Charts.METRIC_REVENUE;
            renderChart();
        });
        renderChart();
    }

    private void showPeriodMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenuInflater().inflate(R.menu.menu_period, menu.getMenu());
        int checkId = period == Charts.PERIOD_7D ? R.id.p7
                : period == Charts.PERIOD_30D ? R.id.p30
                : period == Charts.PERIOD_6M ? R.id.p180 : R.id.p365;
        menu.getMenu().findItem(checkId).setChecked(true);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.p7) period = Charts.PERIOD_7D;
            else if (item.getItemId() == R.id.p30) period = Charts.PERIOD_30D;
            else if (item.getItemId() == R.id.p180) period = Charts.PERIOD_6M;
            else period = Charts.PERIOD_12M;
            b.btnPeriod.setText(Charts.periodLabel(period));
            renderChart();
            return true;
        });
        menu.show();
    }

    private void renderChart() {
        if (b == null) return;
        Charts.Series s = Charts.build(metric, period);
        List<LineChartView.Point> pts = new ArrayList<>();
        for (int i = 0; i < s.values.size(); i++) {
            pts.add(new LineChartView.Point(s.labels.get(i), s.values.get(i)));
        }
        b.perfChart.setPoints(pts);
        b.kpiTotal.setText(s.money ? Ui.money(s.total) : String.valueOf(s.total));
        b.kpiPeak.setText(s.money ? Ui.money(s.peak) : String.valueOf(s.peak));
        b.kpiChange.setText((s.changePct > 0 ? "+" : "") + s.changePct + "%");
        b.kpiChange.setTextColor(ContextCompat.getColor(requireContext(),
                s.changePct >= 0 ? R.color.status_green : R.color.status_red));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        DataStore d = DataStore.get();
        b.headerTitle.setText(d.homestay.name);
        b.headerSub.setText("Owner dashboard \u00B7 " + d.homestay.location.split(",")[0]);

        List<Stat> stats = new ArrayList<>();
        stats.add(new Stat(R.drawable.ic_calendar, String.valueOf(d.todayBookings().size()), "Today's bookings",
                () -> ((MainActivity) requireActivity()).selectTab(R.id.nav_rooms)));
        stats.add(new Stat(R.drawable.ic_people, String.valueOf(d.currentGuestCount()), "Current guests",
                () -> ((MainActivity) requireActivity()).selectTab(R.id.nav_guests)));
        stats.add(new Stat(R.drawable.ic_bed, String.valueOf(d.countRooms("AVAILABLE")), "Available rooms",
                () -> ((MainActivity) requireActivity()).selectTab(R.id.nav_rooms)));
        stats.add(new Stat(R.drawable.ic_payments, Ui.money(d.monthlyRevenue()), "Room revenue this month",
                () -> startActivity(new Intent(requireContext(), com.example.stayride.activity.AnalyticsActivity.class))));
        stats.add(new Stat(R.drawable.ic_car, String.valueOf(d.trips.size()), "Cab bookings",
                () -> ((MainActivity) requireActivity()).selectTab(R.id.nav_cabs)));
        stats.add(new Stat(R.drawable.ic_chart, Ui.money(d.commissionEarned()), "Cab commission earned",
                () -> startActivity(new Intent(requireContext(), com.example.stayride.activity.CommissionActivity.class))));
        stats.add(new Stat(R.drawable.ic_clock, Ui.money(d.commissionPending()), "Pending commissions",
                () -> startActivity(new Intent(requireContext(), com.example.stayride.activity.CommissionActivity.class))));
        stats.add(new Stat(R.drawable.ic_home, d.occupancyPercent() + "%", "Occupancy",
                () -> startActivity(new Intent(requireContext(), com.example.stayride.activity.AnalyticsActivity.class))));
        statAdapter.submit(stats);

        List<Action> actions = new ArrayList<>();
        actions.add(new Action(R.drawable.ic_add, "Add Booking", () ->
                startActivity(new Intent(requireContext(), NewBookingActivity.class))));
        actions.add(new Action(R.drawable.ic_bed, "Manage Rooms", () ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_rooms)));
        actions.add(new Action(R.drawable.ic_car, "Find Driver", () ->
                startActivity(new Intent(requireContext(), FindDriverActivity.class))));
        actions.add(new Action(R.drawable.ic_route, "View Cab Requests", () ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_cabs)));
        actions.add(new Action(R.drawable.ic_people, "Add Guest", () ->
                startActivity(new Intent(requireContext(), NewGuestActivity.class))));
        actions.add(new Action(R.drawable.ic_send, "Create Campaign", () -> {
            Intent i = new Intent(requireContext(), CampaignActivity.class);
            i.putExtra("create", true);
            startActivity(i);
        }));
        quickAdapter.submit(actions);

        bookingAdapter.submit(d.recentBookings(4));
        upcomingAdapter.submit(d.upcomingCheckIns(3));
        tripAdapter.submit(d.recentTrips(3));
        renderChart();
    }

    private void openBooking(Models.Booking booking) {
        Intent i = new Intent(requireContext(), com.example.stayride.activity.BookingDetailActivity.class);
        i.putExtra("booking_id", booking.id);
        startActivity(i);
    }

    private void openTrip(Models.CabTrip trip) {
        Intent i = new Intent(requireContext(), CabRequestDetailActivity.class);
        i.putExtra("trip_id", trip.id);
        startActivity(i);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    static class Stat {
        final int icon;
        final String value;
        final String label;
        final Runnable run;

        Stat(int icon, String value, String label, Runnable run) {
            this.icon = icon;
            this.value = value;
            this.label = label;
            this.run = run;
        }
    }

    static class Action {
        final int icon;
        final String label;
        final Runnable run;

        Action(int icon, String label, Runnable run) {
            this.icon = icon;
            this.label = label;
            this.run = run;
        }
    }

    class StatAdapter extends RecyclerView.Adapter<StatAdapter.VH> {
        private final List<Stat> items = new ArrayList<>();

        void submit(List<Stat> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemStatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Stat s = items.get(position);
            holder.b.icon.setImageResource(s.icon);
            holder.b.value.setText(s.value);
            holder.b.label.setText(s.label);
            holder.b.card.setOnClickListener(v -> {
                if (s.run != null) s.run.run();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemStatBinding b;

            VH(ItemStatBinding binding) {
                super(binding.getRoot());
                b = binding;
            }
        }
    }

    class QuickAdapter extends RecyclerView.Adapter<QuickAdapter.VH> {
        private final List<Action> items = new ArrayList<>();

        void submit(List<Action> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemQuickActionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Action a = items.get(position);
            holder.b.icon.setImageResource(a.icon);
            holder.b.label.setText(a.label);
            holder.b.card.setOnClickListener(v -> a.run.run());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemQuickActionBinding b;

            VH(ItemQuickActionBinding binding) {
                super(binding.getRoot());
                b = binding;
            }
        }
    }
}
