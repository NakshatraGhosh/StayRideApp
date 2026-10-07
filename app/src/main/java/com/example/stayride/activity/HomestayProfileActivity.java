package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stayride.R;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityHomestayProfileBinding;
import com.example.stayride.databinding.DialogHomestayEditBinding;
import com.example.stayride.databinding.DialogQrBinding;
import com.example.stayride.util.Qr;
import com.example.stayride.util.Ui;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class HomestayProfileActivity extends BaseActivity {

    private static final String[] PHOTO_PRESETS = {
            "Front elevation", "Deluxe room", "Valley view balcony", "Dining hall",
            "Rooftop garden", "Reception", "Garden view", "Family room", "Kitchen", "Bonfire area"
    };

    private static final String[] AMENITY_PRESETS = {
            "Free Wi-Fi", "24x7 Hot Water", "Private Parking", "Home Breakfast",
            "Air Conditioning", "Power Backup", "Luggage Storage", "Mountain View",
            "Laundry Service", "In-house Cabs (StayRide)", "Airport Pickup", "Restaurant",
            "Garden", "CCTV Security", "Doctor on Call", "Travel Desk"
    };

    private ActivityHomestayProfileBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityHomestayProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Homestay Profile");

        b.edit.setOnClickListener(v -> showEditDialog());
        b.copy.setOnClickListener(v -> copyLink());
        b.share.setOnClickListener(v -> shareLink());
        b.qr.setOnClickListener(v -> showQr());
        b.openPage.setOnClickListener(v ->
                startActivity(new Intent(this, GuestPageActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        Models.Homestay h = DataStore.get().homestay;
        b.name.setText(h.name);
        b.location.setText(h.location);
        b.owner.setText("Owned by " + h.ownerName);
        b.description.setText(h.description);
        b.contact.setText(h.phone + "  \u00B7  " + h.email);
        b.link.setText(h.link);
        renderPhotos(h);
        renderAmenities(h);
    }

    private void renderPhotos(Models.Homestay h) {
        b.photoRow.removeAllViews();
        for (String photo : h.photos) {
            Chip chip = new Chip(this);
            chip.setText(photo);
            chip.setCloseIconVisible(true);
            chip.setCloseIconResource(R.drawable.ic_close);
            chip.setOnCloseIconClickListener(v -> {
                h.photos.remove(photo);
                renderPhotos(h);
            });
            b.photoRow.addView(chip);
        }
        Chip add = new Chip(this);
        add.setText("+ Add photo");
        add.setChipBackgroundColorResource(R.color.brand_primary_light);
        add.setTextColor(Ui.color(add, R.color.brand_primary_dark));
        add.setOnClickListener(v -> pickPhoto(h));
        b.photoRow.addView(add);
    }

    private void pickPhoto(Models.Homestay h) {
        List<String> remaining = new ArrayList<>();
        for (String p : PHOTO_PRESETS) if (!h.photos.contains(p)) remaining.add(p);
        if (remaining.isEmpty()) {
            Ui.toast(this, "All demo photos are already added");
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add photo")
                .setItems(remaining.toArray(new String[0]), (dialog, which) -> {
                    h.photos.add(remaining.get(which));
                    renderPhotos(h);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renderAmenities(Models.Homestay h) {
        b.amenities.removeAllViews();
        for (String amenity : h.amenities) {
            Chip chip = new Chip(this);
            chip.setText(amenity);
            chip.setCloseIconVisible(true);
            chip.setCloseIconResource(R.drawable.ic_close);
            chip.setOnCloseIconClickListener(v -> {
                h.amenities.remove(amenity);
                renderAmenities(h);
            });
            b.amenities.addView(chip);
        }
        Chip add = new Chip(this);
        add.setText("+ Add amenity");
        add.setChipBackgroundColorResource(R.color.brand_primary_light);
        add.setTextColor(Ui.color(add, R.color.brand_primary_dark));
        add.setOnClickListener(v -> pickAmenity(h));
        b.amenities.addView(add);
    }

    private void pickAmenity(Models.Homestay h) {
        List<String> remaining = new ArrayList<>();
        for (String a : AMENITY_PRESETS) if (!h.amenities.contains(a)) remaining.add(a);
        if (remaining.isEmpty()) {
            Ui.toast(this, "All demo amenities are already added");
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add amenity")
                .setItems(remaining.toArray(new String[0]), (dialog, which) -> {
                    h.amenities.add(remaining.get(which));
                    renderAmenities(h);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog() {
        Models.Homestay h = DataStore.get().homestay;
        DialogHomestayEditBinding f = DialogHomestayEditBinding.inflate(LayoutInflater.from(this));
        f.editName.setText(h.name);
        f.editOwner.setText(h.ownerName);
        f.editPhone.setText(h.phone);
        f.editEmail.setText(h.email);
        f.editLocation.setText(h.location);
        f.editDescription.setText(h.description);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit homestay profile")
                .setView(f.getRoot())
                .setPositiveButton("Save", (dialog, which) -> {
                    if (TextUtils.isEmpty(text(f.editName.getText()))) {
                        Ui.toast(this, "Name cannot be empty");
                        return;
                    }
                    h.name = text(f.editName.getText());
                    h.ownerName = text(f.editOwner.getText());
                    h.phone = text(f.editPhone.getText());
                    h.email = text(f.editEmail.getText());
                    h.location = text(f.editLocation.getText());
                    h.description = text(f.editDescription.getText());
                    Ui.toast(this, "Profile updated");
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void copyLink() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("StayRide booking link",
                    DataStore.get().homestay.link));
        }
        Ui.toast(this, "Booking link copied");
    }

    private void shareLink() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Book your stay and cabs here: "
                + DataStore.get().homestay.link);
        startActivity(Intent.createChooser(intent, "Share booking link"));
    }

    private void showQr() {
        DialogQrBinding f = DialogQrBinding.inflate(LayoutInflater.from(this));
        String link = DataStore.get().homestay.link;
        f.qrLink.setText(link);
        android.graphics.Bitmap bitmap = Qr.of(link, 640);
        if (bitmap != null) f.qrImage.setImageBitmap(bitmap);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Booking QR code")
                .setView(f.getRoot())
                .setPositiveButton("Close", null)
                .show();
    }

    private String text(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
