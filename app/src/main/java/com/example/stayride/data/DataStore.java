package com.example.stayride.data;

import com.example.stayride.util.Dates;

import java.util.ArrayList;
import java.util.List;

public class DataStore {

    private static DataStore instance;

    public static DataStore get() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public static void reset() {
        instance = new DataStore();
    }

    private DataStore() {
        new Seeder(this);
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
        for (Models.Room r : rooms) if (r.status.equals(status)) c++;
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
                if (t.status.equals(s)) {
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
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Models.Guest> out = new ArrayList<>();
        for (Models.Guest g : guests) {
            if ("NEW".equals(filter) && g.isRepeat()) continue;
            if ("REPEAT".equals(filter) && !g.isRepeat()) continue;
            if (q.isEmpty()
                    || g.name.toLowerCase().contains(q)
                    || g.phone.contains(q)
                    || g.email.toLowerCase().contains(q)) {
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

    public void deleteRoom(Models.Room room) {
        rooms.remove(room);
    }

    public void deleteDriver(Models.Driver driver) {
        drivers.remove(driver);
    }

    public void deleteBooking(Models.Booking booking) {
        bookings.remove(booking);
    }

    public void addBookingForGuest(Models.Guest g, Models.Booking b) {
        g.bookingIds.add(b.id);
    }
}
