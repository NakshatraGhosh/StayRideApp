package com.example.stayride.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.stayride.R;
import com.example.stayride.activity.AnalyticsActivity;
import com.example.stayride.activity.CampaignActivity;
import com.example.stayride.activity.CommissionActivity;
import com.example.stayride.activity.FindDriverActivity;
import com.example.stayride.activity.HomestayProfileActivity;
import com.example.stayride.activity.MyDriversActivity;
import com.example.stayride.data.DataStore;
import com.example.stayride.databinding.FragmentMoreBinding;
import com.example.stayride.databinding.ItemLinkBinding;
import com.example.stayride.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MoreFragment extends Fragment {

    private FragmentMoreBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentMoreBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        DataStore d = DataStore.get();
        b.profileName.setText(d.homestay.name);
        b.profileLink.setText(d.homestay.link.replace("https://", ""));
        b.btnOpenProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HomestayProfileActivity.class)));

        addRow(b.rowsTop, R.drawable.ic_chart, "Analytics",
                "Property, guest and cab performance",
                () -> startActivity(new Intent(requireContext(), AnalyticsActivity.class)));
        addRow(b.rowsTop, R.drawable.ic_send, "Guest marketing",
                d.campaigns.size() + " WhatsApp campaigns",
                () -> startActivity(new Intent(requireContext(), CampaignActivity.class)));
        addRow(b.rowsTop, R.drawable.ic_payments, "Cab commission",
                Ui.money(d.commissionEarned()) + " earned \u00B7 " + Ui.money(d.commissionPending()) + " pending",
                () -> startActivity(new Intent(requireContext(), CommissionActivity.class)));
        addRow(b.rowsTop, R.drawable.ic_home, "Homestay profile",
                "Photos, amenities, booking link and QR",
                () -> startActivity(new Intent(requireContext(), HomestayProfileActivity.class)));

        addRow(b.rowsBottom, R.drawable.ic_car, "Find a Driver",
                "50+ StayRide drivers around you",
                () -> startActivity(new Intent(requireContext(), FindDriverActivity.class)));
        addRow(b.rowsBottom, R.drawable.ic_person, "My Drivers",
                d.drivers.size() + " own drivers",
                () -> startActivity(new Intent(requireContext(), MyDriversActivity.class)));
        addRow(b.rowsBottom, R.drawable.ic_info, "How StayRide works",
                "The complete homestay + cab flow",
                this::showFlow);
        addRow(b.rowsBottom, R.drawable.ic_refresh, "Reset demo data",
                "Restore the original demo dataset",
                this::confirmReset);
    }

    private void addRow(LinearLayout container, int icon, String title, String subtitle, Runnable action) {
        ItemLinkBinding binding = ItemLinkBinding.inflate(LayoutInflater.from(requireContext()), container, false);
        binding.icon.setImageResource(icon);
        binding.title.setText(title);
        binding.subtitle.setText(subtitle);
        binding.getRoot().setOnClickListener(v -> action.run());
        container.addView(binding.getRoot());
    }

    private void showFlow() {
        String flow = "1. Homestay joins StayRide\n"
                + "2. StayRide creates the homestay profile\n"
                + "3. Owner manages rooms, bookings and guests\n"
                + "4. Owner adds own drivers or uses the StayRide network of 50+ drivers\n"
                + "5. Guest arrives at the homestay\n"
                + "6. Owner shares the StayRide booking link\n"
                + "7. Guest books a cab from the web page\n"
                + "8. Owner receives the cab request instantly\n"
                + "9. Owner's own drivers get first priority\n"
                + "10. If unavailable, StayRide finds a nearby driver\n"
                + "11. Driver accepts and the guest gets a confirmation\n"
                + "12. Trip completes and the owner earns a commission\n"
                + "13. Guest is saved in the homestay database\n"
                + "14. Owner sends offers and campaigns to that guest";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("The complete StayRide flow")
                .setMessage(flow)
                .setPositiveButton("Got it", null)
                .show();
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset demo data?")
                .setMessage("All bookings, guests, drivers and cab requests you created will be replaced with the original demo dataset.")
                .setPositiveButton("Reset", (dialog, which) -> {
                    DataStore.reset();
                    Ui.toast(requireContext(), "Demo data restored");
                    onViewCreated(getView(), null);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
