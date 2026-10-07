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
        if (editing == null) {
            Models.Guest g = new Models.Guest(d.nextGuestId++, name, phone, text(b.editEmail.getText()));
            g.notes = text(b.editNotes.getText());
            g.marketingConsent = b.consent.isChecked();
            g.visits = 0;
            g.totalSpending = 0;
            g.lastVisit = Dates.today();
            g.firstVisit = Dates.today();
            d.guests.add(g);
            Ui.toast(this, name + " added to guest database");
        } else {
            editing.name = name;
            editing.phone = phone;
            editing.email = text(b.editEmail.getText());
            editing.notes = text(b.editNotes.getText());
            editing.marketingConsent = b.consent.isChecked();
            Ui.toast(this, "Guest updated");
            DataStore.get().bookings.forEach(bk -> {
                if (bk.guestId == editing.id) {
                    bk.guestName = editing.name;
                    bk.phone = editing.phone;
                }
            });
        }
        finish();
    }

    private String text(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
