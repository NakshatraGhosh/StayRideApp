package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityNewGuestBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Ui;

public class NewGuestActivity extends BaseActivity {

    private ActivityNewGuestBinding b;
    private Models.Guest editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityNewGuestBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        int guestId = getIntent().getIntExtra("guest_id", -1);
        if (guestId > 0) {
            editing = DataStore.get().guest(guestId);
        }

        if (editing != null) {
            setTitle("Edit guest");
            b.editName.setText(editing.name);
            b.editPhone.setText(editing.phone);
            b.editEmail.setText(editing.email);
            b.editNotes.setText(editing.notes);
            b.consent.setChecked(editing.marketingConsent);
            b.btnSave.setText("Update guest");
        } else {
            setTitle("Add guest");
        }

        b.btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        String name = text(b.editName.getText());
        String phone = text(b.editPhone.getText());
        if (TextUtils.isEmpty(name)) {
            b.editName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            b.editPhone.setError("Phone number is required");
            return;
        }

        DataStore d = DataStore.get();
        String email = text(b.editEmail.getText());
        String notes = text(b.editNotes.getText());
        boolean consent = b.consent.isChecked();
        if (editing == null) {
            d.createGuest(name, phone, email, notes, consent, () -> {
                Ui.toast(this, name + " added to guest database");
                finish();
            });
        } else {
            d.updateGuest(editing.id, name, phone, email, notes, consent, () -> {
                Ui.toast(this, "Guest updated");
                finish();
            });
        }
    }

    private String text(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
