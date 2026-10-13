/*
 * StayRide mock backend — single-file Node.js server (no dependencies).
 * Implements the endpoints the Android app calls, with seeded demo data.
 * Run:  node server.js     (listens on 0.0.0.0:8080, base path /v1)
 */
const http = require('http');

const DAY = 86400000;
const PREFIX = '/v1';

function today() { const d = new Date(); d.setHours(0, 0, 0, 0); return d.getTime(); }
function plusDays(n) { return today() + n * DAY; }
function mulberry32(a) { return function () { a |= 0; a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }

let db, ids;

function seed() {
  const rooms = []; let rid = 1;
  const room = (number, type, floor, price, status, note) => rooms.push({ id: rid++, number, type, floor, price, status, note });
  room('101', 'Standard', '1st Floor', 1800, 'AVAILABLE', 'Valley side, 1 double bed');
  room('102', 'Deluxe', '1st Floor', 2600, 'OCCUPIED', 'Balcony, mountain view');
  room('103', 'Standard', '1st Floor', 1800, 'AVAILABLE', '2 single beds');
  room('104', 'Deluxe', '1st Floor', 2600, 'RESERVED', 'Heater, attached bathroom');
  room('201', 'Suite', '2nd Floor', 4200, 'OCCUPIED', 'Living area + bedroom');
  room('202', 'Deluxe', '2nd Floor', 2600, 'RESERVED', 'Corner room, king bed');
  room('203', 'Standard', '2nd Floor', 1800, 'MAINTENANCE', 'Bathroom plumbing under repair');
  room('204', 'Suite', '2nd Floor', 4200, 'AVAILABLE', 'Best view, jacuzzi');

  const guests = []; let gid = 1;
  const guest = (name, phone, email, visits, spend, lastDaysAgo, consent, notes) => {
    const last = lastDaysAgo === 0 ? today() : plusDays(lastDaysAgo);
    const g = {
      id: gid++, name, phone, email, notes: notes || '', visits, totalSpending: spend,
      lastVisit: last, firstVisit: visits > 1 ? plusDays(lastDaysAgo - 180) : last, marketingConsent: consent,
    };
    guests.push(g); return g;
  };
  const rahul = guest('Rahul Sharma', '+91 98301 44521', 'rahul.sharma@gmail.com', 3, 12400, -28, true, 'Prefers a quiet room away from the stairs. Early check-in whenever possible.');
  const priya = guest('Priya Verma', '+91 99032 77810', 'priya.verma@gmail.com', 2, 9800, -1, true, 'Vegetarian. Books cabs through the homestay every time.');
  const arjun = guest('Arjun Mehta', '+91 90071 22334', 'arjun.mehta@outlook.com', 1, 0, 0, true, '');
  const sunita = guest('Sunita Rai', '+91 94198 55210', 'sunita.rai@gmail.com', 2, 7800, -1, false, 'Travels with an infant, needs a cot.');
  const vikram = guest('Vikram Singh', '+91 98111 90909', 'vikram.singh@gmail.com', 1, 0, 0, true, '');
  guest('Ananya Bose', '+91 98311 66112', 'ananya.bose@gmail.com', 1, 0, 0, true, '');
  guest('Karan Thapa', '+91 97330 40404', 'karan.thapa@gmail.com', 1, 0, 0, false, '');
  const meera = guest('Meera Iyer', '+91 99450 78787', 'meera.iyer@gmail.com', 1, 0, 0, true, '');
  const dev = guest('Dev Patel', '+91 98200 31313', 'dev.patel@gmail.com', 2, 14200, -12, true, 'Repeat guest from Mumbai. Likes the corner suite.');
  const ritu = guest('Ritu Chettri', '+91 99340 56565', 'ritu.chettri@gmail.com', 2, 8400, -6, true, '');
  guest('Sandeep Kumar', '+91 90000 12121', 'sandeep.kumar@gmail.com', 1, 0, -150, false, '');
  guest('Neha Gupta', '+91 98765 43210', 'neha.gupta@gmail.com', 1, 0, -110, true, '');
  guest('Imran Khan', '+91 97654 33322', 'imran.khan@gmail.com', 1, 0, -130, false, '');
  guest('Kavya Nair', '+91 91234 56789', 'kavya.nair@gmail.com', 1, 0, -20, true, '');

  const bookings = []; let bid = 1;
  const booking = (g, roomNumber, inDay, outDay, status, paid, amount, n, source) =>
    bookings.push({ id: bid++, guestId: g.id, guestName: g.name, phone: g.phone, checkIn: inDay, checkOut: outDay, roomNumber, guests: n, status, paid, amount, source });

  booking(rahul, '102', today(), plusDays(2), 'CONFIRMED', false, 5200, 2, 'StayRide link');
  booking(priya, '201', plusDays(-1), plusDays(1), 'CHECKED_IN', true, 8400, 2, 'Repeat booking');
  booking(sunita, '102', plusDays(-1), plusDays(2), 'CHECKED_IN', false, 7800, 3, 'Phone');
  booking(vikram, '104', plusDays(2), plusDays(4), 'CONFIRMED', false, 5200, 2, 'StayRide link');
  booking(guests[5], '103', plusDays(1), plusDays(3), 'CONFIRMED', true, 3600, 2, 'Walk-in');
  booking(guests[6], '202', plusDays(3), plusDays(5), 'CONFIRMED', false, 5200, 2, 'StayRide link');
  booking(meera, '101', today(), plusDays(1), 'CONFIRMED', false, 1800, 1, 'Walk-in');
  booking(guests[11], '202', today(), plusDays(2), 'CONFIRMED', true, 5200, 2, 'WhatsApp');
  booking(dev, '202', plusDays(-12), plusDays(-9), 'CHECKED_OUT', true, 7800, 2, 'StayRide link');
  booking(ritu, '204', plusDays(-6), plusDays(-4), 'CHECKED_OUT', true, 8400, 4, 'Repeat booking');
  booking(rahul, '101', plusDays(-30), plusDays(-28), 'CHECKED_OUT', true, 3600, 2, 'StayRide link');
  booking(guests[10], '204', plusDays(5), plusDays(7), 'CANCELLED', false, 8400, 2, 'StayRide link');

  const r = mulberry32(42);
  const rn = (n) => Math.floor(r() * n);
  const roomNums = ['101', '102', '103', '104', '201', '202', '203', '204'];
  const sources = ['StayRide link', 'Walk-in', 'Phone', 'WhatsApp', 'Repeat booking'];
  const nightly = [1800, 1800, 2600, 2600, 4200, 1800, 2600];
  for (let i = 0; i < 108; i++) {
    const daysAgo = 5 + rn(360);
    const inMs = plusDays(-daysAgo);
    const nights = 1 + rn(4);
    const g = guests[rn(guests.length)];
    bookings.push({
      id: bid++, guestId: g.id, guestName: g.name, phone: g.phone,
      checkIn: inMs, checkOut: inMs + nights * DAY, roomNumber: roomNums[rn(roomNums.length)],
      guests: 1 + rn(4), status: 'CHECKED_OUT', paid: r() > 0.4,
      amount: nightly[rn(nightly.length)] * nights, source: sources[rn(sources.length)],
    });
  }

  const drivers = []; let did = 1;
  const driver = (name, phone, vehicle, vehicleType, seats, rating, area, trips, available, active, ownerDriver) =>
    drivers.push({ id: did++, name, phone, vehicle, vehicleType, seats, rating, area, trips, available, active, ownerDriver });
  driver('Pemba Sherpa', '+91 98320 45451', 'Mahindra Bolero', 'SUV', 7, 4.8, 'Gangtok / NJP / Bagdogra', ['NJP', 'Bagdogra', 'Local'], true, true, true);
  driver('Bikash Rai', '+91 99330 12124', 'Toyota Innova Crysta', 'SUV', 7, 4.9, 'Gangtok / Darjeeling / Bagdogra', ['NJP', 'Bagdogra', 'Sightseeing', 'Local'], true, true, true);
  driver('Dawa Bhutia', '+91 94198 77665', 'Maruti Dzire', 'Sedan', 4, 4.5, 'Gangtok local', ['Local', 'Sightseeing'], false, true, true);
  driver('Mingma Tamang', '+91 97750 88990', 'Maruti Ertiga', '7-seater', 7, 4.6, 'Siliguri / NJP', ['NJP', 'Bagdogra'], true, false, true);

  const first = ['Bishnu', 'Prakash', 'Tenzin', 'Sagar', 'Deepak', 'Hari', 'Manoj', 'Sunil', 'Rakesh', 'Ajay', 'Suraj', 'Nabin', 'Lhakpa', 'Dorjee', 'Nima', 'Sabina', 'Ranjit', 'Bimal', 'Kiran', 'Rajesh', 'Sujit', 'Prabin', 'Ganesh', 'Tek', 'Dev', 'Amar', 'Jay', 'Sanjay', 'Pemba', 'Mingma', 'Dhana', 'Bikash', 'Roshan', 'Ajit', 'Suresh', 'Prabhu', 'Nabin', 'Tara', 'Suman', 'Rabin', 'Yubraj', 'Bibek', 'Anil', 'Prakash', 'Dinesh', 'Raj', 'Mahesh', 'Vijay', 'Naresh', 'Santosh', 'Deep', 'Ravi', 'Nitesh', 'Aashish'];
  const last = ['Tamang', 'Rai', 'Chettri', 'Ghale', 'Sherpa', 'Bhutia', 'Limbu', 'Mukhia', 'Pradhan', 'Subba', 'Gurung', 'Lepcha', 'Kumar', 'Singh', 'Das', 'Sarkar', 'Biswakarma'];
  const vehicles = [['Toyota Innova Crysta', 'SUV', 7], ['Maruti Dzire', 'Sedan', 4], ['Mahindra Bolero', 'SUV', 7], ['Maruti Ertiga', '7-seater', 7], ['Force Traveller', '9-seater', 9], ['Honda Amaze', 'Sedan', 4], ['Toyota Etios', 'Sedan', 4], ['Kia Carens', '7-seater', 7]];
  const areas = ['NJP Station', 'Bagdogra Airport', 'Gangtok', 'Pelling', 'Lachung', 'Darjeeling', 'Kalimpong', 'Namchi', 'Ravangla', 'Siliguri'];
  const tripPool = ['NJP', 'Bagdogra', 'Sightseeing', 'Local'];
  for (let i = 0; i < 54; i++) {
    const name = first[i % first.length] + ' ' + last[(i * 5) % last.length];
    const veh = vehicles[i % vehicles.length];
    const tr = [tripPool[i % tripPool.length], tripPool[(i + 2) % tripPool.length]];
    if (!tr.includes('NJP') && i % 3 === 0) tr.push('NJP');
    driver(name, '+91 9' + (810000000 + i * 1307), veh[0], veh[1], veh[2],
      Math.round((3.9 + (i % 11) * 0.1) * 10) / 10, areas[i % areas.length], tr,
      (i % 10) !== 3 && (i % 10) !== 7, true, false);
  }

  const trips = []; let tid = 1;
  const trip = (o) => { trips.push(Object.assign({ id: tid++, driverId: null, driverName: null, ownerDriver: false, fare: 0, commission: 0, commissionPaid: false }, o)); return trips[trips.length - 1]; };
  trip({ guestName: 'Rahul Sharma', phone: '+91 98301 44521', pickup: 'Bagdogra Airport', destination: 'Mountain View Homestay', date: plusDays(3), time: '10:30 AM', passengers: 4, vehicleType: 'SUV', estFare: 3000, status: 'NEW_REQUEST' });
  trip({ guestName: 'Priya Verma', phone: '+91 99032 77810', pickup: 'Mountain View Homestay', destination: 'NJP Station', date: plusDays(1), time: '06:15 AM', passengers: 3, vehicleType: 'Sedan', estFare: 2500, status: 'FINDING_DRIVER' });
  trip({ guestName: 'Arjun Mehta', phone: '+91 90071 22334', pickup: 'Mountain View Homestay', destination: 'Bagdogra Airport', date: plusDays(2), time: '09:00 AM', passengers: 5, vehicleType: '7-seater', estFare: 3200, status: 'DRIVER_ASSIGNED', driverId: 2, driverName: 'Bikash Rai', ownerDriver: true });
  trip({ guestName: 'Sunita Rai', phone: '+91 94198 55210', pickup: 'NJP Station', destination: 'Mountain View Homestay', date: plusDays(1), time: '12:45 PM', passengers: 2, vehicleType: 'Sedan', estFare: 2200, status: 'CONFIRMED', driverId: 56, driverName: 'Sagar Chettri', ownerDriver: false });
  trip({ guestName: 'Vikram Singh', phone: '+91 98111 90909', pickup: 'Mountain View Homestay', destination: 'Darjeeling Sightseeing', date: today(), time: '08:00 AM', passengers: 4, vehicleType: 'SUV', estFare: 4500, status: 'TRIP_STARTED', driverId: 1, driverName: 'Pemba Sherpa', ownerDriver: true });
  trip({ guestName: 'Meera Iyer', phone: '+91 99450 78787', pickup: 'Mountain View Homestay', destination: 'NJP Station', date: plusDays(-2), time: '07:00 AM', passengers: 3, vehicleType: 'SUV', estFare: 2500, status: 'COMPLETED', fare: 2500, commission: 300, driverId: 57, driverName: 'Deepak Ghale', ownerDriver: false });
  trip({ guestName: 'Dev Patel', phone: '+91 98200 31313', pickup: 'Bagdogra Airport', destination: 'Mountain View Homestay', date: plusDays(-5), time: '11:20 AM', passengers: 6, vehicleType: '7-seater', estFare: 2000, status: 'COMPLETED', fare: 2000, commission: 240, commissionPaid: true, driverId: 2, driverName: 'Bikash Rai', ownerDriver: true });

  const r2 = mulberry32(7);
  const rn2 = (n) => Math.floor(r2() * n);
  const routeSet = [['Bagdogra Airport', 'Mountain View Homestay'], ['Mountain View Homestay', 'NJP Station'], ['NJP Station', 'Mountain View Homestay'], ['Mountain View Homestay', 'Darjeeling Sightseeing'], ['Mountain View Homestay', 'Pelling'], ['Bagdogra Airport', 'Gangtok']];
  const times = ['07:30 AM', '09:00 AM', '11:45 AM', '02:15 PM', '06:40 PM', '10:20 PM'];
  const vtypes = ['SUV', 'Sedan', '7-seater', '9-seater'];
  const ownerDrivers = drivers.filter((d) => d.ownerDriver);
  for (let i = 0; i < 72; i++) {
    const g = guests[rn2(guests.length)];
    const rt = routeSet[rn2(routeSet.length)];
    const est = 1500 + rn2(4000);
    const od = ownerDrivers[i % ownerDrivers.length];
    trip({ guestName: g.name, phone: g.phone, pickup: rt[0], destination: rt[1], date: plusDays(-(5 + rn2(360))), time: times[rn2(times.length)], passengers: 1 + rn2(7), vehicleType: vtypes[rn2(vtypes.length)], estFare: est, status: 'COMPLETED', fare: est, commission: Math.round(est * 0.12), commissionPaid: r2() > 0.5, driverId: od.id, driverName: od.name, ownerDriver: true });
  }

  const campaigns = []; let cid = 1;
  campaigns.push({ id: cid++, title: '20% off your next Darjeeling stay', message: 'Hi {name}! You are invited back to Mountain View Homestay with 20% off your next Darjeeling trip. Book on StayRide.', audience: 'All guests', status: 'COMPLETED', recipients: 14, sent: 13, failed: 1, createdOn: plusDays(-9) });
  campaigns.push({ id: cid++, title: 'Winter special: snow trip to Lachung', message: 'Hello {name}! Freeze the season with our Lachung snow package. Cab included, 15% off stays this winter.', audience: 'Repeat guests', status: 'CREATED', recipients: 5, sent: 0, failed: 0, createdOn: plusDays(-1) });

  const homestay = {
    name: 'Mountain View Homestay',
    description: 'A cosy family-run homestay on the ridge above MG Marg, Gangtok. Warm rooms, home-cooked Sikkimese food and a live in-house cab service for all your mountain travel needs.',
    location: 'MG Marg, Gangtok, Sikkim 737101',
    phone: '+91 98320 11223',
    email: 'stay@mountainview.in',
    ownerName: 'Tshering Bhutia',
    link: 'https://stayride.in/h/mountain-view-gangtok',
    mapQuery: 'MG Marg, Gangtok, Sikkim',
    amenities: ['Free Wi-Fi', '24x7 Hot Water', 'Private Parking', 'Home Breakfast', 'Air Conditioning', 'Power Backup', 'Luggage Storage', 'Mountain View', 'Laundry Service', 'In-house Cabs (StayRide)'],
    photos: ['Front elevation', 'Deluxe room', 'Valley view balcony', 'Dining hall', 'Rooftop garden', 'Reception'],
  };

  db = { rooms, guests, bookings, drivers, trips, campaigns, homestay };
  ids = { room: rid, guest: gid, booking: bid, driver: did, trip: tid, campaign: cid };
}

seed();

// ---------------------------------------------------------------- helpers
const roomStatusFor = (b) => (b.status === 'CONFIRMED' ? 'RESERVED' : b.status === 'CHECKED_IN' ? 'OCCUPIED' : 'AVAILABLE');
function refreshGuest(gid) {
  const g = db.guests.find((x) => x.id === gid);
  if (!g) return;
  const list = db.bookings.filter((b) => b.guestId === gid && b.status !== 'CANCELLED');
  if (list.length) {
    g.visits = list.length;
    g.totalSpending = list.reduce((s, b) => s + b.amount, 0);
    g.firstVisit = Math.min(...list.map((b) => b.checkIn));
    g.lastVisit = Math.max(...list.map((b) => b.checkIn));
  }
}
function estimate(vehicleType) {
  switch (vehicleType) {
    case 'Sedan': return 2200;
    case '7-seater': return 3200;
    case '9-seater': return 4200;
    default: return 3000;
  }
}
function recipientsFor(audience) {
  const consented = db.guests.filter((g) => g.marketingConsent);
  const cut60 = today() - 60 * DAY, cut90 = today() - 90 * DAY;
  switch (audience) {
    case 'Repeat guests': return consented.filter((g) => g.visits > 1).length;
    case 'Recent guests': return consented.filter((g) => g.lastVisit >= cut60).length;
    case 'Inactive guests': return consented.filter((g) => g.lastVisit < cut90).length;
    default: return consented.length;
  }
}

function send(res, code, obj) {
  const body = obj === undefined ? '' : JSON.stringify(obj);
  res.writeHead(code, { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) });
  res.end(body);
}
const sendArray = (res, arr) => send(res, 200, arr);
const fail = (res, code, err, msg) => send(res, code, { error: { code: err, message: msg } });

function body(req) {
  return new Promise((resolve) => {
    let d = '';
    req.on('data', (c) => (d += c));
    req.on('end', () => { try { resolve(d ? JSON.parse(d) : {}); } catch { resolve({}); } });
  });
}

// ---------------------------------------------------------------- server
const server = http.createServer(async (req, res) => {
  const u = new URL(req.url, 'http://localhost');
  let path = u.pathname;
  if (!path.startsWith(PREFIX)) return fail(res, 404, 'NOT_FOUND', 'Unknown path');
  path = path.slice(PREFIX.length) || '/';
  const seg = path.split('/').filter(Boolean);
  const method = req.method.toUpperCase();
  const q = u.searchParams;
  const b = ['POST', 'PUT', 'PATCH'].includes(method) ? await body(req) : {};
  const s = (segs, i) => parseInt(segs[i], 10);

  try {
    // ---- auth (accepts anything) ----
    if (seg[0] === 'auth' && (seg[1] === 'login' || seg[1] === 'signup')) {
      const owner = { id: 1, name: b.name || 'Tshering Bhutia', phone: b.phone || '+91 98320 11223', email: b.email || 'stay@mountainview.in' };
      if (b.homestayName) db.homestay.name = b.homestayName;
      if (b.location) db.homestay.location = b.location;
      if (b.name) db.homestay.ownerName = b.name;
      return send(res, seg[1] === 'signup' ? 201 : 200, {
        accessToken: 'mock-access-token', refreshToken: 'mock-refresh-token',
        owner, homestay: { id: 1, name: db.homestay.name, link: db.homestay.link },
      });
    }
    if (seg[0] === 'auth' && seg[1] === 'refresh') {
      return send(res, 200, { accessToken: 'mock-access-token', refreshToken: 'mock-refresh-token', owner: { id: 1, name: db.homestay.ownerName }, homestay: { id: 1, name: db.homestay.name, link: db.homestay.link } });
    }
    if (seg[0] === 'auth' && seg[1] === 'logout') return send(res, 204);

    if (seg[0] === 'dev' && seg[1] === 'seed') { seed(); return send(res, 200, {}); }

    // ---- homestay ----
    if (seg[0] === 'homestay') {
      if (method === 'GET') return send(res, 200, db.homestay);
      if (method === 'PUT') { db.homestay = Object.assign({}, db.homestay, b); return send(res, 200, db.homestay); }
    }

    // ---- rooms ----
    if (seg[0] === 'rooms') {
      if (seg.length === 1 && method === 'GET') return sendArray(res, db.rooms);
      if (seg.length === 1 && method === 'POST') {
        if (db.rooms.some((r) => r.number === b.number)) return fail(res, 409, 'ROOM_NUMBER_TAKEN', 'Room number already exists');
        const r = { id: ids.room++, number: b.number, type: b.type, floor: b.floor || '', price: b.price || 0, status: b.status || 'AVAILABLE', note: b.note || '' };
        db.rooms.push(r); return send(res, 201, r);
      }
      const id = s(seg, 1);
      const r = db.rooms.find((x) => x.id === id);
      if (!r) return fail(res, 404, 'ROOM_NOT_FOUND', 'Room not found');
      if (seg.length === 2 && method === 'PUT') { Object.assign(r, { number: b.number, type: b.type, floor: b.floor, price: b.price, status: b.status, note: b.note }); return send(res, 200, r); }
      if (seg.length === 2 && method === 'DELETE') { db.rooms = db.rooms.filter((x) => x.id !== id); return send(res, 200, {}); }
      if (seg[2] === 'status' && method === 'PATCH') { r.status = b.status; return send(res, 200, r); }
    }

    // ---- guests ----
    if (seg[0] === 'guests') {
      if (seg.length === 1 && method === 'GET') return sendArray(res, db.guests);
      if (seg.length === 1 && method === 'POST') {
        const g = { id: ids.guest++, name: b.name, phone: b.phone, email: b.email || '', notes: b.notes || '', visits: 0, totalSpending: 0, firstVisit: today(), lastVisit: today(), marketingConsent: !!b.marketingConsent };
        db.guests.push(g); return send(res, 201, g);
      }
      const id = s(seg, 1);
      const g = db.guests.find((x) => x.id === id);
      if (!g) return fail(res, 404, 'GUEST_NOT_FOUND', 'Guest not found');
      if (seg[2] === 'bookings' && method === 'GET') return sendArray(res, db.bookings.filter((x) => x.guestId === id));
      if (seg.length === 2 && method === 'PUT') { Object.assign(g, { name: b.name, phone: b.phone, email: b.email, notes: b.notes, marketingConsent: !!b.marketingConsent }); return send(res, 200, g); }
      if (seg.length === 2 && method === 'DELETE') { db.guests = db.guests.filter((x) => x.id !== id); return send(res, 200, {}); }
    }

    // ---- bookings ----
    if (seg[0] === 'bookings') {
      if (seg.length === 1 && method === 'GET') {
        let list = db.bookings.slice();
        if (q.get('status')) list = list.filter((x) => x.status === q.get('status'));
        return sendArray(res, list);
      }
      if (seg.length === 1 && method === 'POST') {
        let g = b.guestId ? db.guests.find((x) => x.id === b.guestId) : null;
        if (!g && b.guestPhone) g = db.guests.find((x) => x.phone === b.guestPhone);
        if (!g) {
          g = { id: ids.guest++, name: b.guestName, phone: b.guestPhone, email: b.guestEmail || '', notes: '', visits: 0, totalSpending: 0, firstVisit: today(), lastVisit: today(), marketingConsent: false };
          db.guests.push(g);
        }
        const room = db.rooms.find((r) => r.number === b.roomNumber);
        const bk = { id: ids.booking++, guestId: g.id, guestName: g.name, phone: g.phone, checkIn: b.checkIn, checkOut: b.checkOut, roomNumber: b.roomNumber, guests: b.guests || 1, status: 'CONFIRMED', paid: false, amount: b.amount || 0, source: b.source || 'Walk-in' };
        db.bookings.push(bk);
        if (room) room.status = 'RESERVED';
        return send(res, 201, bk);
      }
      const id = s(seg, 1);
      const bk = db.bookings.find((x) => x.id === id);
      if (!bk) return fail(res, 404, 'BOOKING_NOT_FOUND', 'Booking not found');
      const room = db.rooms.find((r) => r.number === bk.roomNumber);
      const act = seg[2];
      if (act === 'check-in' && method === 'POST') { bk.status = 'CHECKED_IN'; if (room) room.status = 'OCCUPIED'; refreshGuest(bk.guestId); return send(res, 200, bk); }
      if (act === 'check-out' && method === 'POST') { bk.status = 'CHECKED_OUT'; if (room) room.status = 'AVAILABLE'; refreshGuest(bk.guestId); return send(res, 200, bk); }
      if (act === 'cancel' && method === 'POST') { bk.status = 'CANCELLED'; if (room) room.status = 'AVAILABLE'; return send(res, 200, bk); }
      if (act === 'payment' && method === 'PATCH') { bk.paid = !!b.paid; return send(res, 200, bk); }
      if (act === 'room' && method === 'POST') {
        const old = db.rooms.find((r) => r.number === bk.roomNumber);
        const next = db.rooms.find((r) => r.number === b.roomNumber);
        if (old) old.status = 'AVAILABLE';
        bk.roomNumber = b.roomNumber;
        if (next) next.status = bk.status === 'CHECKED_IN' ? 'OCCUPIED' : 'RESERVED';
        return send(res, 200, bk);
      }
      if (seg.length === 2 && method === 'DELETE') { if (room) room.status = 'AVAILABLE'; db.bookings = db.bookings.filter((x) => x.id !== id); return send(res, 200, {}); }
    }

    // ---- commission ----
    if (seg[0] === 'commission') {
      const done = db.trips.filter((t) => t.status === 'COMPLETED');
      if (seg[1] === 'summary') return send(res, 200, {
        earned: done.reduce((a, t) => a + t.commission, 0),
        pending: done.filter((t) => !t.commissionPaid).reduce((a, t) => a + t.commission, 0),
        paid: done.filter((t) => t.commissionPaid).reduce((a, t) => a + t.commission, 0),
      });
      if (seg[1] === 'ledger') return sendArray(res, done);
    }

    // ---- drivers ----
    if (seg[0] === 'drivers') {
      if (seg.length === 1 && method === 'GET') {
        const scope = q.get('scope');
        let list = db.drivers;
        if (scope === 'mine') list = list.filter((d) => d.ownerDriver);
        else if (scope === 'network') list = list.filter((d) => !d.ownerDriver);
        return sendArray(res, list);
      }
      if (seg.length === 1 && method === 'POST') {
        const d = { id: ids.driver++, name: b.name, phone: b.phone, vehicle: b.vehicle, vehicleType: b.vehicleType, seats: b.seats || 4, rating: b.rating || 4.5, area: b.area || 'Gangtok', trips: b.trips || [], available: b.available !== false, active: true, ownerDriver: true };
        db.drivers.push(d); return send(res, 201, d);
      }
      const id = s(seg, 1);
      const d = db.drivers.find((x) => x.id === id);
      if (!d) return fail(res, 404, 'DRIVER_NOT_FOUND', 'Driver not found');
      if (seg.length === 2 && method === 'GET') return send(res, 200, d);
      if (seg.length === 2 && method === 'PUT') { Object.assign(d, { name: b.name, phone: b.phone, vehicle: b.vehicle, vehicleType: b.vehicleType, seats: b.seats, rating: b.rating, area: b.area, trips: b.trips || d.trips, available: b.available !== undefined ? b.available : d.available }); return send(res, 200, d); }
      if (seg.length === 2 && method === 'DELETE') { d.active = false; return send(res, 200, d); }
      if (seg[2] === 'availability' && method === 'PATCH') { d.available = !!b.available; return send(res, 200, d); }
      if (seg[2] === 'active' && method === 'PATCH') { d.active = !!b.active; return send(res, 200, d); }
    }

    // ---- trips ----
    if (seg[0] === 'trips') {
      if (seg.length === 1 && method === 'GET') {
        const tab = q.get('tab');
        let list = db.trips.slice();
        if (tab === 'requests') list = list.filter((t) => ['NEW_REQUEST', 'FINDING_DRIVER', 'DRIVER_ASSIGNED'].includes(t.status));
        else if (tab === 'trips') list = list.filter((t) => ['CONFIRMED', 'TRIP_STARTED', 'COMPLETED'].includes(t.status));
        return sendArray(res, list);
      }
      if (seg.length === 1 && method === 'POST') {
        const t = { id: ids.trip++, guestName: b.guestName, phone: b.phone, pickup: b.pickup, destination: b.destination, date: b.date, time: b.time, passengers: b.passengers || 1, vehicleType: b.vehicleType, estFare: estimate(b.vehicleType), status: 'NEW_REQUEST', driverId: null, driverName: null, ownerDriver: false, fare: 0, commission: 0, commissionPaid: false };
        db.trips.push(t); return send(res, 201, t);
      }
      if (seg[1] === 'estimate') return send(res, 200, { estFare: estimate(q.get('vehicleType')) });
      if (seg[1] === 'match') return sendArray(res, db.drivers.filter((d) => d.ownerDriver ? d.active && d.available : d.available));
      const id = s(seg, 1);
      const t = db.trips.find((x) => x.id === id);
      if (!t) return fail(res, 404, 'TRIP_NOT_FOUND', 'Trip not found');
      if (seg.length === 2 && method === 'GET') return send(res, 200, t);
      const act = seg[2];
      if (act === 'assign' && method === 'POST') {
        t.driverId = b.driverId;
        const d = db.drivers.find((x) => x.id === b.driverId);
        t.driverName = d ? d.name : '';
        t.ownerDriver = d ? d.ownerDriver : false;
        t.status = 'DRIVER_ASSIGNED';
        return send(res, 200, t);
      }
      if (act === 'accept' && method === 'POST') { t.status = 'CONFIRMED'; return send(res, 200, t); }
      if (act === 'reassign' && method === 'POST') { t.status = 'FINDING_DRIVER'; t.driverId = null; t.driverName = null; t.ownerDriver = false; return send(res, 200, t); }
      if (act === 'start' && method === 'POST') { t.status = 'TRIP_STARTED'; return send(res, 200, t); }
      if (act === 'complete' && method === 'POST') { t.status = 'COMPLETED'; t.fare = t.fare || t.estFare; t.commission = Math.round(t.fare * 0.12); return send(res, 200, t); }
      if (act === 'cancel' && method === 'POST') { t.status = 'CANCELLED'; return send(res, 200, t); }
      if (act === 'commission' && seg[3] === 'pay' && method === 'POST') { t.commissionPaid = !!b.paid; return send(res, 200, t); }
    }

    // ---- campaigns ----
    if (seg[0] === 'campaigns') {
      if (seg.length === 1 && method === 'GET') return sendArray(res, db.campaigns);
      if (seg.length === 1 && method === 'POST') {
        const c = { id: ids.campaign++, title: b.title, message: b.message, audience: b.audience, status: 'CREATED', recipients: recipientsFor(b.audience), sent: 0, failed: 0, createdOn: Date.now() };
        db.campaigns.push(c); return send(res, 201, c);
      }
      const id = s(seg, 1);
      const c = db.campaigns.find((x) => x.id === id);
      if (!c) return fail(res, 404, 'CAMPAIGN_NOT_FOUND', 'Campaign not found');
      if (seg[2] === 'send' && method === 'POST') {
        c.status = 'SENDING';
        setTimeout(() => { c.status = 'COMPLETED'; c.sent = Math.max(0, c.recipients - 1); c.failed = c.recipients - c.sent; }, 800);
        return send(res, 200, c);
      }
      if (seg.length === 2 && method === 'DELETE') { db.campaigns = db.campaigns.filter((x) => x.id !== id); return send(res, 200, {}); }
    }

    // ---- dashboard / analytics (optional) ----
    if (seg[0] === 'dashboard' && seg[1] === 'summary') {
      const oc = db.rooms.filter((r) => r.status === 'OCCUPIED' || r.status === 'RESERVED').length;
      const now = new Date();
      const month = db.bookings.filter((b) => b.status !== 'CANCELLED' && new Date(b.checkIn).getMonth() === now.getMonth() && new Date(b.checkIn).getFullYear() === now.getFullYear());
      const rev = month.reduce((a, b) => a + b.amount, 0);
      return send(res, 200, {
        todayCheckIns: db.bookings.filter((b) => b.checkIn === today() && b.status !== 'CANCELLED').length,
        currentGuests: db.bookings.filter((b) => b.status === 'CHECKED_IN').reduce((a, b) => a + Math.max(1, b.guests), 0),
        availableRooms: db.rooms.filter((r) => r.status === 'AVAILABLE').length,
        occupancyPercent: db.rooms.length ? Math.round(oc * 100 / db.rooms.length) : 0,
        monthRevenue: rev, monthBookings: month.length, totalBookings: db.bookings.length,
        avgBookingValue: month.length ? Math.round(rev / month.length) : 0,
      });
    }

    return fail(res, 404, 'NOT_FOUND', `No route for ${method} ${path}`);
  } catch (e) {
    return fail(res, 500, 'SERVER_ERROR', e.message);
  }
});

const PORT = process.env.PORT || 8080;
server.listen(PORT, '0.0.0.0', () => console.log(`StayRide mock backend on http://0.0.0.0:${PORT}${PREFIX}`));
