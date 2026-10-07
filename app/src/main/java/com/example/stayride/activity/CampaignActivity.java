package com.example.stayride.activity;

import com.example.stayride.BaseActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.stayride.adapter.CampaignAdapter;
import com.example.stayride.data.DataStore;
import com.example.stayride.data.Models;
import com.example.stayride.databinding.ActivityCampaignBinding;
import com.example.stayride.databinding.DialogCampaignFormBinding;
import com.example.stayride.util.Dates;
import com.example.stayride.util.Sim;
import com.example.stayride.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CampaignActivity extends BaseActivity {

    private static final String[] AUDIENCES = {"All guests", "Repeat guests", "Recent guests", "Inactive guests"};

    private ActivityCampaignBinding b;
    private final CampaignAdapter adapter = new CampaignAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityCampaignBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Campaigns");

        b.list.setLayoutManager(new LinearLayoutManager(this));
        b.list.setAdapter(adapter);
        adapter.setListener(this::onCampaignClick);
        b.fab.setOnClickListener(v -> showCreateForm());

        if (getIntent().getBooleanExtra("create", false)) {
            b.getRoot().post(this::showCreateForm);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        DataStore d = DataStore.get();
        adapter.submit(d.campaigns);
        int consent = 0;
        for (Models.Guest g : d.guests) if (g.marketingConsent) consent++;
        b.summary.setText(d.campaigns.size() + " campaigns \u00B7 " + d.guests.size()
                + " guests \u00B7 " + consent + " with marketing consent");
        b.empty.setVisibility(d.campaigns.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void onCampaignClick(Models.Campaign c) {
        switch (c.status) {
            case "CREATED":
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Send campaign?")
                        .setMessage("Send \u201C" + c.title + "\u201D to " + c.recipients
                                + " " + c.audience.toLowerCase() + " on WhatsApp? (demo)")
                        .setPositiveButton("Send now", (dialog, which) -> startSending(c))
                        .setNegativeButton("Not now", null)
                        .show();
                break;
            case "SENDING":
                Ui.toast(this, "Sending\u2026 " + c.sent + "/" + c.recipients);
                break;
            default:
                showDetails(c);
                break;
        }
    }

    private void showDetails(Models.Campaign c) {
        String body = c.message + "\n\nAudience: " + c.audience
                + "\nRecipients: " + c.recipients
                + "\nSent: " + c.sent + " \u00B7 Failed: " + c.failed
                + "\nCreated: " + Dates.fmt(c.createdOn);
        new MaterialAlertDialogBuilder(this)
                .setTitle(c.title)
                .setMessage(body)
                .setPositiveButton("Close", null)
                .show();
    }

    private void startSending(Models.Campaign c) {
        c.status = "SENDING";
        c.sent = 0;
        c.failed = 0;
        refresh();
        sendStep(c);
    }

    private void sendStep(Models.Campaign c) {
        Sim.delay(450, () -> {
            if (isFinishing() || isDestroyed()) return;
            int done = c.sent + c.failed;
            if ((done + 1) % 5 == 0) c.failed++;
            else c.sent++;
            if (c.sent + c.failed >= c.recipients) {
                c.status = "COMPLETED";
                Ui.toast(this, "Campaign completed \u2014 " + c.sent + " delivered");
            } else {
                sendStep(c);
            }
            refresh();
        });
    }

    private void showCreateForm() {
        DialogCampaignFormBinding f = DialogCampaignFormBinding.inflate(LayoutInflater.from(this));
        f.editAudience.setSimpleItems(AUDIENCES);
        f.editAudience.setText(AUDIENCES[0], false);
        Ui.wireDropdown(f.editAudience);
        f.editTitle.setText("Weekend in Gangtok: flat 15% off");
        f.editMessage.setText("Hi {name}! Plan your next mountain weekend at Mountain View Homestay with 15% off and a free airport pickup. Book on StayRide.");
        updateRecipients(f);

        f.editAudience.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRecipients(f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("New campaign")
                .setView(f.getRoot())
                .setPositiveButton("Create", (dialog, which) -> create(f))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateRecipients(DialogCampaignFormBinding f) {
        String audience = f.editAudience.getText() == null
                ? AUDIENCES[0] : f.editAudience.getText().toString();
        int count = DataStore.get().newCampaignRecipients(audience);
        f.recipients.setText(count + " recipients will get this message");
    }

    private void create(DialogCampaignFormBinding f) {
        String title = text(f.editTitle.getText());
        String message = text(f.editMessage.getText());
        if (TextUtils.isEmpty(title)) {
            f.editTitle.setError("Title is required");
            return;
        }
        if (TextUtils.isEmpty(message)) {
            f.inputMessage.setError("Message is required");
            return;
        }
        String audience = f.editAudience.getText() == null
                ? AUDIENCES[0] : f.editAudience.getText().toString();

        DataStore d = DataStore.get();
        Models.Campaign c = new Models.Campaign(d.nextCampaignId++);
        c.title = title;
        c.message = message;
        c.audience = audience;
        c.recipients = d.newCampaignRecipients(audience);
        c.status = "CREATED";
        c.createdOn = Dates.today();
        d.campaigns.add(0, c);
        Ui.toast(this, "Campaign created \u2014 tap it to start sending");
        refresh();
    }

    private String text(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
