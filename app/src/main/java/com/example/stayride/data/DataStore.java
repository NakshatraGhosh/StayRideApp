package com.example.stayride.data;

import android.os.Handler;
import android.os.Looper;

import com.example.stayride.util.Dates;
import com.example.stayride.util.Ui;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataStore {

    private static DataStore instance;

    public static DataStore get() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private DataStore() {
    }

    public final List<Models.Room> rooms = new ArrayList<>();
    public final List<Models.Guest> guests = new ArrayList<>();
    public final List<Models.Booking> bookings = new ArrayList<>();
    public final List<Models.Driver> drivers = new ArrayList<>();
    public final List<Models.Driver> networkDrivers = new ArrayList<>();
    public final List<Models.CabTrip> trips = new ArrayList<>();
    public final List<Models.Campaign> campaigns = new ArrayList<>();
    public Models.Homestay homestay = new Models.Homestay();

    public int nextRoomId = 1;
    public int nextGuestId = 1;
    public int nextBookingId = 1;
    public int nextDriverId = 1;
    public int nextTripId = 1;
    public int nextCampaignId = 1;

    public int[] revenueSeries = {186000, 214000, 172000, 248000, 265000};
    public int[] bookingSeries = {46, 52, 41, 63, 68};
    public int[] cabSeries = {14, 19, 16, 24, 27};

    private ApiClient api() {
        return ApiClient.get();
    }

    // ---------------------------------------------------------------- sync

    public void sync(Runnable onDone) {
        ApiClient api = api();
        if (api == null || !api.isLoggedIn()) {
            main.post(onDone);
            return;
        }
        io.execute(() -> {
            try {
                Snapshot s = fetchAll(api);
                main.post(() -> {
                    apply(s);
                    if (onDone != null) onDone.run();
                });
            } catch (Exception e) {
                postError(e);
                main.post(() -> {
                    if (onDone != null) onDone.run();
                });
            }
        });
    }

    private interface Call {
        void run() throws Exception;
    }

    private void mutate(Call call, Runnable onDone) {
        ApiClient api = api();
        if (api == null || !api.isLoggedIn()) {
            main.post(onDone);
            return;
        }
        io.execute(() -> {
            try {
                call.run();
            } catch (Exception e) {
                postError(e);
            }
            try {
                Snapshot s = fetchAll(api);
                main.post(() -> {
                    apply(s);
                    if (onDone != null) onDone.run();
                });
            } catch (Exception e) {
                postError(e);
                main.post(() -> {
                    if (onDone != null) onDone.run();
                });
            }
        });
    }

    private void postError(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) msg = "Network error";
        final String m = msg;
        ApiClient api = api();
        if (api != null) {
            main.post(() -> Ui.toast(api.context(), m));
        }
    }

    private Snapshot fetchAll(ApiClient api) throws Exception {
        Snapshot s = new Snapshot();
        s.homestay = Json.homestay(api.get("/homestay"));
        s.rooms = Json.rooms(api.getArray("/rooms"));
        s.guests = Json.guests(api.getArray("/guests"));
        s.bookings = Json.bookings(api.getArray("/bookings"));
        s.drivers = Json.drivers(api.getArray("/drivers?scope=mine"));
        s.network = Json.drivers(api.getArray("/drivers?scope=network"));
        s.trips = Json.trips(api.getArray("/trips?tab=all"));
        s.campaigns = Json.campaigns(api.getArray("/campaigns"));
        return s;
    }

    private void apply(Snapshot s) {
        homestay = s.homestay;
        rooms.clear();
        rooms.addAll(s.rooms);
        guests.clear();
        guests.addAll(s.guests);
        bookings.clear();
        bookings.addAll(s.bookings);
        drivers.clear();
        drivers.addAll(s.drivers);
        networkDrivers.clear();
        networkDrivers.addAll(s.network);
        trips.clear();
        trips.addAll(s.trips);
        campaigns.clear();
        campaigns.addAll(s.campaigns);
        resequence();
    }

    private void resequence() {
        nextRoomId = maxId(rooms) + 1;
        nextGuestId = maxGuestId() + 1;
        nextBookingId = maxBookingId() + 1;
        nextDriverId = maxDriverId() + 1;
        nextTripId = maxTripId() + 1;
        nextCampaignId = maxCampaignId() + 1;
    }

    private int maxId(List<Models.Room> list) {
        int m = 0;
        for (Models.Room r : list) m = Math.max(m, r.id);
        return m;
    }

    private int maxDriverId() {
        int m = 0;
        for (Models.Driver d : drivers) m = Math.max(m, d.id);
        return m;
    }

    private int maxGuestId() {
        int m = 0;
        for (Models.Guest g : guests) m = Math.max(m, g.id);
        return m;
    }

    private int maxBookingId() {
        int m = 0;
        for (Models.Booking b : bookings) m = Math.max(m, b.id);
        return m;
    }

    private int maxTripId() {
        int m = 0;
        for (Models.CabTrip t : trips) m = Math.max(m, t.id);
        return m;
    }

    private int maxCampaignId() {
        int m = 0;
        for (Models.Campaign c : campaigns) m = Math.max(m, c.id);
        return m;
    }

    private static class Snapshot {
        Models.Homestay homestay;
        List<Models.Room> rooms;
        List<Models.Guest> guests;
        List<Models.Booking> bookings;
        List<Models.Driver> drivers;
        List<Models.Driver> network;
        List<Models.CabTrip> trips;
        List<Models.Campaign> campaigns;
    }

    // ---------------------------------------------------------------- writes

    public void createRoom(String number, String type, String floor, int price, String status,
                           String note, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            b.put("number", number);
            b.put("type", type);
            b.put("floor", floor);
            b.put("price", price);
            b.put("status", status);
            b.put("note", note);
            api().post("/rooms", b);
        }, onDone);
    }

    public void updateRoom(int id, String number, String type, String floor, int price, String status,
                           String note, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            b.put("number", number);
            b.put("type", type);
            b.put("floor", floor);
            b.put("price", price);
            b.put("status", status);
            b.put("note", note);
            api().put("/rooms/" + id, b);
        }, onDone);
    }

    public void setRoomStatus(int id, String status, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            b.put("status", status);
            api().patch("/rooms/" + id + "/status", b);
        }, onDone);
    }

    public void deleteRoom(int id, Runnable onDone) {
        mutate(() -> api().delete("/rooms/" + id), onDone);
    }

    public void createGuest(String name, String phone, String email, String notes, boolean consent,
                            Runnable onDone) {
        mutate(() -> api().post("/guests", guestBody(name, phone, email, notes, consent)), onDone);
    }

    public void updateGuest(int id, String name, String phone, String email, String notes, boolean consent,
                            Runnable onDone) {
        mutate(() -> api().put("/guests/" + id, guestBody(name, phone, email, notes, consent)), onDone);
    }

    private JSONObject guestBody(String name, String phone, String email, String notes, boolean consent)
            throws Exception {
        JSONObject b = new JSONObject();
        b.put("name", name);
        b.put("phone", phone);
        b.put("email", email == null ? "" : email);
        b.put("notes", notes == null ? "" : notes);
        b.put("marketingConsent", consent);
        return b;
    }

    public void createBooking(Integer guestId, String guestName, String guestPhone, String guestEmail,
                              String roomNumber, long checkIn, long checkOut, int guests, int amount,
                              String source, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            if (guestId != null) b.put("guestId", guestId);
            b.put("guestName", guestName);
            b.put("guestPhone", guestPhone);
            b.put("guestEmail", guestEmail == null ? "" : guestEmail);
            b.put("roomNumber", roomNumber);
            b.put("checkIn", checkIn);
            b.put("checkOut", checkOut);
            b.put("guests", guests);
            b.put("amount", amount);
            b.put("source", source);
            api().post("/bookings", b);
        }, onDone);
    }

    public void checkIn(int id, Runnable onDone) {
        mutate(() -> api().post("/bookings/" + id + "/check-in", null), onDone);
    }

    public void checkOut(int id, Runnable onDone) {
        mutate(() -> api().post("/bookings/" + id + "/check-out", null), onDone);
    }

    public void cancelBooking(int id, Runnable onDone) {
        mutate(() -> api().post("/bookings/" + id + "/cancel", null), onDone);
    }

    public void setBookingPaid(int id, boolean paid, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            b.put("paid", paid);
            api().patch("/bookings/" + id + "/payment", b);
        }, onDone);
    }

    public void changeBookingRoom(int id, String roomNumber, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            b.put("roomNumber", roomNumber);
            api().post("/bookings/" + id + "/room", b);
        }, onDone);
    }

    public void deleteBooking(int id, Runnable onDone) {
        mutate(() -> api().delete("/bookings/" + id), onDone);
    }

    public void createDriver(String name, String phone, String vehicle, String vehicleType, int seats,
                             float rating, String area, List<String> tripRoutes, Runnable onDone) {
        mutate(() -> api().post("/drivers", driverBody(name, phone, vehicle, vehicleType, seats, rating, area, tripRoutes)),
                onDone);
    }

    public void updateDriver(int id, String name, String phone, String vehicle, String vehicleType,
                             int seats, float rating, String area, List<String> tripRoutes, Runnable onDone) {
        mutate(() -> api().put("/drivers/" + id,
                driverBody(name, phone, vehicle, vehicleType, seats, rating, area, tripRoutes)), onDone);
    }

    private JSONObject driverBody(String name, String phone, String vehicle, String vehicleType, int seats,
                                  float rating, String area, List<String> tripRoutes) throws Exception {
        JSONObject b = new JSONObject();
        b.put("name", name);
        b.put("phone", phone);
        b.put("vehicle", vehicle);
        b.put("vehicleType", vehicleType);
        b.put("seats", seats);
        b.put("rating", rating);
        b.put("area", area);
        JSONArray routes = new JSONArray();
        if (tripRoutes != null) {
            for (String t : tripRoutes) routes.put(t);
        }
        b.put("trips", routes);
        return b;
    }

    public void setDriverAvailability(int id, boolean available, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            b.put("available", available);
            api().patch("/drivers/" + id + "/availability", b);
        }, onDone);
    }

    public void setDriverActive(int id, boolean active, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            b.put("active", active);
            api().patch("/drivers/" + id + "/active", b);
        }, onDone);
    }

    public void deleteDriver(int id, Runnable onDone) {
        mutate(() -> api().delete("/drivers/" + id), onDone);
    }

    public void createTrip(String guestName, String phone, String pickup, String destination, long date,
                           String time, int passengers, String vehicleType, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            b.put("guestName", guestName);
            b.put("phone", phone);
            b.put("pickup", pickup);
            b.put("destination", destination);
            b.put("date", date);
            b.put("time", time);
            b.put("passengers", passengers);
            b.put("vehicleType", vehicleType);
            api().post("/trips", b);
        }, onDone);
    }

    public void bookDriver(String guestName, String phone, String pickup, String destination, long date,
                           String time, int passengers, String vehicleType, int driverId, Runnable onDone) {
        ApiClient api = api();
        if (api == null || !api.isLoggedIn()) {
            main.post(onDone);
            return;
        }
        io.execute(() -> {
            try {
                JSONObject b = new JSONObject();
                b.put("guestName", guestName);
                b.put("phone", phone);
                b.put("pickup", pickup);
                b.put("destination", destination);
                b.put("date", date);
                b.put("time", time);
                b.put("passengers", passengers);
                b.put("vehicleType", vehicleType);
                JSONObject created = api.post("/trips", b);
                int tripId = created == null ? 0 : created.optInt("id", 0);
                if (tripId > 0) {
                    JSONObject a = new JSONObject();
                    a.put("driverId", driverId);
                    api.post("/trips/" + tripId + "/assign", a);
                }
            } catch (Exception e) {
                postError(e);
            }
            try {
                Snapshot s = fetchAll(api);
                main.post(() -> {
                    apply(s);
                    if (onDone != null) onDone.run();
                });
            } catch (Exception e) {
                postError(e);
                main.post(() -> {
                    if (onDone != null) onDone.run();
                });
            }
        });
    }

    public void assignTripDriver(int tripId, int driverId, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            b.put("driverId", driverId);
            api().post("/trips/" + tripId + "/assign", b);
        }, onDone);
    }

    public void acceptTrip(int tripId, Runnable onDone) {
        mutate(() -> api().post("/trips/" + tripId + "/accept", null), onDone);
    }

    public void reassignTrip(int tripId, Runnable onDone) {
        mutate(() -> api().post("/trips/" + tripId + "/reassign", null), onDone);
    }

    public void startTrip(int tripId, Runnable onDone) {
        mutate(() -> api().post("/trips/" + tripId + "/start", null), onDone);
    }

    public void completeTrip(int tripId, Runnable onDone) {
        mutate(() -> api().post("/trips/" + tripId + "/complete", null), onDone);
    }

    public void cancelTrip(int tripId, Runnable onDone) {
        mutate(() -> api().post("/trips/" + tripId + "/cancel", null), onDone);
    }

    public void payCommission(int tripId, boolean paid, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            b.put("paid", paid);
            api().post("/trips/" + tripId + "/commission/pay", b);
        }, onDone);
    }

    public void createCampaign(String title, String message, String audience, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            b.put("title", title);
            b.put("message", message);
            b.put("audience", audience);
            api().post("/campaigns", b);
        }, onDone);
    }

    public void sendCampaign(int id, Runnable onDone) {
        mutate(() -> api().post("/campaigns/" + id + "/send", null), onDone);
    }

    public void deleteCampaign(int id, Runnable onDone) {
        mutate(() -> api().delete("/campaigns/" + id), onDone);
    }

    public void updateHomestay(Models.Homestay h, Runnable onDone) {
        mutate(() -> {
            JSONObject b = new JSONObject();
            b.put("name", h.name);
            b.put("description", h.description);
            b.put("location", h.location);
            b.put("phone", h.phone);
            b.put("email", h.email);
            b.put("ownerName", h.ownerName);
            b.put("link", h.link);
            b.put("mapQuery", h.mapQuery);
            JSONArray amenities = new JSONArray();
            for (String a : h.amenities) amenities.put(a);
            b.put("amenities", amenities);
            JSONArray photos = new JSONArray();
            for (String p : h.photos) photos.put(p);
            b.put("photos", photos);
            api().put("/homestay", b);
        }, onDone);
    }

    public void resetDemo(Runnable onDone) {
        ApiClient api = api();
        if (api == null || !api.isLoggedIn()) {
            main.post(onDone);
            return;
        }
        io.execute(() -> {
            try {
                api.post("/dev/seed", null);
            } catch (Exception e) {
                postError(e);
            }
            try {
                Snapshot s = fetchAll(api);
                main.post(() -> {
                    apply(s);
                    if (onDone != null) onDone.run();
                });
            } catch (Exception e) {
                postError(e);
                main.post(() -> {
                    if (onDone != null) onDone.run();
                });
            }
        });
    }

    // ---------------------------------------------------------------- queries

    public Models.Room room(int id) {
        for (Models.Room r : rooms) if (r.id == id) return r;
        return null;
    }

    public Models.Guest guest(int id) {
        for (Models.Guest g : guests) if (g.id == id) return g;
        return null;
    }

    public Models.Booking booking(int id) {
        for (Models.Booking b : bookings) if (b.id == id) return b;
        return null;
    }

    public Models.CabTrip trip(int id) {
        for (Models.CabTrip t : trips) if (t.id == id) return t;
        return null;
    }

    public Models.Driver ownerDriver(int id) {
        for (Models.Driver d : drivers) if (d.id == id) return d;
        return null;
    }

    public Models.Driver driverAny(int id) {
        Models.Driver d = ownerDriver(id);
        if (d != null) return d;
        for (Models.Driver n : networkDrivers) if (n.id == id) return n;
        return null;
    }

    public List<Models.Booking> todayBookings() {
        List<Models.Booking> out = new ArrayList<>();
        long today = Dates.today();
        for (Models.Booking b : bookings) {
            if (b.checkIn == today && !"CANCELLED".equals(b.status)) out.add(b);
        }
        return out;
    }

    public int currentGuestCount() {
        int count = 0;
        for (Models.Booking b : bookings) {
            if ("CHECKED_IN".equals(b.status)) count += Math.max(1, b.guests);
        }
        return count;
    }

    public int countRooms(String status) {
        int c = 0;
        for (Models.Room r : rooms) if (status.equals(r.status)) c++;
        return c;
    }

    public int monthlyRevenue() {
        return monthRevenue(Dates.today());
    }

    public int monthRevenue(long anyDayInMonth) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(anyDayInMonth);
        int month = cal.get(java.util.Calendar.MONTH);
        int year = cal.get(java.util.Calendar.YEAR);
        int sum = 0;
        for (Models.Booking b : bookings) {
            if ("CANCELLED".equals(b.status)) continue;
            java.util.Calendar c2 = java.util.Calendar.getInstance();
            c2.setTimeInMillis(b.checkIn);
            if (c2.get(java.util.Calendar.MONTH) == month && c2.get(java.util.Calendar.YEAR) == year) {
                sum += b.amount;
            }
        }
        return sum;
    }

    public int monthBookingCount() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int month = cal.get(java.util.Calendar.MONTH);
        int year = cal.get(java.util.Calendar.YEAR);
        int c = 0;
        for (Models.Booking b : bookings) {
            if ("CANCELLED".equals(b.status)) continue;
            java.util.Calendar c2 = java.util.Calendar.getInstance();
            c2.setTimeInMillis(b.checkIn);
            if (c2.get(java.util.Calendar.MONTH) == month && c2.get(java.util.Calendar.YEAR) == year) c++;
        }
        return c;
    }

    public int totalBookings() {
        return bookings.size();
    }

    public int occupancyPercent() {
        if (rooms.isEmpty()) return 0;
        int occupied = countRooms("OCCUPIED") + countRooms("RESERVED");
        return Math.round(occupied * 100f / rooms.size());
    }

    public int averageBookingValue() {
        int revenue = monthlyRevenue();
        int count = monthBookingCount();
        return count == 0 ? 0 : revenue / count;
    }

    public int commissionEarned() {
        int sum = 0;
        for (Models.CabTrip t : trips) if (t.isCompleted()) sum += t.commission;
        return sum;
    }

    public int commissionPending() {
        int sum = 0;
        for (Models.CabTrip t : trips) if (t.isCompleted() && !t.commissionPaid) sum += t.commission;
        return sum;
    }

    public int commissionPaid() {
        int sum = 0;
        for (Models.CabTrip t : trips) if (t.isCompleted() && t.commissionPaid) sum += t.commission;
        return sum;
    }

    public int completedTrips() {
        int c = 0;
        for (Models.CabTrip t : trips) if (t.isCompleted()) c++;
        return c;
    }

    public List<Models.Booking> recentBookings(int n) {
        List<Models.Booking> out = new ArrayList<>(bookings);
        java.util.Collections.sort(out, (a, b) -> Long.compare(b.checkIn, a.checkIn));
        return out.subList(0, Math.min(n, out.size()));
    }

    public List<Models.Booking> upcomingCheckIns(int n) {
        long today = Dates.today();
        List<Models.Booking> out = new ArrayList<>();
        for (Models.Booking b : bookings) {
            if (b.checkIn >= today && "CONFIRMED".equals(b.status)) out.add(b);
        }
        java.util.Collections.sort(out, (a, b) -> Long.compare(a.checkIn, b.checkIn));
        return out.subList(0, Math.min(n, out.size()));
    }

    public List<Models.Booking> bookingsOfGuest(int guestId) {
        List<Models.Booking> out = new ArrayList<>();
        for (Models.Booking b : bookings) if (b.guestId == guestId) out.add(b);
        java.util.Collections.sort(out, (a, b) -> Long.compare(b.checkIn, a.checkIn));
        return out;
    }

    public List<Models.CabTrip> recentTrips(int n) {
        List<Models.CabTrip> out = new ArrayList<>(trips);
        java.util.Collections.sort(out, (a, b) -> Long.compare(b.date, a.date));
        return out.subList(0, Math.min(n, out.size()));
    }

    public List<Models.CabTrip> tripsWithStatus(String... statuses) {
        List<Models.CabTrip> out = new ArrayList<>();
        for (Models.CabTrip t : trips) {
            for (String s : statuses) {
                if (s.equals(t.status)) {
                    out.add(t);
                    break;
                }
            }
        }
        return out;
    }

    public int countGuests(boolean repeat) {
        int c = 0;
        for (Models.Guest g : guests) {
            if (g.isRepeat() == repeat) c++;
        }
        return c;
    }

    public List<Models.Guest> searchGuests(String query, String filter) {
        return searchGuests(query, filter, 0);
    }

    public List<Models.Guest> searchGuests(String query, String filter, int periodDays) {
        String q = query == null ? "" : query.trim().toLowerCase();
        long cutoff = periodDays > 0 ? Dates.today() - (long) periodDays * Dates.DAY : 0L;
        List<Models.Guest> out = new ArrayList<>();
        for (Models.Guest g : guests) {
            if ("NEW".equals(filter) && g.isRepeat()) continue;
            if ("REPEAT".equals(filter) && !g.isRepeat()) continue;
            if (cutoff > 0 && g.lastVisit < cutoff) continue;
            boolean nameMatch = g.name != null && g.name.toLowerCase().contains(q);
            boolean phoneMatch = g.phone != null && g.phone.contains(q);
            boolean emailMatch = g.email != null && g.email.toLowerCase().contains(q);
            if (q.isEmpty() || nameMatch || phoneMatch || emailMatch) {
                out.add(g);
            }
        }
        return out;
    }

    public List<Models.Driver> availableOwnerDrivers(String vehicleType) {
        List<Models.Driver> out = new ArrayList<>();
        for (Models.Driver d : drivers) {
            if (d.active && d.available && (vehicleType == null || vehicleType.equalsIgnoreCase(d.vehicleType))) {
                out.add(d);
            }
        }
        return out;
    }

    public List<Models.Driver> availableNetworkDrivers(String vehicleType) {
        List<Models.Driver> out = new ArrayList<>();
        for (Models.Driver d : networkDrivers) {
            if (d.available && (vehicleType == null || vehicleType.equalsIgnoreCase(d.vehicleType))) {
                out.add(d);
            }
        }
        return out;
    }

    public int newCampaignRecipients(String audience) {
        int all = guests.size();
        if ("All guests".equals(audience)) return all;
        if ("Repeat guests".equals(audience)) return countGuests(true);
        if ("Recent guests".equals(audience)) {
            long cutoff = Dates.today() - 60 * Dates.DAY;
            int c = 0;
            for (Models.Guest g : guests) if (g.lastVisit >= cutoff) c++;
            return c;
        }
        if ("Inactive guests".equals(audience)) {
            long cutoff = Dates.today() - 90 * Dates.DAY;
            int c = 0;
            for (Models.Guest g : guests) if (g.lastVisit < cutoff) c++;
            return c;
        }
        return all;
    }
}
