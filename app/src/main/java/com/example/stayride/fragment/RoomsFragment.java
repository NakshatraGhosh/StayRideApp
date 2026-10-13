package com.example.stayride.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.stayride.R;
import com.example.stayride.activity.BookingDetailActivity;
import com.example.stayride.activity.NewBookingActivity;
import com.example.stayride.adapter.BookingAdapter;
import com.example.stayride.adapter.RoomAdapter;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.DialogRoomBinding;
import com.example.stayride.databinding.FragmentRoomsBinding;
import com.example.stayride.util.Sim;
import com.example.stayride.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class RoomsFragment extends Fragment {

    private FragmentRoomsBinding b;
    private final RoomAdapter roomAdapter = new RoomAdapter();
    private final BookingAdapter bookingAdapter = new BookingAdapter();
    private boolean roomsTab = true;
    private String filter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentRoomsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        b.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        roomAdapter.setListener(this::showRoomDialog);
        bookingAdapter.setListener(book -> {
            Intent i = new Intent(requireContext(), BookingDetailActivity.class);
            i.putExtra("booking_id", book.id);
            startActivity(i);
        });

        b.tabRooms.setOnClickListener(v -> switchTab(true));
        b.tabBookings.setOnClickListener(v -> switchTab(false));

        int[] roomChips = {R.id.fAll, R.id.fAvailable, R.id.fOccupied, R.id.fReserved, R.id.fMaintenance};
        for (int id : roomChips) {
            View chip = b.getRoot().findViewById(id);
            if (chip != null) chip.setOnClickListener(v -> {
                setFilterFromChip(v.getId());
                refresh();
            });
        }
        int[] bookingChips = {R.id.fAll, R.id.fConfirmed, R.id.fCheckedIn, R.id.fCheckedOut, R.id.fCancelled};
        for (int id : bookingChips) {
            View chip = b.getRoot().findViewById(id);
            if (chip != null) chip.setOnClickListener(v -> {
                setFilterFromChip(v.getId());
                refresh();
            });
        }

        b.fab.setOnClickListener(v -> {
            if (roomsTab) {
                showRoomDialog(null);
            } else {
                startActivity(new Intent(requireContext(), NewBookingActivity.class));
            }
        });

        b.toggle.check(R.id.tabRooms);
        refresh();
    }

    private void setFilterFromChip(int id) {
        if (id == R.id.fAll) filter = "ALL";
        else if (id == R.id.fAvailable) filter = "AVAILABLE";
        else if (id == R.id.fOccupied) filter = "OCCUPIED";
        else if (id == R.id.fReserved) filter = "RESERVED";
        else if (id == R.id.fMaintenance) filter = "MAINTENANCE";
        else if (id == R.id.fConfirmed) filter = "CONFIRMED";
        else if (id == R.id.fCheckedIn) filter = "CHECKED_IN";
        else if (id == R.id.fCheckedOut) filter = "CHECKED_OUT";
        else if (id == R.id.fCancelled) filter = "CANCELLED";
    }

    private void switchTab(boolean rooms) {
        roomsTab = rooms;
        filter = "ALL";
        b.fAll.setChecked(true);
        setChipVisibility(rooms);
        refresh();
    }

    private void setChipVisibility(boolean rooms) {
        b.fAvailable.setVisibility(rooms ? View.VISIBLE : View.GONE);
        b.fOccupied.setVisibility(rooms ? View.VISIBLE : View.GONE);
        b.fReserved.setVisibility(rooms ? View.VISIBLE : View.GONE);
        b.fMaintenance.setVisibility(rooms ? View.VISIBLE : View.GONE);
        b.fConfirmed.setVisibility(rooms ? View.GONE : View.VISIBLE);
        b.fCheckedIn.setVisibility(rooms ? View.GONE : View.VISIBLE);
        b.fCheckedOut.setVisibility(rooms ? View.GONE : View.VISIBLE);
        b.fCancelled.setVisibility(rooms ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        DataStore.get().sync(this::refresh);
    }

    private void refresh() {
        if (b == null) return;
        DataStore d = DataStore.get();
        if (roomsTab) {
            List<Models.Room> rooms = new ArrayList<>();
            for (Models.Room r : d.rooms) {
                if ("ALL".equals(filter) || r.status.equals(filter)) rooms.add(r);
            }
            roomAdapter.submit(rooms);
            b.list.setAdapter(roomAdapter);
            b.count.setText(rooms.size() + " of " + d.rooms.size() + " rooms \u00B7 "
                    + d.countRooms("AVAILABLE") + " available");
            b.empty.setVisibility(rooms.isEmpty() ? View.VISIBLE : View.GONE);
            b.empty.setText("No rooms match this filter.");
            b.fab.setImageResource(R.drawable.ic_add);
        } else {
            List<Models.Booking> list = new ArrayList<>();
            for (Models.Booking bk : d.bookings) {
                if ("ALL".equals(filter) || bk.status.equals(filter)) list.add(bk);
            }
            java.util.Collections.sort(list, (x, y) -> Long.compare(y.checkIn, x.checkIn));
            bookingAdapter.submit(list);
            b.list.setAdapter(bookingAdapter);
            b.count.setText(list.size() + " of " + d.bookings.size() + " bookings");
            b.empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            b.empty.setText("No bookings match this filter.");
            b.fab.setImageResource(R.drawable.ic_add);
        }
    }

    private void showRoomDialog(@Nullable Models.Room existing) {
        DialogRoomBinding bind = DialogRoomBinding.inflate(LayoutInflater.from(requireContext()));
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        sheet.setContentView(bind.getRoot());

        bind.sheetTitle.setText(existing == null ? "Add room" : "Room " + existing.number);
        bind.editNumber.setText(existing == null ? "" : existing.number);
        bind.editFloor.setText(existing == null ? "" : existing.floor);
        bind.editPrice.setText(existing == null ? "1800" : String.valueOf(existing.price));
        bind.editNote.setText(existing == null ? "" : existing.note);

        checkType(bind, existing == null ? "Standard" : existing.type);
        checkStatus(bind, existing == null ? "AVAILABLE" : existing.status);
        bind.btnDelete.setVisibility(existing == null ? View.GONE : View.VISIBLE);

        bind.btnClose.setOnClickListener(v -> sheet.dismiss());
        bind.btnCancel.setOnClickListener(v -> sheet.dismiss());
        bind.btnSave.setOnClickListener(v -> {
            if (saveRoom(existing, bind)) sheet.dismiss();
        });
        bind.btnDelete.setOnClickListener(v -> {
            sheet.dismiss();
            confirmDelete(existing);
        });
        sheet.show();
    }

    private void checkType(DialogRoomBinding bind, String type) {
        switch (type) {
            case "Deluxe": bind.tDeluxe.setChecked(true); break;
            case "Suite": bind.tSuite.setChecked(true); break;
            case "Family": bind.tFamily.setChecked(true); break;
            case "Dorm": bind.tDorm.setChecked(true); break;
            default: bind.tStandard.setChecked(true); break;
        }
    }

    private String selectedType(DialogRoomBinding bind) {
        int id = bind.typeGroup.getCheckedChipId();
        if (id == R.id.tDeluxe) return "Deluxe";
        if (id == R.id.tSuite) return "Suite";
        if (id == R.id.tFamily) return "Family";
        if (id == R.id.tDorm) return "Dorm";
        return "Standard";
    }

    private void checkStatus(DialogRoomBinding bind, String status) {
        switch (status) {
            case "OCCUPIED": bind.sOccupied.setChecked(true); break;
            case "RESERVED": bind.sReserved.setChecked(true); break;
            case "MAINTENANCE": bind.sMaintenance.setChecked(true); break;
            default: bind.sAvailable.setChecked(true); break;
        }
    }

    private String selectedStatus(DialogRoomBinding bind) {
        int id = bind.statusGroup.getCheckedChipId();
        if (id == R.id.sOccupied) return "OCCUPIED";
        if (id == R.id.sReserved) return "RESERVED";
        if (id == R.id.sMaintenance) return "MAINTENANCE";
        return "AVAILABLE";
    }

    private boolean saveRoom(@Nullable Models.Room existing, DialogRoomBinding bind) {
        String number = bind.editNumber.getText() == null ? "" : bind.editNumber.getText().toString().trim();
        String priceRaw = bind.editPrice.getText() == null ? "" : bind.editPrice.getText().toString().trim();
        if (number.isEmpty()) {
            bind.inputNumber.setError("Room number required");
            return false;
        }
        int price;
        try {
            price = Integer.parseInt(priceRaw);
        } catch (NumberFormatException e) {
            bind.inputPrice.setError("Enter a valid price");
            return false;
        }
        DataStore d = DataStore.get();
        String type = selectedType(bind);
        String floor = bind.editFloor.getText() == null ? "" : bind.editFloor.getText().toString().trim();
        String note = bind.editNote.getText() == null ? "" : bind.editNote.getText().toString().trim();
        String status = selectedStatus(bind);
        if (existing == null) {
            d.createRoom(number, type, floor, price, status, note, () -> {
                if (b == null) return;
                Ui.toast(requireContext(), "Room " + number + " added");
                refresh();
            });
        } else {
            d.updateRoom(existing.id, number, type, floor, price, status, note, () -> {
                if (b == null) return;
                Ui.toast(requireContext(), "Room " + number + " updated");
                refresh();
            });
        }
        return true;
    }

    private void confirmDelete(Models.Room room) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete room " + room.number + "?")
                .setMessage("This removes the room from your homestay. Existing bookings keep their history.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    DataStore.get().deleteRoom(room.id, () -> {
                        if (b == null) return;
                        Ui.toast(requireContext(), "Room deleted");
                        refresh();
                    });
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
