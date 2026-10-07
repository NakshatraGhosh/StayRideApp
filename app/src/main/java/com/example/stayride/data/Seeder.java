package com.example.stayride.data;

import com.example.stayride.util.Dates;

import java.util.Arrays;

public class Seeder {

    public Seeder(DataStore d) {
        seedRooms(d);
        seedGuests(d);
        seedBookings(d);
        seedHistory(d);
        seedDrivers(d);
        seedTrips(d);
        seedTripHistory(d);
        seedCampaigns(d);
        seedProfile(d);
    }

    private void seedRooms(DataStore d) {
        d.rooms.add(new Models.Room(d.nextRoomId++, "101", "Standard", "1st Floor", 1800, "AVAILABLE", "Valley side, 1 double bed"));
        d.rooms.add(new Models.Room(d.nextRoomId++, "102", "Deluxe", "1st Floor", 2600, "OCCUPIED", "Balcony, mountain view"));
        d.rooms.add(new Models.Room(d.nextRoomId++, "103", "Standard", "1st Floor", 1800, "AVAILABLE", "2 single beds"));
        d.rooms.add(new Models.Room(d.nextRoomId++, "104", "Deluxe", "1st Floor", 2600, "RESERVED", "Heater, attached bathroom"));
        d.rooms.add(new Models.Room(d.nextRoomId++, "201", "Suite", "2nd Floor", 4200, "OCCUPIED", "Living area + bedroom"));
        d.rooms.add(new Models.Room(d.nextRoomId++, "202", "Deluxe", "2nd Floor", 2600, "RESERVED", "Corner room, king bed"));
        d.rooms.add(new Models.Room(d.nextRoomId++, "203", "Standard", "2nd Floor", 1800, "MAINTENANCE", "Bathroom plumbing under repair"));
        d.rooms.add(new Models.Room(d.nextRoomId++, "204", "Suite", "2nd Floor", 4200, "AVAILABLE", "Best view, jacuzzi"));
    }

    private void seedGuests(DataStore d) {
        d.guests.add(g(d, "Rahul Sharma", "+91 98301 44521", "rahul.sharma@gmail.com", 3, 12400, -28, true,
                "Prefers a quiet room away from the stairs. Early check-in whenever possible."));
        d.guests.add(g(d, "Priya Verma", "+91 99032 77810", "priya.verma@gmail.com", 2, 9800, -1, true,
                "Vegetarian. Books cabs through the homestay every time."));
        d.guests.add(g(d, "Arjun Mehta", "+91 90071 22334", "arjun.mehta@outlook.com", 1, 0, 0, true, ""));
        d.guests.add(g(d, "Sunita Rai", "+91 94198 55210", "sunita.rai@gmail.com", 2, 7800, -1, false,
                "Travels with an infant, needs a cot."));
        d.guests.add(g(d, "Vikram Singh", "+91 98111 90909", "vikram.singh@gmail.com", 1, 0, 0, true, ""));
        d.guests.add(g(d, "Ananya Bose", "+91 98311 66112", "ananya.bose@gmail.com", 1, 0, 0, true, ""));
        d.guests.add(g(d, "Karan Thapa", "+91 97330 40404", "karan.thapa@gmail.com", 1, 0, 0, false, ""));
        d.guests.add(g(d, "Meera Iyer", "+91 99450 78787", "meera.iyer@gmail.com", 1, 0, 0, true, ""));
        d.guests.add(g(d, "Dev Patel", "+91 98200 31313", "dev.patel@gmail.com", 2, 14200, -12, true,
                "Repeat guest from Mumbai. Likes the corner suite."));
        d.guests.add(g(d, "Ritu Chettri", "+91 99340 56565", "ritu.chettri@gmail.com", 2, 8400, -6, true, ""));
        d.guests.add(g(d, "Sandeep Kumar", "+91 90000 12121", "sandeep.kumar@gmail.com", 1, 0, -150, false, ""));
        d.guests.add(g(d, "Neha Gupta", "+91 98765 43210", "neha.gupta@gmail.com", 1, 0, -110, true, ""));
        d.guests.add(g(d, "Imran Khan", "+91 97654 33322", "imran.khan@gmail.com", 1, 0, -130, false, ""));
        d.guests.add(g(d, "Kavya Nair", "+91 91234 56789", "kavya.nair@gmail.com", 1, 0, -20, true, ""));
    }

    private Models.Guest g(DataStore d, String name, String phone, String email, int visits, int spend,
                           int lastVisitDaysAgo, boolean consent, String notes) {
        Models.Guest g = new Models.Guest(d.nextGuestId++, name, phone, email);
        g.visits = visits;
        g.totalSpending = spend;
        g.lastVisit = lastVisitDaysAgo == 0 ? Dates.today() : Dates.plusDays(lastVisitDaysAgo);
        g.firstVisit = visits > 1 ? Dates.plusDays(lastVisitDaysAgo - 180) : g.lastVisit;
        g.marketingConsent = consent;
        g.notes = notes;
        return g;
    }

    private void seedBookings(DataStore d) {
        // Guests that already exist in the database
        Models.Guest rahul = d.guest(1);
        Models.Guest priya = d.guest(2);
        Models.Guest arjun = d.guest(3);
        Models.Guest sunita = d.guest(4);
        Models.Guest vikram = d.guest(5);
        Models.Guest ananya = d.guest(6);
        Models.Guest karan = d.guest(7);
        Models.Guest meera = d.guest(8);
        Models.Guest dev = d.guest(9);
        Models.Guest ritu = d.guest(10);
        Models.Guest sandeep = d.guest(11);
        Models.Guest neha = d.guest(12);

        add(d, rahul, "102", Dates.today(), Dates.plusDays(2), "CONFIRMED", false, 5200, 2, "StayRide link");
        add(d, priya, "201", Dates.plusDays(-1), Dates.plusDays(1), "CHECKED_IN", true, 8400, 2, "Repeat booking");
        add(d, sunita, "102", Dates.plusDays(-1), Dates.plusDays(2), "CHECKED_IN", false, 7800, 3, "Phone");
        add(d, vikram, "104", Dates.plusDays(2), Dates.plusDays(4), "CONFIRMED", false, 5200, 2, "StayRide link");
        add(d, ananya, "103", Dates.plusDays(1), Dates.plusDays(3), "CONFIRMED", true, 3600, 2, "Walk-in");
        add(d, karan, "202", Dates.plusDays(3), Dates.plusDays(5), "CONFIRMED", false, 5200, 2, "StayRide link");
        add(d, meera, "101", Dates.today(), Dates.plusDays(1), "CONFIRMED", false, 1800, 1, "Walk-in");
        add(d, neha, "202", Dates.today(), Dates.plusDays(2), "CONFIRMED", true, 5200, 2, "WhatsApp");
        add(d, dev, "202", Dates.plusDays(-12), Dates.plusDays(-9), "CHECKED_OUT", true, 7800, 2, "StayRide link");
        add(d, ritu, "204", Dates.plusDays(-6), Dates.plusDays(-4), "CHECKED_OUT", true, 8400, 4, "Repeat booking");
        add(d, rahul, "101", Dates.plusDays(-30), Dates.plusDays(-28), "CHECKED_OUT", true, 3600, 2, "StayRide link");
        add(d, sandeep, "204", Dates.plusDays(5), Dates.plusDays(7), "CANCELLED", false, 8400, 2, "StayRide link");
    }

    private void add(DataStore d, Models.Guest g, String roomNumber, long in, long out, String status,
                     boolean paid, int amount, int guests, String source) {
        Models.Booking b = new Models.Booking(d.nextBookingId++, g.id, g.name, g.phone);
        b.checkIn = in;
        b.checkOut = out;
        b.roomNumber = roomNumber;
        b.status = status;
        b.paid = paid;
        b.amount = amount;
        b.guests = guests;
        b.source = source;
        d.bookings.add(b);
        g.bookingIds.add(b.id);
    }

    private void seedHistory(DataStore d) {
        String[] rooms = {"101", "102", "103", "104", "201", "202", "203", "204"};
        String[] sources = {"StayRide link", "Walk-in", "Phone", "WhatsApp", "Repeat booking"};
        int[] nightly = {1800, 1800, 2600, 2600, 4200, 1800, 2600};
        java.util.Random r = new java.util.Random(42);
        for (int i = 0; i < 108; i++) {
            int daysAgo = 5 + r.nextInt(360);
            long in = Dates.plusDays(-daysAgo);
            int nights = 1 + r.nextInt(4);
            long out = in + nights * Dates.DAY;
            Models.Guest g = d.guests.get(r.nextInt(d.guests.size()));
            int amount = nightly[r.nextInt(nightly.length)] * nights;
            add(d, g, rooms[r.nextInt(rooms.length)], in, out, "CHECKED_OUT",
                    r.nextBoolean(), amount, 1 + r.nextInt(4), sources[r.nextInt(sources.length)]);
        }
    }

    private void seedTripHistory(DataStore d) {
        String[] types = {"SUV", "Sedan", "7-seater", "9-seater"};
        String[][] routes = {
                {"Bagdogra Airport", "Mountain View Homestay"},
                {"Mountain View Homestay", "NJP Station"},
                {"NJP Station", "Mountain View Homestay"},
                {"Mountain View Homestay", "Darjeeling Sightseeing"},
                {"Mountain View Homestay", "Pelling"},
                {"Bagdogra Airport", "Gangtok"}
        };
        String[] times = {"07:30 AM", "09:00 AM", "11:45 AM", "02:15 PM", "06:40 PM", "10:20 PM"};
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < 72; i++) {
            Models.CabTrip t = new Models.CabTrip(d.nextTripId++);
            Models.Guest g = d.guests.get(r.nextInt(d.guests.size()));
            t.guestName = g.name;
            t.phone = g.phone;
            String[] route = routes[r.nextInt(routes.length)];
            t.pickup = route[0];
            t.destination = route[1];
            t.date = Dates.plusDays(-(5 + r.nextInt(360)));
            t.time = times[r.nextInt(times.length)];
            t.passengers = 1 + r.nextInt(7);
            t.vehicleType = types[r.nextInt(types.length)];
            t.estFare = 1500 + r.nextInt(4000);
            t.status = "COMPLETED";
            t.fare = t.estFare;
            t.commission = Math.round(t.fare * 0.12f);
            t.commissionPaid = r.nextBoolean();
            Models.Driver owner = d.drivers.get(i % d.drivers.size());
            t.driverId = owner.id;
            t.driverName = owner.name;
            t.ownerDriver = true;
            d.trips.add(t);
        }
    }

    private void seedDrivers(DataStore d) {
        Models.Driver a = new Models.Driver(d.nextDriverId++, "Pemba Sherpa");
        a.phone = "+91 98320 45451";
        a.vehicle = "Mahindra Bolero";
        a.vehicleType = "SUV";
        a.seats = 7;
        a.rating = 4.8f;
        a.area = "Gangtok / NJP / Bagdogra";
        a.available = true;
        a.active = true;
        a.ownerDriver = true;
        a.trips = Arrays.asList("NJP", "Bagdogra", "Local");
        d.drivers.add(a);

        Models.Driver b = new Models.Driver(d.nextDriverId++, "Bikash Rai");
        b.phone = "+91 99330 12124";
        b.vehicle = "Toyota Innova Crysta";
        b.vehicleType = "SUV";
        b.seats = 7;
        b.rating = 4.9f;
        b.area = "Gangtok / Darjeeling / Bagdogra";
        b.available = true;
        b.active = true;
        b.ownerDriver = true;
        b.trips = Arrays.asList("NJP", "Bagdogra", "Sightseeing", "Local");
        d.drivers.add(b);

        Models.Driver c = new Models.Driver(d.nextDriverId++, "Dawa Bhutia");
        c.phone = "+91 94198 77665";
        c.vehicle = "Maruti Dzire";
        c.vehicleType = "Sedan";
        c.seats = 4;
        c.rating = 4.5f;
        c.area = "Gangtok local";
        c.available = false;
        c.active = true;
        c.ownerDriver = true;
        c.trips = Arrays.asList("Local", "Sightseeing");
        d.drivers.add(c);

        Models.Driver e = new Models.Driver(d.nextDriverId++, "Mingma Tamang");
        e.phone = "+91 97750 88990";
        e.vehicle = "Maruti Ertiga";
        e.vehicleType = "7-seater";
        e.seats = 7;
        e.rating = 4.6f;
        e.area = "Siliguri / NJP";
        e.available = true;
        e.active = false;
        e.ownerDriver = true;
        e.trips = Arrays.asList("NJP", "Bagdogra");
        d.drivers.add(e);

        String[] first = {"Bishnu", "Prakash", "Tenzin", "Sagar", "Deepak", "Hari", "Manoj", "Sunil", "Rakesh",
                "Ajay", "Suraj", "Nabin", "Lhakpa", "Dorjee", "Nima", "Sabina", "Ranjit", "Bimal", "Kiran",
                "Rajesh", "Sujit", "Prabin", "Ganesh", "Tek", "Dev", "Amar", "Jay", "Sanjay", "Pemba", "Mingma",
                "Dhana", "Bikash", "Roshan", "Ajit", "Suresh", "Prabhu", "Nabin", "Tara", "Suman", "Rabin",
                "Yubraj", "Bibek", "Anil", "Prakash", "Dinesh", "Raj", "Mahesh", "Vijay", "Naresh", "Santosh",
                "Deep", "Ravi", "Nitesh", "Aashish"};
        String[] last = {"Tamang", "Rai", "Chettri", "Ghale", "Sherpa", "Bhutia", "Limbu", "Mukhia", "Pradhan",
                "Subba", "Gurung", "Lepcha", "Kumar", "Singh", "Das", "Sarkar", "Biswakarma"};
        String[][] vehicles = {
                {"Toyota Innova Crysta", "SUV", "7"},
                {"Maruti Dzire", "Sedan", "4"},
                {"Mahindra Bolero", "SUV", "7"},
                {"Maruti Ertiga", "7-seater", "7"},
                {"Force Traveller", "9-seater", "9"},
                {"Honda Amaze", "Sedan", "4"},
                {"Toyota Etios", "Sedan", "4"},
                {"Kia Carens", "7-seater", "7"}
        };
        String[] areas = {"NJP Station", "Bagdogra Airport", "Gangtok", "Pelling", "Lachung", "Darjeeling",
                "Kalimpong", "Namchi", "Ravangla", "Siliguri"};
        String[] tripPool = {"NJP", "Bagdogra", "Sightseeing", "Local"};

        for (int i = 0; i < 54; i++) {
            String name = first[i % first.length] + " " + last[(i * 5) % last.length];
            Models.Driver dr = new Models.Driver(d.nextDriverId++, name);
            dr.phone = "+91 9" + (810000000 + i * 1307);
            String[] veh = vehicles[i % vehicles.length];
            dr.vehicle = veh[0];
            dr.vehicleType = veh[1];
            dr.seats = Integer.parseInt(veh[2]);
            dr.rating = Math.round((3.9f + (i % 11) * 0.1f) * 10f) / 10f;
            dr.area = areas[i % areas.length];
            dr.available = (i % 10) != 3 && (i % 10) != 7;
            dr.active = true;
            dr.ownerDriver = false;
            dr.trips.add(tripPool[i % tripPool.length]);
            dr.trips.add(tripPool[(i + 2) % tripPool.length]);
            if (!dr.trips.contains("NJP") && i % 3 == 0) dr.trips.add("NJP");
            d.networkDrivers.add(dr);
        }
    }

    private void seedTrips(DataStore d) {
        Models.CabTrip t1 = new Models.CabTrip(d.nextTripId++);
        t1.guestName = "Rahul Sharma";
        t1.phone = "+91 98301 44521";
        t1.pickup = "Bagdogra Airport";
        t1.destination = "Mountain View Homestay";
        t1.date = Dates.plusDays(3);
        t1.time = "10:30 AM";
        t1.passengers = 4;
        t1.vehicleType = "SUV";
        t1.estFare = 3000;
        t1.status = "NEW_REQUEST";
        d.trips.add(t1);

        Models.CabTrip t2 = new Models.CabTrip(d.nextTripId++);
        t2.guestName = "Priya Verma";
        t2.phone = "+91 99032 77810";
        t2.pickup = "Mountain View Homestay";
        t2.destination = "NJP Station";
        t2.date = Dates.plusDays(1);
        t2.time = "06:15 AM";
        t2.passengers = 3;
        t2.vehicleType = "Sedan";
        t2.estFare = 2500;
        t2.status = "FINDING_DRIVER";
        d.trips.add(t2);

        Models.CabTrip t3 = new Models.CabTrip(d.nextTripId++);
        t3.guestName = "Arjun Mehta";
        t3.phone = "+91 90071 22334";
        t3.pickup = "Mountain View Homestay";
        t3.destination = "Bagdogra Airport";
        t3.date = Dates.plusDays(2);
        t3.time = "09:00 AM";
        t3.passengers = 5;
        t3.vehicleType = "7-seater";
        t3.estFare = 3200;
        t3.status = "DRIVER_ASSIGNED";
        t3.driverId = 2;
        t3.driverName = "Bikash Rai";
        t3.ownerDriver = true;
        d.trips.add(t3);

        Models.CabTrip t4 = new Models.CabTrip(d.nextTripId++);
        t4.guestName = "Sunita Rai";
        t4.phone = "+91 94198 55210";
        t4.pickup = "NJP Station";
        t4.destination = "Mountain View Homestay";
        t4.date = Dates.plusDays(1);
        t4.time = "12:45 PM";
        t4.passengers = 2;
        t4.vehicleType = "Sedan";
        t4.estFare = 2200;
        t4.status = "CONFIRMED";
        t4.driverId = 56;
        t4.driverName = "Sagar Chettri";
        t4.ownerDriver = false;
        d.trips.add(t4);

        Models.CabTrip t5 = new Models.CabTrip(d.nextTripId++);
        t5.guestName = "Vikram Singh";
        t5.phone = "+91 98111 90909";
        t5.pickup = "Mountain View Homestay";
        t5.destination = "Darjeeling Sightseeing";
        t5.date = Dates.today();
        t5.time = "08:00 AM";
        t5.passengers = 4;
        t5.vehicleType = "SUV";
        t5.estFare = 4500;
        t5.status = "TRIP_STARTED";
        t5.driverId = 1;
        t5.driverName = "Pemba Sherpa";
        t5.ownerDriver = true;
        d.trips.add(t5);

        Models.CabTrip t6 = new Models.CabTrip(d.nextTripId++);
        t6.guestName = "Meera Iyer";
        t6.phone = "+91 99450 78787";
        t6.pickup = "Mountain View Homestay";
        t6.destination = "NJP Station";
        t6.date = Dates.plusDays(-2);
        t6.time = "07:00 AM";
        t6.passengers = 3;
        t6.vehicleType = "SUV";
        t6.estFare = 2500;
        t6.status = "COMPLETED";
        t6.fare = 2500;
        t6.commission = 300;
        t6.commissionPaid = false;
        t6.driverId = 57;
        t6.driverName = "Deepak Ghale";
        t6.ownerDriver = false;
        d.trips.add(t6);

        Models.CabTrip t7 = new Models.CabTrip(d.nextTripId++);
        t7.guestName = "Dev Patel";
        t7.phone = "+91 98200 31313";
        t7.pickup = "Bagdogra Airport";
        t7.destination = "Mountain View Homestay";
        t7.date = Dates.plusDays(-5);
        t7.time = "11:20 AM";
        t7.passengers = 6;
        t7.vehicleType = "7-seater";
        t7.estFare = 2000;
        t7.status = "COMPLETED";
        t7.fare = 2000;
        t7.commission = 240;
        t7.commissionPaid = true;
        t7.driverId = 2;
        t7.driverName = "Bikash Rai";
        t7.ownerDriver = true;
        d.trips.add(t7);
    }

    private void seedCampaigns(DataStore d) {
        Models.Campaign c1 = new Models.Campaign(d.nextCampaignId++);
        c1.title = "20% off your next Darjeeling stay";
        c1.message = "Hi {name}! You are invited back to Mountain View Homestay with 20% off your next Darjeeling trip. Book on StayRide.";
        c1.audience = "All guests";
        c1.status = "COMPLETED";
        c1.recipients = 14;
        c1.sent = 13;
        c1.failed = 1;
        c1.createdOn = Dates.plusDays(-9);
        d.campaigns.add(c1);

        Models.Campaign c2 = new Models.Campaign(d.nextCampaignId++);
        c2.title = "Winter special: snow trip to Lachung";
        c2.message = "Hello {name}! Freeze the season with our Lachung snow package. Cab included, 15% off stays this winter.";
        c2.audience = "Repeat guests";
        c2.status = "CREATED";
        c2.recipients = 5;
        c2.createdOn = Dates.plusDays(-1);
        d.campaigns.add(c2);
    }

    private void seedProfile(DataStore d) {
        Models.Homestay h = d.homestay;
        h.amenities = new java.util.ArrayList<>(Arrays.asList(
                "Free Wi-Fi", "24x7 Hot Water", "Private Parking", "Home Breakfast",
                "Air Conditioning", "Power Backup", "Luggage Storage", "Mountain View",
                "Laundry Service", "In-house Cabs (StayRide)"));
        h.photos = new java.util.ArrayList<>(Arrays.asList(
                "Front elevation", "Deluxe room", "Valley view balcony",
                "Dining hall", "Rooftop garden", "Reception"));
    }
}
