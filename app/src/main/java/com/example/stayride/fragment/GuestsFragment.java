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

        b.fab.setOnClickListener(v -> startActivity(new Intent(requireContext(), NewGuestActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        DataStore d = DataStore.get();
        adapter.submit(d.searchGuests(query, filter));
        b.count.setText(d.guests.size() + " guests \u00B7 " + d.countGuests(true) + " repeat \u00B7 "
                + d.countGuests(false) + " new");
        b.empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
