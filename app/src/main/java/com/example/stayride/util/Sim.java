package com.example.stayride.util;

import android.os.Handler;
import android.os.Looper;

public final class Sim {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private Sim() {
    }

    public static void delay(long ms, Runnable r) {
        HANDLER.postDelayed(r, ms);
    }

    public static void next(Runnable r) {
        HANDLER.post(r);
    }

    public static void cancel(Runnable r) {
        HANDLER.removeCallbacks(r);
    }
}
