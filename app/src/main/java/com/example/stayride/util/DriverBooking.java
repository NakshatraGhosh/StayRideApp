package com.example.stayride.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stayride.R;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.DialogBookedBinding;
import com.example.stayride.databinding.DialogDriverBookingBinding;
import com.example.stayride.databinding.DialogSelectPassengerBinding;
import com.example.stayride.databinding.ItemPassengerBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared owner-side flow to book a cab for a guest with a specific driver:
 * pickup / date / time -> passenger selection -> trip created & driver assigned
 * -> WhatsApp confirmation with the advance payment (Razorpay) link.
 */
public final class DriverBooking {

    public static final String[] TIMES = {
            "06:00 AM", "07:30 AM", "09:00 AM", "10:30 AM", "12:00 PM",
            "02:00 PM", "04:30 PM", "06:00 PM", "08:00 PM"
    };

    private static final String RAZORPAY = "https://rzp.io/l/stayride-advance";

    private DriverBooking() {
    }

    public static List<String> pickupItems(String homestay) {
        List<String> list = new ArrayList<>();
        list.add(homestay);
        list.add("Use my current location");
        list.add("NJP Station");
        list.add("Bagdogra Airport");
        list.add("Gangtok MG Marg");
        list.add("Darjeeling");
        list.add("Pelling");
        return list;
    }

    /** Kick off the booking flow for one driver. */
    public static void start(Activity a, Models.Driver driver, String defaultPickup) {
        DialogDriverBookingBinding f = DialogDriverBookingBinding.inflate(a.getLayoutInflater());
        String homestay = DataStore.get().homestay.name;

        List<String> pickups = pickupItems(homestay);
        f.editPickup.setSimpleItems(pickups.toArray(new String[0]));
        Ui.wireDropdown(f.editPickup);
        f.editTime.setSimpleItems(TIMES);
        Ui.wireDropdown(f.editTime);

        final List<Long> dates = new ArrayList<>();
        final List<String> dateLabels = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            long d = Dates.plusDays(i);
            dates.add(d);
            String label = i == 0 ? "Today \u00B7 " + Dates.fmt(d)
                    : i == 1 ? "Tomorrow \u00B7 " + Dates.fmt(d) : Dates.fmt(d);
            dateLabels.add(label);
        }
        f.editDate.setSimpleItems(dateLabels.toArray(new String[0]));
        Ui.wireDropdown(f.editDate);

        f.editPickup.setText(defaultPickup == null || defaultPickup.isEmpty() ? homestay : defaultPickup, false);
        f.editDate.setText(dateLabels.get(1), false);
        f.editTime.setText(TIMES[2], false);

        new MaterialAlertDialogBuilder(a)
                .setTitle("Book " + driver.name)
                .setView(f.getRoot())
                .setPositiveButton("Continue", (dlg, w) -> {
                    String pickup = text(f.editPickup.getText());
                    String time = text(f.editTime.getText());
                    String dateText = text(f.editDate.getText());
                    int idx = dateLabels.indexOf(dateText);
                    long date = idx >= 0 ? dates.get(idx) : Dates.plusDays(1);
                    choosePassenger(a, driver,
                            pickup.isEmpty() ? homestay : pickup,
                            date, time.isEmpty() ? TIMES[2] : time);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void choosePassenger(Activity a, Models.Driver driver, String pickup, long date, String time) {
        final List<Models.Guest> guests = new ArrayList<>(DataStore.get().guests);
        if (guests.isEmpty()) {
            Ui.toast(a, "Add a guest first, then book a cab");
            return;
        }

        DialogSelectPassengerBinding f = DialogSelectPassengerBinding.inflate(a.getLayoutInflater());
        final int[] selected = {-1};
        PassengerAdapter adapter = new PassengerAdapter(guests, pos -> selected[0] = pos);
        f.guestList.setLayoutManager(new LinearLayoutManager(a));
        f.guestList.setAdapter(adapter);

        AlertDialog dlg = new MaterialAlertDialogBuilder(a)
                .setTitle("Who is this cab for?")
                .setView(f.getRoot())
                .setPositiveButton("Book", null)
                .setNegativeButton("Cancel", null)
                .show();

        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (selected[0] < 0) {
                Ui.toast(a, "Select the guest this cab is for");
                return;
            }
            dlg.dismiss();
            Models.Guest guest = guests.get(selected[0]);
            book(a, driver, guest, pickup, date, time);
        });
    }

    private static void book(Activity a, Models.Driver driver, Models.Guest guest,
                             String pickup, long date, String time) {
        Ui.toast(a, "Booking " + driver.name + "\u2026");
        DataStore.get().bookDriver(guest.name, guest.phone, pickup, "Local travel",
                date, time, 2, driver.vehicleType, driver.id,
                () -> showBooked(a, driver, guest, pickup, date, time));
    }

    private static void showBooked(Activity a, Models.Driver driver, Models.Guest guest,
                                   String pickup, long date, String time) {
        DialogBookedBinding f = DialogBookedBinding.inflate(a.getLayoutInflater());
        f.message.setText("Driver " + driver.name + " is booked for " + guest.name
                + " from " + pickup + " at " + time + " on " + Dates.fmt(date)
                + ". Tell your guest to confirm the booking.");

        final String message = whatsappText(guest.name, DataStore.get().homestay.name,
                pickup, time, Dates.fmt(date));

        AlertDialog dlg = new MaterialAlertDialogBuilder(a)
                .setView(f.getRoot())
                .setCancelable(false)
                .create();
        f.btnWhatsapp.setOnClickListener(v -> shareWhatsapp(a, guest.phone, message));
        f.btnDone.setOnClickListener(v -> dlg.dismiss());
        dlg.show();
    }

    private static String whatsappText(String guest, String homestay, String pickup,
                                       String time, String date) {
        return "Dear " + guest + ", your driver has been booked from " + homestay
                + " for " + date + " at " + time + " (pickup: " + pickup + ")."
                + " Kindly pay the advance booking charge to confirm the trip."
                + " Pay securely here: " + RAZORPAY
                + ". Kindly pay this as soon as possible to confirm the driver.";
    }

    private static void shareWhatsapp(Activity a, String phone, String message) {
        String digits = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) digits = "91" + digits;
        Uri uri = Uri.parse("https://wa.me/" + digits + "?text=" + Uri.encode(message));
        Intent view = new Intent(Intent.ACTION_VIEW, uri);
        try {
            a.startActivity(new Intent(view).setPackage("com.whatsapp"));
        } catch (ActivityNotFoundException e) {
            try {
                a.startActivity(view);
            } catch (ActivityNotFoundException e2) {
                Ui.toast(a, "No app available to share on WhatsApp");
            }
        }
    }

    private static String text(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private interface OnSelect {
        void onSelect(int position);
    }

    private static class PassengerAdapter extends RecyclerView.Adapter<PassengerAdapter.VH> {
        private final List<Models.Guest> items;
        private final OnSelect callback;
        private int selectedPos = -1;

        PassengerAdapter(List<Models.Guest> items, OnSelect callback) {
            this.items = items;
            this.callback = callback;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemPassengerBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Models.Guest g = items.get(position);
            holder.b.name.setText(g.name);
            holder.b.phone.setText(g.phone);
            boolean selected = position == selectedPos;
            holder.b.check.setVisibility(selected ? android.view.View.VISIBLE : android.view.View.GONE);
            holder.b.card.setStrokeColor(Ui.color(holder.itemView,
                    selected ? R.color.brand_primary : R.color.divider));
            holder.b.card.setStrokeWidth(selected ? 3 : 1);
            holder.b.card.setOnClickListener(v -> {
                int old = selectedPos;
                selectedPos = holder.getBindingAdapterPosition();
                if (old >= 0) notifyItemChanged(old);
                notifyItemChanged(selectedPos);
                callback.onSelect(selectedPos);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ItemPassengerBinding b;

            VH(ItemPassengerBinding binding) {
                super(binding.getRoot());
                b = binding;
            }
        }
    }
}
