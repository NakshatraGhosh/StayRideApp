package com.example.stayride;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stayride.data.ApiClient;
import com.example.stayride.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_MS = 4000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivitySplashBinding b;
    private boolean navigated = false;

    private final Runnable goNext = this::goNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApiClient.init(getApplicationContext());
        b = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        startAnimations();

        if (savedInstanceState != null) {
            goNext.run();
        } else {
            handler.postDelayed(goNext, SPLASH_MS);
        }
    }

    private void startAnimations() {
        b.logo.setScaleX(0.6f);
        b.logo.setScaleY(0.6f);
        b.logo.setAlpha(0f);
        ObjectAnimator logoIn = ObjectAnimator.ofFloat(b.logo, "scale", 0.6f, 1.08f);
        logoIn.setDuration(650);
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(b.logo, "alpha", 0f, 1f);
        logoAlpha.setDuration(450);

        ObjectAnimator pulseUp = ObjectAnimator.ofFloat(b.logo, "scale", 1f, 1.09f);
        pulseUp.setDuration(900);
        ObjectAnimator pulseDown = ObjectAnimator.ofFloat(b.logo, "scale", 1.09f, 1f);
        pulseDown.setDuration(900);
        AnimatorSet pulse = new AnimatorSet();
        pulse.playSequentially(pulseUp, pulseDown);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator bob = ObjectAnimator.ofFloat(b.logo, "translationY", 0f, -14f);
        bob.setDuration(1100);
        bob.setRepeatCount(ObjectAnimator.INFINITE);
        bob.setRepeatMode(ObjectAnimator.REVERSE);
        bob.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator nameIn = ObjectAnimator.ofFloat(b.appName, "translationY", 60f, 0f);
        nameIn.setDuration(600);
        nameIn.setStartDelay(350);
        ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(b.appName, "alpha", 0f, 1f);
        nameAlpha.setDuration(500);
        nameAlpha.setStartDelay(350);

        ObjectAnimator tagIn = ObjectAnimator.ofFloat(b.tagline, "translationY", 40f, 0f);
        tagIn.setDuration(600);
        tagIn.setStartDelay(550);
        ObjectAnimator tagAlpha = ObjectAnimator.ofFloat(b.tagline, "alpha", 0f, 1f);
        tagAlpha.setDuration(500);
        tagAlpha.setStartDelay(550);

        ObjectAnimator loaderIn = ObjectAnimator.ofFloat(b.loader, "alpha", 0f, 1f);
        loaderIn.setDuration(500);
        loaderIn.setStartDelay(900);
        ObjectAnimator readyIn = ObjectAnimator.ofFloat(b.ready, "alpha", 0f, 1f);
        readyIn.setDuration(500);
        readyIn.setStartDelay(1100);

        AnimatorSet enter = new AnimatorSet();
        enter.playTogether(nameIn, nameAlpha, tagIn, tagAlpha, loaderIn, readyIn);
        enter.setInterpolator(new OvershootInterpolator(1.1f));

        AnimatorSet first = new AnimatorSet();
        first.playTogether(logoIn, logoAlpha);
        first.setInterpolator(new OvershootInterpolator(1.4f));

        first.start();
        enter.start();
        pulse.start();
        bob.start();
    }

    private void goNext() {
        if (navigated || isFinishing()) return;
        navigated = true;
        boolean loggedIn = ApiClient.get() != null && ApiClient.get().isLoggedIn();
        startActivity(new Intent(this, loggedIn ? MainActivity.class : LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(goNext);
        super.onDestroy();
    }
}
