package com.example.stayride.data;

import java.util.ArrayList;
import java.util.List;

public class Models {

    public static class Room {
        public int id;
        public String number;
        public String type;
        public String floor;
        public int price;
        public String status;
        public String note;

        public Room(int id, String number, String type, String floor, int price, String status, String note) {
            this.id = id;
            this.number = number;
            this.type = type;
            this.floor = floor;
            this.price = price;
            this.status = status;
            this.note = note;
        }
    }

    public static class Guest {
        public int id;
        public String name;
        public String phone;
        public String email;
        public String notes;
        public int visits;
        public int totalSpending;
        public long lastVisit;
        public long firstVisit;
        public boolean marketingConsent;
        public List<Integer> bookingIds = new ArrayList<>();

        public Guest(int id, String name, String phone, String email) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.email = email;
        }

        public boolean isRepeat() {
            return visits > 1;
        }
    }

    public static class Booking {
        public int id;
        public int guestId;
        public String guestName;
        public String phone;
        public long checkIn;
        public long checkOut;
        public String roomNumber;
        public int guests;
        public String status;
        public boolean paid;
        public int amount;
        public String source;

        public Booking(int id, int guestId, String guestName, String phone) {
            this.id = id;
            this.guestId = guestId;
            this.guestName = guestName;
            this.phone = phone;
        }

        public int nights() {
            long diff = checkOut - checkIn;
            int n = (int) (diff / 86400000L);
            return Math.max(1, n);
        }
    }

    public static class Driver {
        public int id;
        public String name;
        public String phone;
        public String vehicle;
        public String vehicleType;
        public int seats;
        public float rating;
        public String area;
        public boolean available;
        public boolean active = true;
        public boolean ownerDriver;
        public List<String> trips = new ArrayList<>();

        public Driver(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public boolean supports(String trip) {
            for (String t : trips) {
                if (t.equalsIgnoreCase(trip)) return true;
            }
            return false;
        }
    }

    public static class CabTrip {
        public int id;
        public String guestName;
        public String phone;
        public String pickup;
        public String destination;
        public long date;
        public String time;
        public int passengers;
        public String vehicleType;
        public int estFare;
        public String status;
        public Integer driverId;
        public String driverName;
        public boolean ownerDriver;
        public int fare;
        public int commission;
        public boolean commissionPaid;

        public CabTrip(int id) {
            this.id = id;
        }

        public String route() {
            return pickup + " \u2192 " + destination;
        }

        public boolean isCompleted() {
            return "COMPLETED".equals(status);
        }

        public boolean isCancelled() {
            return "CANCELLED".equals(status);
        }
    }

    public static class Campaign {
        public int id;
        public String title;
        public String message;
        public String audience;
        public String status;
        public int recipients;
        public int sent;
        public int failed;
        public long createdOn;

        public Campaign(int id) {
            this.id = id;
        }
    }

    public static class Homestay {
        public String name = "Mountain View Homestay";
        public String description = "A cosy family-run homestay on the ridge above MG Marg, Gangtok. Warm rooms, home-cooked Sikkimese food and a live in-house cab service for all your mountain travel needs.";
        public String location = "MG Marg, Gangtok, Sikkim 737101";
        public String phone = "+91 98320 11223";
        public String email = "stay@mountainview.in";
        public String ownerName = "Tshering Bhutia";
        public String link = "https://stayride.in/h/mountain-view-gangtok";
        public String mapQuery = "MG Marg, Gangtok, Sikkim";
        public List<String> amenities = new ArrayList<>();
        public List<String> photos = new ArrayList<>();
    }
}
