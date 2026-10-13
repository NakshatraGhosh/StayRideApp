package com.example.stayride.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class Json {

    private Json() {
    }

    static String str(JSONObject o, String k) {
        if (o == null || o.isNull(k)) return null;
        return o.optString(k, null);
    }

    static int i(JSONObject o, String k) {
        return o.optInt(k, 0);
    }

    static long l(JSONObject o, String k) {
        return o.optLong(k, 0L);
    }

    static boolean b(JSONObject o, String k) {
        return o.optBoolean(k, false);
    }

    static float f(JSONObject o, String k) {
        return (float) o.optDouble(k, 0.0f);
    }

    static Integer integerOrNull(JSONObject o, String k) {
        if (o == null || o.isNull(k)) return null;
        return o.optInt(k);
    }

    public static Models.Room room(JSONObject o) {
        Models.Room r = new Models.Room(i(o, "id"), str(o, "number"), str(o, "type"),
                str(o, "floor"), i(o, "price"), str(o, "status"), str(o, "note"));
        return r;
    }

    public static Models.Guest guest(JSONObject o) {
        Models.Guest g = new Models.Guest(i(o, "id"), str(o, "name"), str(o, "phone"), str(o, "email"));
        g.notes = str(o, "notes");
        g.visits = i(o, "visits");
        g.totalSpending = i(o, "totalSpending");
        g.lastVisit = l(o, "lastVisit");
        g.firstVisit = l(o, "firstVisit");
        g.marketingConsent = b(o, "marketingConsent");
        return g;
    }

    public static Models.Booking booking(JSONObject o) {
        Models.Booking bk = new Models.Booking(i(o, "id"), i(o, "guestId"),
                str(o, "guestName"), str(o, "phone"));
        bk.checkIn = l(o, "checkIn");
        bk.checkOut = l(o, "checkOut");
        bk.roomNumber = str(o, "roomNumber");
        bk.guests = i(o, "guests");
        bk.status = str(o, "status");
        bk.paid = b(o, "paid");
        bk.amount = i(o, "amount");
        bk.source = str(o, "source");
        return bk;
    }

    public static Models.Driver driver(JSONObject o) {
        Models.Driver d = new Models.Driver(i(o, "id"), str(o, "name"));
        d.phone = str(o, "phone");
        d.vehicle = str(o, "vehicle");
        d.vehicleType = str(o, "vehicleType");
        d.seats = i(o, "seats");
        d.rating = f(o, "rating");
        d.area = str(o, "area");
        d.available = b(o, "available");
        d.active = o.has("active") ? b(o, "active") : true;
        d.ownerDriver = b(o, "ownerDriver");
        d.trips = new ArrayList<>();
        JSONArray arr = o.optJSONArray("trips");
        if (arr != null) {
            for (int j = 0; j < arr.length(); j++) {
                String t = arr.optString(j, null);
                if (t != null) d.trips.add(t);
            }
        }
        return d;
    }

    public static Models.CabTrip trip(JSONObject o) {
        Models.CabTrip t = new Models.CabTrip(i(o, "id"));
        t.guestName = str(o, "guestName");
        t.phone = str(o, "phone");
        t.pickup = str(o, "pickup");
        t.destination = str(o, "destination");
        t.date = l(o, "date");
        t.time = str(o, "time");
        t.passengers = i(o, "passengers");
        t.vehicleType = str(o, "vehicleType");
        t.estFare = i(o, "estFare");
        t.status = str(o, "status");
        t.driverId = integerOrNull(o, "driverId");
        t.driverName = str(o, "driverName");
        t.ownerDriver = b(o, "ownerDriver");
        t.fare = i(o, "fare");
        t.commission = i(o, "commission");
        t.commissionPaid = b(o, "commissionPaid");
        return t;
    }

    public static Models.Campaign campaign(JSONObject o) {
        Models.Campaign c = new Models.Campaign(i(o, "id"));
        c.title = str(o, "title");
        c.message = str(o, "message");
        c.audience = str(o, "audience");
        c.status = str(o, "status");
        c.recipients = i(o, "recipients");
        c.sent = i(o, "sent");
        c.failed = i(o, "failed");
        c.createdOn = l(o, "createdOn");
        return c;
    }

    public static Models.Homestay homestay(JSONObject o) {
        Models.Homestay h = new Models.Homestay();
        if (o == null) return h;
        String name = str(o, "name");
        if (name != null) h.name = name;
        String desc = str(o, "description");
        if (desc != null) h.description = desc;
        String loc = str(o, "location");
        if (loc != null) h.location = loc;
        String phone = str(o, "phone");
        if (phone != null) h.phone = phone;
        String email = str(o, "email");
        if (email != null) h.email = email;
        String owner = str(o, "ownerName");
        if (owner != null) h.ownerName = owner;
        String link = str(o, "link");
        if (link != null) h.link = link;
        String map = str(o, "mapQuery");
        if (map != null) h.mapQuery = map;
        JSONArray amenities = o.optJSONArray("amenities");
        if (amenities != null) {
            for (int j = 0; j < amenities.length(); j++) {
                String a = amenities.optString(j, null);
                if (a != null) h.amenities.add(a);
            }
        }
        JSONArray photos = o.optJSONArray("photos");
        if (photos != null) {
            for (int j = 0; j < photos.length(); j++) {
                String p = photos.optString(j, null);
                if (p != null) h.photos.add(p);
            }
        }
        return h;
    }

    public static List<Models.Room> rooms(JSONArray arr) {
        List<Models.Room> out = new ArrayList<>();
        for (int j = 0; j < arr.length(); j++) out.add(room(arr.optJSONObject(j)));
        return out;
    }

    public static List<Models.Guest> guests(JSONArray arr) {
        List<Models.Guest> out = new ArrayList<>();
        for (int j = 0; j < arr.length(); j++) out.add(guest(arr.optJSONObject(j)));
        return out;
    }

    public static List<Models.Booking> bookings(JSONArray arr) {
        List<Models.Booking> out = new ArrayList<>();
        for (int j = 0; j < arr.length(); j++) out.add(booking(arr.optJSONObject(j)));
        return out;
    }

    public static List<Models.Driver> drivers(JSONArray arr) {
        List<Models.Driver> out = new ArrayList<>();
        for (int j = 0; j < arr.length(); j++) out.add(driver(arr.optJSONObject(j)));
        return out;
    }

    public static List<Models.CabTrip> trips(JSONArray arr) {
        List<Models.CabTrip> out = new ArrayList<>();
        for (int j = 0; j < arr.length(); j++) out.add(trip(arr.optJSONObject(j)));
        return out;
    }

    public static List<Models.Campaign> campaigns(JSONArray arr) {
        List<Models.Campaign> out = new ArrayList<>();
        for (int j = 0; j < arr.length(); j++) out.add(campaign(arr.optJSONObject(j)));
        return out;
    }
}
