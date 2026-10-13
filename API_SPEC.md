# StayRide — Backend API Specification

**For:** backend developer
**App:** StayRide — a single-owner homestay + cab management Android app
**Base URL:** `https://api.example.com/v1` (dev: `http://<host>:8080/v1`)
**Content type:** `application/json` for every request/response
**Frontend source of truth:** the Android app in this repo. Field names below **must match exactly** — the app parses JSON into typed models and any renamed/missing key will silently render as empty.

---

## 0. How to read this document

Every endpoint entry has the same shape:

```
METHOD /path
Purpose:   one line
Auth:      required?
Request:   JSON body (or query params)
Response:  JSON body
Errors:    non-200 cases
Notes:     side effects / rules
```

Two endpoint tiers:

- **CORE** — the app calls this **today**, on this exact path, with these exact keys. Non-negotiable for the app to work.
- **RECOMMENDED** — described in the product flow but currently computed on-device. Build them if you want the app to move that logic server-side (future-proofing). Marked clearly.

---

## 1. Global conventions

| Topic | Decision |
|---|---|
| Protocol | REST over HTTPS, JSON |
| Auth | JWT bearer. Owner logs in once. Send `Authorization: Bearer <accessToken>` on every `/api/**` call |
| Tenancy | Every entity is scoped to the logged-in owner's `homestayId`. Never leak rows across tenants |
| Timestamps | **epoch milliseconds, int64** (`long` in the app) |
| Date-only fields | `checkIn`, `checkOut`, trip `date` = epoch millis at **local midnight** |
| Money | **integer INR, no decimals** (`1800`, not `1800.00`) |
| IDs | **integer, auto-increment** (app types are `int`; do not use UUIDs/Strings) |
| Enums | **UPPERCASE strings**, exactly as listed in §3 |
| Booleans | real JSON booleans (`true`/`false`), never `"true"` |
| Nullable | `driverId` may be `null`; everything else should be present |
| Lists | all list endpoints return a **bare JSON array** (`[...]`), not `{data:[...]}` |
| Objects | detail endpoints return a **bare JSON object** |
| Pagination | optional `?page=&size=` on list endpoints (app currently fetches everything; ignore if unused) |
| Error body | `{ "error": { "code": "ROOM_OCCUPIED", "message": "Human readable" } }` |
| Error HTTP | 400 validation · 401 auth · 403 forbidden · 404 not found · 409 conflict · 500 server |

### Error example
```json
{ "error": { "code": "ROOM_OVERLAP", "message": "Room 102 is already booked on these dates." } }
```

---

## 2. Auth flow

> The app **now has a login screen**: `SplashActivity` → `LoginActivity` (email/password sign in + sign up) → `MainActivity`.
> On sign up the app immediately calls `POST /dev/seed` to populate a demo dataset (see §11). If you don't want seeding, return 404 for that call — the app ignores failures there.

### 2.1 `POST /auth/signup` — CORE
Create an owner account and their homestay in one call.

Request:
```json
{
  "name": "Tshering Bhutia",
  "phone": "+91 98320 11223",
  "email": "stay@mountainview.in",
  "password": "secret123",
  "homestayName": "Mountain View Homestay",
  "location": "MG Marg, Gangtok, Sikkim 737101"
}
```

Response `201`:
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "owner":    { "id": 1, "name": "Tshering Bhutia", "phone": "+91 98320 11223", "email": "stay@mountainview.in" },
  "homestay": { "id": 1, "name": "Mountain View Homestay", "link": "https://stayride.in/h/mountain-view-gangtok" }
}
```

Errors: `409 EMAIL_TAKEN`, `400 VALIDATION_ERROR`.

### 2.2 `POST /auth/login` — CORE
Request:
```json
{ "email": "stay@mountainview.in", "password": "secret123" }
```
Response `200`: same shape as signup `{ accessToken, refreshToken, owner, homestay }`.
Errors: `401 INVALID_CREDENTIALS`.

### 2.3 `POST /auth/refresh` — CORE
The app calls this automatically when any request returns `401`.

Request:
```json
{ "refreshToken": "eyJhbGciOi..." }
```
Response `200`: `{ accessToken, refreshToken, owner, homestay }`.
Errors: `401` → the app clears the session and returns the user to Login.

### 2.4 `POST /auth/logout` — RECOMMENDED
Invalidate the refresh token. Response `204`.

**Token payload should carry:** `ownerId`, `homestayId`, `exp`.

---

## 3. Enums & status machines

### 3.1 Room status
`AVAILABLE` · `RESERVED` · `OCCUPIED` · `MAINTENANCE`

```
AVAILABLE --booking created--> RESERVED --check-in--> OCCUPIED
   ^                              |                       |
   +---- check-out / cancel ------+-----------------------+
OCCUPIED/AVAILABLE <-> MAINTENANCE   (manual toggle from Rooms screen)
```

### 3.2 Booking status
`CONFIRMED` · `CHECKED_IN` · `CHECKED_OUT` · `CANCELLED`

```
CONFIRMED --check-in--> CHECKED_IN --check-out--> CHECKED_OUT
    |                       |
    +-------- cancel -------+--> CANCELLED
```

### 3.3 Booking source
`"StayRide link"` · `"Walk-in"` · `"Phone"` · `"WhatsApp"` · `"Repeat booking"` (exact strings, mixed case)

### 3.4 Driver vehicle type
`"SUV"` · `"Sedan"` · `"7-seater"` · `"9-seater"`

### 3.5 Driver trip routes (`trips[]`)
`"Local"` · `"NJP"` · `"Bagdogra"` · `"Sightseeing"`

### 3.6 Cab trip status
`NEW_REQUEST` · `FINDING_DRIVER` · `DRIVER_ASSIGNED` · `CONFIRMED` · `TRIP_STARTED` · `COMPLETED` · `CANCELLED`

```
NEW_REQUEST --accept--> FINDING_DRIVER --assign--> DRIVER_ASSIGNED
     ^                        |                            | accept
     | re-search              |                            v
     +------------------------+                       CONFIRMED
                                                           | start
                                                           v
                                                    TRIP_STARTED --complete--> COMPLETED
(any state) --cancel--> CANCELLED
```

### 3.7 Campaign audience
`"All guests"` · `"Repeat guests"` · `"Recent guests"` · `"Inactive guests"`

### 3.8 Campaign status
`CREATED` · `SENDING` · `COMPLETED`

---

## 4. Data models (canonical JSON)

These are the exact shapes the app expects. **All writes return the full refreshed entity** (or an empty `{}` — the app re-fetches everything after every write anyway; see §12).

### 4.1 Room
```json
{
  "id": 3,
  "number": "103",
  "type": "Standard",
  "floor": "1st Floor",
  "price": 1800,
  "status": "AVAILABLE",
  "note": "2 single beds"
}
```
`type` is free text but the app's picker uses: `Standard`, `Deluxe`, `Suite`, `Family`, `Dorm`.

### 4.2 Guest
```json
{
  "id": 9,
  "name": "Dev Patel",
  "phone": "+91 98200 31313",
  "email": "dev.patel@gmail.com",
  "notes": "Repeat guest from Mumbai. Likes the corner suite.",
  "visits": 2,
  "totalSpending": 14200,
  "firstVisit": 1712000000000,
  "lastVisit": 1719000000000,
  "marketingConsent": true
}
```
`visits`, `totalSpending`, `firstVisit`, `lastVisit` are **derived server-side from bookings** — never accept them on write. `isRepeat` is computed client-side as `visits > 1`.

### 4.3 Booking
```json
{
  "id": 15,
  "guestId": 9,
  "guestName": "Dev Patel",
  "phone": "+91 98200 31313",
  "checkIn": 1719000000000,
  "checkOut": 1719360000000,
  "roomNumber": "201",
  "guests": 2,
  "status": "CONFIRMED",
  "paid": false,
  "amount": 5200,
  "source": "StayRide link"
}
```
`nights` is computed client-side from `checkOut - checkIn`.

### 4.4 Driver
```json
{
  "id": 2,
  "name": "Bikash Rai",
  "phone": "+91 99330 12124",
  "vehicle": "Toyota Innova Crysta",
  "vehicleType": "SUV",
  "seats": 7,
  "rating": 4.9,
  "area": "Gangtok / Darjeeling / Bagdogra",
  "trips": ["NJP", "Bagdogra", "Sightseeing", "Local"],
  "available": true,
  "active": true,
  "ownerDriver": true
}
```

### 4.5 CabTrip
```json
{
  "id": 27,
  "guestName": "Sunita Rai",
  "phone": "+91 94198 55210",
  "pickup": "NJP Station",
  "destination": "Mountain View Homestay",
  "date": 1719100000000,
  "time": "12:45 PM",
  "passengers": 2,
  "vehicleType": "Sedan",
  "estFare": 2200,
  "status": "CONFIRMED",
  "driverId": 56,
  "driverName": "Sagar Chettri",
  "ownerDriver": false,
  "fare": 0,
  "commission": 0,
  "commissionPaid": false
}
```
`time` is a display string `"h:mm AM/PM"`. `fare`/`commission` are `0` until `COMPLETED`.

### 4.6 Campaign
```json
{
  "id": 2,
  "title": "Winter special: snow trip to Lachung",
  "message": "Hello {name}! ...",
  "audience": "Repeat guests",
  "status": "CREATED",
  "recipients": 5,
  "sent": 0,
  "failed": 0,
  "createdOn": 1719000000000
}
```

### 4.7 Homestay
```json
{
  "name": "Mountain View Homestay",
  "description": "A cosy family-run homestay ...",
  "location": "MG Marg, Gangtok, Sikkim 737101",
  "phone": "+91 98320 11223",
  "email": "stay@mountainview.in",
  "ownerName": "Tshering Bhutia",
  "link": "https://stayride.in/h/mountain-view-gangtok",
  "mapQuery": "MG Marg, Gangtok, Sikkim",
  "amenities": ["Free Wi-Fi", "24x7 Hot Water", "Private Parking"],
  "photos": ["Front elevation", "Deluxe room", "Valley view balcony"]
}
```
`link` is the public booking URL rendered as a QR code in-app. `photos` currently holds **labels**, not URLs (the app has no image loader yet). Keep it an array of strings.

---

## 5. Rooms — CORE

### `GET /rooms`
Returns all rooms for the homestay, unsorted (app sorts/filters locally).
Response `200`: `[ Room, Room, ... ]`

### `POST /rooms`
Request: `{ number, type, floor, price, status, note }` (no `id`).
Response `201`: `Room`.
Errors: `409 ROOM_NUMBER_TAKEN` (number unique per homestay).

### `PUT /rooms/{id}`
Full update. Request = same body as create.
Response `200`: `Room`. Errors: `404`.

### `PATCH /rooms/{id}/status`
Request: `{ "status": "MAINTENANCE" }`
Response `200`: `Room`.
Notes: manual status toggle from the Rooms screen. Do **not** let this clobber booking-driven transitions unexpectedly — but this is the owner's explicit override, so honor it.

### `DELETE /rooms/{id}`
Response `200` `{}` or `204`.
Notes: hard delete is fine; existing bookings keep `roomNumber` as history.

---

## 6. Guests — CORE

### `GET /guests`
Returns all guests for the homestay. The app does search (`?q=`) and filter (`ALL`/`NEW`/`REPEAT`) on-device, so plain list is enough.
Response `200`: `[ Guest, ... ]`

### `POST /guests`
Request:
```json
{ "name": "Neha Gupta", "phone": "+91 98765 43210", "email": "neha@x.com", "notes": "", "marketingConsent": true }
```
Response `201`: `Guest`. Errors: `400 VALIDATION_ERROR`.

### `PUT /guests/{id}`
Full update, same body as create.
Response `200`: `Guest`.

### `DELETE /guests/{id}` — (app has no UI yet, include for completeness)
Response `200`/`204`.

### `GET /guests/{id}/bookings` — RECOMMENDED
Booking history for a guest (ordered newest first). The app currently filters bookings locally.
Response `200`: `[ Booking, ... ]`.

---

## 7. Bookings — CORE

### `GET /bookings`
Optional query params: `?status=CONFIRMED&q=neha&from=<ms>&to=<ms>`.
Response `200`: `[ Booking, ... ]`

### `GET /bookings/{id}` — RECOMMENDED
Response `200`: `Booking`. (App currently finds it in the already-fetched list.)

### `POST /bookings`
Create a booking. Guest may be existing (`guestId`) **or** new (inline fields).

Request:
```json
{
  "guestId": 9,
  "guestName": "Dev Patel",
  "guestPhone": "+91 98200 31313",
  "guestEmail": "dev.patel@gmail.com",
  "roomNumber": "201",
  "checkIn": 1719000000000,
  "checkOut": 1719360000000,
  "guests": 2,
  "amount": 5200,
  "source": "StayRide link"
}
```
- `guestId` is **omitted/null** for a brand-new guest; then `guestName` + `guestPhone` are required and the server creates the guest.

Server rules:
1. Validate room exists.
2. Reject if the room has an **overlapping** non-`CANCELLED` booking (`checkIn < other.checkOut && checkOut > other.checkIn`).
3. Reject unless `checkOut > checkIn`.
4. Create guest if new (derive nothing else).
5. Create booking `status = CONFIRMED`, `paid = false`, denormalize `guestName` + `phone`.
6. Set room → `RESERVED`.

Response `201`: `Booking`.
Errors: `409 ROOM_OVERLAP`, `400 INVALID_DATES`, `404 ROOM_NOT_FOUND`.

### `POST /bookings/{id}/check-in`
No body.
Effects: booking → `CHECKED_IN`; its room → `OCCUPIED`.
Response `200`: `Booking`. Errors: `409 INVALID_STATE`.

### `POST /bookings/{id}/check-out`
No body.
Effects: booking → `CHECKED_OUT`; room → `AVAILABLE`; recompute the guest's `visits`, `totalSpending`, `firstVisit`, `lastVisit`.
Response `200`: `Booking`.

### `POST /bookings/{id}/cancel`
No body.
Effects: booking → `CANCELLED`; room → `AVAILABLE`. Cancelled bookings are **excluded from all revenue/occupancy metrics**.
Response `200`: `Booking`.

### `PATCH /bookings/{id}/payment`
Request: `{ "paid": true }`.
Response `200`: `Booking`.

### `POST /bookings/{id}/room`
Change the assigned room. Request: `{ "roomNumber": "103" }`.
Effects: release the old room (→ `AVAILABLE` if no other active booking), reserve the new room; update `booking.roomNumber`.
Response `200`: `Booking`.
Errors: `409 ROOM_OVERLAP`.

### `DELETE /bookings/{id}`
Owner delete; also release the room. Response `200`/`204`.

---

## 8. Drivers — CORE

### `GET /drivers?scope=mine|network`
- `scope=mine` → the owner's own drivers (`ownerDriver: true`, all `active` states).
- `scope=network` → the shared StayRide pool (`ownerDriver: false`), across the platform, not owned by this homestay.

Response `200`: `[ Driver, ... ]`.

### `POST /drivers`
Request:
```json
{
  "name": "Bikash Rai", "phone": "+91 99330 12124",
  "vehicle": "Toyota Innova Crysta", "vehicleType": "SUV", "seats": 7,
  "rating": 4.5, "area": "Gangtok", "trips": ["NJP", "Bagdogra"]
}
```
`rating` from the app is a default `4.5` for new drivers. Server sets `ownerDriver = true`, `active = true`, `available = true` (unless provided).
Response `201`: `Driver`.

### `PUT /drivers/{id}`
Full update, same body as create (owner only).
Response `200`: `Driver`.

### `PATCH /drivers/{id}/availability`
Request: `{ "available": false }` → response `200`: `Driver`.

### `PATCH /drivers/{id}/active`
Request: `{ "active": false }` → response `200`: `Driver`.
Notes: the app's Active switch. `active=false` = soft-disabled (still listed, shown greyed as INACTIVE).

### `DELETE /drivers/{id}`
Soft delete (`active = false`). Response `200`/`204`.

### `GET /drivers/{id}` — RECOMMENDED
Driver profile for the pop-up card. Response `200`: `Driver`.

### Driver matching — RECOMMENDED
### `GET /trips/match?vehicleType=SUV&tripType=NJP`
Returns available drivers whose `vehicleType` matches and whose `trips[]` covers the route category, **owner's drivers first, then network**. The app currently computes this from the two driver lists.
Response `200`: `[ Driver, ... ]`.

---

## 9. Cab trips — CORE

> Trip creation is triggered by the guest (via `GuestPageActivity` / the booking link) **or** simulated by the owner (`CabsFragment` "simulate" button). Same endpoint.

### `POST /trips`
Request:
```json
{
  "guestName": "Rahul Sharma",
  "phone": "+91 98301 44521",
  "pickup": "Bagdogra Airport",
  "destination": "Mountain View Homestay",
  "date": 1719400000000,
  "time": "10:30 AM",
  "passengers": 4,
  "vehicleType": "SUV"
}
```
Server: compute `estFare` (see estimate below), set `status = NEW_REQUEST`, `driverId = null`.
Response `201`: `CabTrip`.

### `GET /trips?tab=requests|trips|all`
- `tab=requests` → `NEW_REQUEST`, `FINDING_DRIVER`, `DRIVER_ASSIGNED`
- `tab=trips` → `CONFIRMED`, `TRIP_STARTED`, `COMPLETED`
- `tab=all` → everything (this is what the DataStore uses)

Response `200`: `[ CabTrip, ... ]`.

### `GET /trips/{id}` — RECOMMENDED
Response `200`: `CabTrip`.

### `POST /trips/{id}/assign`
Request: `{ "driverId": 2 }`.
Effect: status → `DRIVER_ASSIGNED`; set `driverId`, denormalized `driverName`, `ownerDriver`.
Response `200`: `CabTrip`.

### `POST /trips/{id}/accept`
No body. Represents the driver accepting. Effect: status → `CONFIRMED`.
Response `200`: `CabTrip`.

### `POST /trips/{id}/reassign`
No body. Effect: status → `FINDING_DRIVER`, clear driver fields.
Response `200`: `CabTrip`.

### `POST /trips/{id}/start`
No body. Effect: status → `TRIP_STARTED`.
Response `200`: `CabTrip`.

### `POST /trips/{id}/complete`
No body.
Effect: status → `COMPLETED`; set `fare` (final) and **`commission = round(fare * 0.12)`** server-side.
Response `200`: `CabTrip`.

### `POST /trips/{id}/cancel`
No body. Effect: status → `CANCELLED` (from any state).
Response `200`: `CabTrip`.

### `GET /trips/estimate?pickup=&destination=&vehicleType=` — RECOMMENDED
Replaces the app's client-side estimate (Sedan 2200 / SUV 3000 / 7-seater 3200 / 9-seater 4200).
Response `200`: `{ "estFare": 3000 }`.

### `POST /trips/{id}/commission/pay`
Request: `{ "paid": true }`. Sets `commissionPaid`.
Response `200`: `CabTrip`.

---

## 10. Commission — RECOMMENDED (app computes locally today)

| Endpoint | Response |
|---|---|
| `GET /commission/summary` | `{ "earned": 51200, "pending": 8600, "paid": 42600 }` |
| `GET /commission/ledger` | `[ { CabTrip (completed) with fare + commission), ... } ]` |

Rules: `earned` = Σ `commission` over `COMPLETED` trips. `pending` = same where `commissionPaid=false`. `paid` = where `commissionPaid=true`. Rate is fixed at **12%**.

---

## 11. Campaigns — CORE

### `GET /campaigns`
Response `200`: `[ Campaign, ... ]`.

### `POST /campaigns`
Request: `{ "title": "...", "message": "...", "audience": "Repeat guests" }`.
Server computes `recipients` by audience:
- `All guests` → every guest
- `Repeat guests` → `visits > 1`
- `Recent guests` → `lastVisit >= today - 60 days`
- `Inactive guests` → `lastVisit < today - 90 days`

Status = `CREATED`, `sent = 0`, `failed = 0`, `createdOn = now`.
Response `201`: `Campaign`.

### `POST /campaigns/{id}/send`
No body. Effect: status `CREATED → SENDING`, async worker dispatches (SMS/WhatsApp provider or stub), then `COMPLETED` with filled `sent` / `failed`. Only guests with `marketingConsent = true` actually receive it.
Response `200`: `Campaign` (may still be `SENDING`; the app re-fetches on resume).

### `DELETE /campaigns/{id}`
Response `200`/`204`.

---

## 12. Homestay profile — CORE

### `GET /homestay`
Response `200`: `Homestay` (§4.7).

### `PUT /homestay`
Request: full homestay body (name, description, location, phone, email, ownerName, link, mapQuery, amenities[], photos[]).
Response `200`: `Homestay`.

### Photo upload — RECOMMENDED
`POST /homestay/photos` (multipart) → returns `{ "url": "..." }`. The app currently only stores photo **labels**, so this is future work.

---

## 13. Dev / seeding

### `POST /dev/seed` — CORE for demo parity
Resets the owner's homestay to a rich demo dataset (8 rooms, ~14 guests, ~120 bookings, 58 drivers, ~80 trips, 2 campaigns, profile). The app calls this (a) right after signup, and (b) from **More → Reset demo data**. Guard with an env flag; return `404`/`403` in production.
Response `200`: `{}`.
Reference for exact demo values: `app/src/main/java/com/example/stayride/data/Seeder.java`.

---

## 14. Dashboard & Analytics — RECOMMENDED

> The app currently computes every number below on-device from the full lists it fetches. These endpoints exist so you can move the math server-side later; the app does **not** call them yet.

### `GET /dashboard/summary`
```json
{
  "todayCheckIns": 3,
  "currentGuests": 5,
  "availableRooms": 4,
  "occupancyPercent": 62,
  "monthRevenue": 248000,
  "monthBookings": 27,
  "totalBookings": 120,
  "avgBookingValue": 9185
}
```
Formulas:
- `todayCheckIns` = bookings with `checkIn == today && status != CANCELLED`
- `currentGuests` = Σ `guests` over `CHECKED_IN` bookings
- `occupancyPercent` = round((OCCUPIED + RESERVED) / totalRooms × 100)
- `monthRevenue` = Σ `amount` over non-cancelled bookings with `checkIn` in the current calendar month
- `avgBookingValue` = monthRevenue / monthBookingCount

### `GET /dashboard/performance?period=month`
`{ "cabTrips": 42, "revenue": 118000, "commission": 14160 }`

### `GET /dashboard/series?metric=revenue|bookings|cabs&period=7d|30d|6m|1y|all`
`{ "labels": ["Jan","Feb","Mar"], "values": [186000,214000,172000] }`
Bucketing: `7d` → daily (7 pts) · `30d` → 5×6-day buckets · `6m`/`1y` → monthly.

### `GET /analytics?period=7d|30d|6m|1y|all`
Series + KPIs (revenue, occupancy, cab revenue, commission, repeat-guest %, ADR). Feeds `AnalyticsActivity`'s bar + pie charts.

---

## 15. Screen → endpoint map (what the app currently calls)

| Screen | Calls today |
|---|---|
| Splash | none (checks local token) |
| Login / Signup | `POST /auth/login`, `POST /auth/signup`, then `POST /dev/seed` on signup |
| Main / every screen (`onResume`) | `GET /homestay`, `/rooms`, `/guests`, `/bookings`, `/drivers?scope=mine`, `/drivers?scope=network`, `/trips?tab=all`, `/campaigns` |
| Home dashboard | renders from the above (charts computed locally) |
| Rooms | `GET /rooms`; `POST/PUT/DELETE /rooms`, `PATCH /rooms/{id}/status` |
| Bookings | `POST /bookings`, `POST /bookings/{id}/check-in\|check-out\|cancel\|room`, `PATCH /bookings/{id}/payment`, `DELETE /bookings/{id}` |
| Guests | `GET /guests`; `POST/PUT /guests` |
| New Booking | `GET /guests`, `GET /rooms` + `POST /bookings` |
| Find Driver / My Drivers | `GET /drivers?scope=network\|mine`; `POST/PUT /drivers`, `PATCH .../availability`, `PATCH .../active`, `DELETE /drivers/{id}` |
| Cab request detail | `POST /trips/{id}/assign\|accept\|reassign\|start\|complete\|cancel`, `POST /trips/{id}/commission/pay` |
| Cabs (simulate) / Guest page | `POST /trips` |
| Commission | renders from `GET /trips` (locally) |
| Campaigns | `GET /campaigns`; `POST /campaigns`, `POST /campaigns/{id}/send`, `DELETE /campaigns/{id}` |
| Homestay profile | `GET /homestay`, `PUT /homestay` |
| More → Reset demo | `POST /dev/seed` |

---

## 16. Critical behavior rules (server-authoritative)

1. **Revenue excludes `CANCELLED` bookings** everywhere.
2. **Check-out updates guest aggregates** (`visits`,`totalSpending`,`firstVisit`,`lastVisit`).
3. **Commission = `round(fare * 0.12)`**, computed at trip completion, never accepted from the client.
4. **Room status is driven by booking events**: create→RESERVED, check-in→OCCUPIED, check-out/cancel→AVAILABLE.
5. **`id`s are ints**, **money is int INR**, **times are epoch ms**, **enums are exact uppercase strings**.
6. **List endpoints return bare arrays.** The app's JSON parser expects arrays directly.
7. After every successful write the app calls the full `GET` set again — so writes don't strictly need to return the object, but returning it is preferred.

---

## 17. Non-goals for v1

- Driver-side app / driver auth, real SMS/WhatsApp sending, payments, the public booking web page (`stayride.in/h/<slug>`) are **out of scope** — stub behind the noted endpoints.
- **No geolocation or live trip tracking** — trips are static records.
- The app has **no cache layer**; every screen re-fetches on `onResume`. Keep responses fast and self-contained.
