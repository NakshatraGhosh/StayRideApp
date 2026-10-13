package com.example.stayride.util;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import com.example.stayride.data.Models;

/**
 * Lightweight geolocation helpers for the demo. Real lat/lng are not stored on
 * the backend, so a driver's "current location" is derived from the first known
 * area in their service string, with a small deterministic offset per driver so
 * equal-area drivers still rank differently.
 */
public final class Geo {

    public static final double[] HOMESTAY = {27.3314, 88.6138}; // MG Marg, Gangtok

    private Geo() {
    }

    /** Best-effort coordinate lookup for a place or service-area string. */
    public static double[] byName(String name) {
        if (name == null) return null;
        String s = name.toLowerCase();
        if (s.contains("njp")) return new double[]{26.7147, 88.4165};
        if (s.contains("bagdogra")) return new double[]{26.6812, 88.3285};
        if (s.contains("darjeeling")) return new double[]{27.0410, 88.2663};
        if (s.contains("pelling")) return new double[]{27.3011, 88.2397};
        if (s.contains("lachung")) return new double[]{27.6875, 88.7425};
        if (s.contains("lachen")) return new double[]{27.7242, 88.5567};
        if (s.contains("kalimpong")) return new double[]{27.0594, 88.4700};
        if (s.contains("namchi")) return new double[]{27.1667, 88.3500};
        if (s.contains("ravangla")) return new double[]{27.3000, 88.3600};
        if (s.contains("siliguri")) return new double[]{26.7271, 88.3953};
        if (s.contains("gangtok") || s.contains("mg marg") || s.contains("homestay")) {
            return new double[]{27.3314, 88.6138};
        }
        return null;
    }

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Distance in km from the given target point to a driver's current location. */
    public static double distanceFrom(double[] target, Models.Driver d) {
        if (target == null) target = HOMESTAY;
        double[] c = byName(d.area);
        double base;
        if (c == null) {
            base = 4.0 + (d.id % 10) * 0.6;
        } else {
            base = distanceKm(target[0], target[1], c[0], c[1]);
        }
        base += (d.id % 7) * 0.3; // deterministic jitter so drivers differ
        return Math.max(0.4, Math.round(base * 10) / 10.0);
    }

    /** Last known device location, or null when unavailable/denied. */
    public static double[] myLocation(Context c) {
        try {
            LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return null;
            Location best = null;
            for (String provider : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getAccuracy() < best.getAccuracy())) {
                    best = l;
                }
            }
            if (best == null) return null;
            return new double[]{best.getLatitude(), best.getLongitude()};
        } catch (Exception e) {
            return null;
        }
    }
}
