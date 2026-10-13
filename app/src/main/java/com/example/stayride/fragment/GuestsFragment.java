package com.example.stayride.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.stayride.R;
import com.example.stayride.activity.GuestDetailActivity;
import com.example.stayride.activity.NewGuestActivity;
import com.example.stayride.adapter.GuestAdapter;
import com.example.stayride.data.DataStore;
import com.example.stayride.databinding.FragmentGuestsBinding;

public class GuestsFragment extends Fragment {

    private FragmentGuestsBinding b;
    private final GuestAdapter adapter = new GuestAdapter();
    private String query = "";
    private String filter = "ALL";
    private int periodDays = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentGuestsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        b.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.list.setAdapter(adapter);
        adapter.setListener(guest -> {
            Intent i = new Intent(requireContext(), GuestDetailActivity.class);
            i.putExtra("guest_id", guest.id);
            startActivity(i);
        });

        b.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s == null ? "" : s.toString();
                refresh();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        b.fAll.setOnClickListener(v -> { filter = "ALL"; refresh(); });
        b.fNew.setOnClickListener(v -> { filter = "NEW"; refresh(); });
        b.fRepeat.setOnClickListener(v -> { filter = "REPEAT"; refresh(); });

        b.periodFilter.setText(periodLabel(periodDays));
        b.periodFilter.setOnClickListener(this::showPeriodMenu);

        b.fab.setOnClickListener(v -> startActivity(new Intent(requireContext(), NewGuestActivity.class)));
    }

    private void showPeriodMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenuInflater().inflate(R.menu.menu_guest_period, menu.getMenu());
        int checkId = periodDays == 7 ? R.id.g7
                : periodDays == 30 ? R.id.g30
                : periodDays == 180 ? R.id.g180
                : periodDays == 365 ? R.id.g365 : R.id.gAll;
        menu.getMenu().findItem(checkId).setChecked(true);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.g7) periodDays = 7;
            else if (item.getItemId() == R.id.g30) periodDays = 30;
            else if (item.getItemId() == R.id.g180) periodDays = 180;
            else if (item.getItemId() == R.id.g365) periodDays = 365;
            else periodDays = 0;
            if (b != null) b.periodFilter.setText(periodLabel(periodDays));
            refresh();
            return true;
        });
        menu.show();
    }

    private String periodLabel(int days) {
        switch (days) {
            case 7: return "Last 7 days";
            case 30: return "Last 30 days";
            case 180: return "Last 6 months";
            case 365: return "Last 1 year";
            default: return "All time";
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        DataStore.get().sync(this::refresh);
    }

    private void refresh() {
        if (b == null) return;
        DataStore d = DataStore.get();
        adapter.submit(d.searchGuests(query, filter, periodDays));
        b.count.setText(adapter.getItemCount() + " showing \u00B7 " + d.guests.size() + " guests \u00B7 "
                + d.countGuests(true) + " repeat \u00B7 " + d.countGuests(false) + " new");
        b.empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
