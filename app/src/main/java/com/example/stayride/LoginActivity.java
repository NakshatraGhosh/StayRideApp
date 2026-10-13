package com.example.stayride;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stayride.data.ApiClient;
import com.example.stayride.databinding.ActivityLoginBinding;
import com.example.stayride.util.Ui;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding b;
    private boolean signup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApiClient.init(getApplicationContext());
        if (ApiClient.get() != null && ApiClient.get().isLoggedIn()) {
            goMain();
            return;
        }
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("StayRide");
        }

        b.switchSignup.setOnClickListener(v -> toggle());
        b.btnSubmit.setOnClickListener(v -> submit());
    }

    private void toggle() {
        signup = !signup;
        int visibility = signup ? View.VISIBLE : View.GONE;
        b.inputName.setVisibility(visibility);
        b.inputPhone.setVisibility(visibility);
        b.inputHomestay.setVisibility(visibility);
        b.inputLocation.setVisibility(visibility);
        b.subtitle.setText(signup ? "Create your homestay account" : "Owner sign in");
        b.btnSubmit.setText(signup ? "Create account" : "Sign in");
        b.switchSignup.setText(signup ? "Already have an account? Sign in" : "New here? Create a homestay account");
    }

    private void submit() {
        String email = text(b.editEmail.getText());
        String password = text(b.editPassword.getText());
        if (TextUtils.isEmpty(email)) {
            b.inputEmail.setError("Email is required");
            return;
        }
        b.inputEmail.setError(null);
        if (TextUtils.isEmpty(password)) {
            b.inputPassword.setError("Password is required");
            return;
        }
        b.inputPassword.setError(null);

        String name = text(b.editName.getText());
        String phone = text(b.editPhone.getText());
        String homestay = text(b.editHomestay.getText());
        if (signup) {
            if (TextUtils.isEmpty(name)) {
                b.inputName.setError("Name is required");
                return;
            }
            b.inputName.setError(null);
            if (TextUtils.isEmpty(phone)) {
                b.inputPhone.setError("Phone is required");
                return;
            }
            b.inputPhone.setError(null);
            if (TextUtils.isEmpty(homestay)) {
                b.inputHomestay.setError("Homestay name is required");
                return;
            }
            b.inputHomestay.setError(null);
        }

        setBusy(true);
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);
                JSONObject auth;
                if (signup) {
                    body.put("name", name);
                    body.put("phone", phone);
                    body.put("homestayName", homestay);
                    body.put("location", text(b.editLocation.getText()));
                    auth = ApiClient.get().post("/auth/signup", body);
                } else {
                    auth = ApiClient.get().post("/auth/login", body);
                }
                ApiClient.get().saveAuth(auth);
                if (signup) {
                    try {
                        ApiClient.get().post("/dev/seed", null);
                    } catch (Exception ignored) {
                    }
                }
                runOnUiThread(this::goMain);
            } catch (Exception e) {
                final String message = e.getMessage() == null ? "Sign in failed" : e.getMessage();
                runOnUiThread(() -> {
                    setBusy(false);
                    Ui.toast(this, message);
                });
            }
        }).start();
    }

    private void setBusy(boolean busy) {
        b.btnSubmit.setEnabled(!busy);
        b.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private String text(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
