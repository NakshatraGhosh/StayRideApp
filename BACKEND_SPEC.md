# StayRide — Backend Specification (Flow Doc)

**Audience:** backend developer building the API for the StayRide Android app.
**Repo:** https://github.com/NakshatraGhosh/StayRideApp (frontend source of truth)

---

## 1. What the app is

StayRide is a **homestay owner's management app** (single-owner dashboard). One owner runs one homestay. The app covers:

- **Dashboard** — today's check-ins, occupancy, revenue, charts
- **Rooms** — inventory + status
- **Bookings** — create, check-in / check-out / cancel, payment flag
- **Guests** — CRM (history, repeat vs new, marketing consent)
- **Drivers** — owner's own drivers + a shared "network" driver pool
- **Cab trips** — guests request cabs; owner assigns drivers; fare + 12% commission
- **Campaigns** — SMS/WhatsApp-style broadcasts to guest segments
- **Homestay profile** — public listing info + booking link + QR

**Current state:** the Android app is 100% frontend. All data lives in an in-memory mock store (`data/DataStore.java`, seeded by `data/Seeder.java`) and resets on app restart. Your job: replace that store with a real API.

---

## 2. Conventions

| Topic | Decision |
|---|---|
| Style | REST, JSON request/response |
| Base URL | `https://api.example.com/v1` |
| Auth | JWT bearer token (see §3). Owner logs in once; token in `Authorization` header |
| Timestamps | **epoch milliseconds** (int64) — the app uses `long` millis everywhere |
| Dates-only fields | epoch millis at local midnight (`checkIn`, `checkOut`) |
| Money | integer **INR**, no decimals (`amount`, `fare`, `commission`) |
| IDs | integer, auto-increment (app types are `int`) |
| Enums | uppercase strings, exactly as listed in §5 |
| Errors | `4xx/5xx` + `{"error": {"code": "ROOM_OCCUPIED", "message": "..."}}` |
| Lists | support `?page=&size=` (app currently fetches everything; pagination is forward-looking) |

---

## 3. Auth flow

> The app currently has **no login screen** (Splash → Home directly). The backend should still be multi-tenant-ready; a login screen can be added to the app later.

1. `POST /auth/signup` — owner registers (name, phone, email, password) and creates their homestay.
2. `POST /auth/login` — returns `{accessToken, refreshToken, owner}`.
3. All `/api/**` routes require `Authorization: Bearer <token>`; token carries `ownerId` + `homestayId`.
4. Every entity below is **scoped to the owner's homestay** — never leak across tenants.

Optional (nice-to-have): phone + OTP login, since the app's audience is phone-first.

---

## 4. Entity model

```
Owner ──1:1── Homestay ──┬──* Room
                         ├──* Booking ──* Guest (Guest 1:*)
                         ├──* Driver            (owner's own drivers)
                         ├──* CabTrip ──? Driver
                         └──* Campaign
Driver (network) ── shared pool, not owned by a single homestay
```

### 4.1 Room
| field | type | notes |
|---|---|---|
| id | int | |
| number | string | unique per homestay, e.g. "101" |
| type | string | "Single"/"Double"/"Deluxe"/... (free text in app) |
| floor | string | |
| price | int | INR per night |
| status | enum | see §5 |
| note | string | |

### 4.2 Guest
| field | type | notes |
|---|---|---|
| id, name, phone, email | | phone unique-ish per homestay |
| notes | string | preferences, allergies… |
| marketingConsent | bool | opt-in for campaigns |
| visits, totalSpending, firstVisit, lastVisit | int / int / ms / ms | **derived from bookings** — don't accept from client |
| isRepeat | bool | `visits > 1` (derived) |

### 4.3 Booking
| field | type | notes |
|---|---|---|
| id, guestId | int | also denormalize `guestName`, `phone` for list views |
| checkIn, checkOut | ms | checkOut > checkIn |
| roomNumber | string | |
| guests | int | headcount |
| status | enum | §5 |
| paid | bool | payment collected flag (owner toggles) |
| amount | int | total stay amount (INR) |
| source | enum | `"StayRide link"`, `"Walk-in"`, `"Phone"`, `"WhatsApp"`, `"Repeat booking"` |

### 4.4 Driver
| field | type | notes |
|---|---|---|
| id, name, phone | | |
| vehicle | string | e.g. "Toyota Innova Crysta" |
| vehicleType | enum | `"SUV"`, `"Sedan"`, `"7-seater"`, `"9-seater"` |
| seats | int | |
| rating | float | 0–5, display only |
| area | string | operating area, e.g. "Gangtok" |
| trips | string[] | route categories this driver covers: `"Local"`, `"NJP"`, `"Bagdogra"`, `"Sightseeing"` |
| available | bool | live availability toggle |
| active | bool | owner soft-deletes by setting false |
| ownerDriver | bool | true = homestay's own driver, false = network pool |

### 4.5 CabTrip
| field | type | notes |
|---|---|---|
| id | int | |
| guestName, phone | string | requestor |
| pickup, destination | string | free text |
| date, time | ms, "HH:mm" | |
| passengers | int | |
| vehicleType | enum | same as Driver.vehicleType |
| estFare | int | quoted to guest at request time |
| status | enum | §5 |
| driverId, driverName, ownerDriver | int / string / bool | set on assignment |
| fare | int | final fare, set at COMPLETED |
| commission | int | **12% of fare**, computed server-side at completion |
| commissionPaid | bool | owner marks paid later |

### 4.6 Campaign
| field | type | notes |
|---|---|---|
| id, title, message | | |
| audience | enum | `"All guests"`, `"Repeat guests"`, `"Recent guests"`, `"Inactive guests"` |
| status | enum | §5 |
| recipients, sent, failed | int | recipients computed at creation |
| createdOn | ms | |

### 4.7 Homestay (profile)
`name, description, location, phone, email, ownerName, link, mapQuery, amenities[], photos[]` — single row per owner. `link` = public booking URL (`https://stayride.in/h/<slug>`), rendered as QR in-app.

---

## 5. Status machines

**Booking:**
```
CONFIRMED ──check-in──▶ CHECKED_IN ──check-out──▶ CHECKED_OUT
    │                      │
    └──── cancel ──────────┴──▶ CANCELLED
```

**Room:**
```
AVAILABLE ──booking created──▶ RESERVED ──check-in──▶ OCCUPIED
    ▲                              │                    │
    └──── check-out / cancel ──────┴────────────────────┘
OCCUPIED/AVAILABLE ⇄ MAINTENANCE (manual toggle from Rooms screen)
```

**CabTrip:**
```
NEW_REQUEST ──search──▶ FINDING_DRIVER ──offer──▶ DRIVER_ASSIGNED
     ▲                                              │ accept
     │ (re-search)                                  ▼
     └──────────────────────────────────────── CONFIRMED
                                                    │ start
                                                    ▼
                                             TRIP_STARTED ──complete──▶ COMPLETED
(any state) ──cancel──▶ CANCELLED
```

**Campaign:** `CREATED ──send──▶ SENDING ──▶ COMPLETED` (async; `sent`/`failed` filled on completion)

---

## 6. Endpoints by module

### Auth
| Method & path | Purpose |
|---|---|
| `POST /auth/signup` | register owner + homestay |
| `POST /auth/login` / `/refresh` / `/logout` | token lifecycle |

### Dashboard (Home screen)
| Method & path | Purpose |
|---|---|
| `GET /dashboard/summary` | today's check-ins, current guest count, occupancy %, month revenue, month bookings, total bookings, avg booking value |
| `GET /dashboard/recent-bookings?limit=5` | recent + upcoming check-ins lists |
| `GET /dashboard/performance?period=month` | cab trips count, revenue, commission earned (Performance card) |
| `GET /dashboard/series?metric=revenue\|bookings\|cabs&period=7d\|30d\|6m\|1y\|all` | chart points for the line chart |

### Rooms
`GET /rooms` · `POST /rooms` · `PUT /rooms/{id}` · `DELETE /rooms/{id}` · `PATCH /rooms/{id}/status`

### Bookings
| Method & path | Purpose |
|---|---|
| `GET /bookings?status=&q=&from=&to=` | list / search |
| `GET /bookings/{id}` | detail |
| `POST /bookings` | create (see flow §7.1) |
| `POST /bookings/{id}/check-in` | booking → CHECKED_IN, room → OCCUPIED |
| `POST /bookings/{id}/check-out` | → CHECKED_OUT, room → AVAILABLE |
| `POST /bookings/{id}/cancel` | → CANCELLED, room → AVAILABLE |
| `PATCH /bookings/{id}/payment` | toggle `paid` |
| `DELETE /bookings/{id}` | owner delete (also releases room) |

### Guests
`GET /guests?q=&filter=ALL\|NEW\|REPEAT` · `POST /guests` · `PUT /guests/{id}` · `DELETE /guests/{id}` · `GET /guests/{id}/bookings`

### Drivers
| Method & path | Purpose |
|---|---|
| `GET /drivers?scope=mine\|network&vehicleType=&available=true` | lists |
| `POST /drivers` · `PUT /drivers/{id}` | owner CRUD (mine only) |
| `PATCH /drivers/{id}/availability` | toggle `available` |
| `DELETE /drivers/{id}` | soft delete (`active=false`) |
| `GET /drivers/{id}` | profile for the in-app pop-up card |

### Cab trips
| Method & path | Purpose |
|---|---|
| `POST /trips` | create request → NEW_REQUEST (see §7.2) |
| `GET /trips?tab=requests\|trips` | requests = NEW_REQUEST + FINDING_DRIVER + DRIVER_ASSIGNED; trips = CONFIRMED + TRIP_STARTED + COMPLETED |
| `GET /trips/{id}` | detail |
| `GET /trips/match?vehicleType=&tripType=` | **find-driver matching**: available drivers where `vehicleType` matches and `trips[]` covers requested route category — returns mine first, then network |
| `POST /trips/{id}/assign` body `{driverId}` | → DRIVER_ASSIGNED |
| `POST /trips/{id}/accept` / `/reject` | driver-side (future); owner app fakes this today |
| `POST /trips/{id}/start` / `/complete` | → TRIP_STARTED / COMPLETED (complete sets `fare` + `commission = 12% fare`) |
| `POST /trips/{id}/reassign` | back to FINDING_DRIVER |
| `POST /trips/{id}/cancel` | → CANCELLED |
| `GET /trips/estimate?pickup=&destination=&vehicleType=` | replace the app's random fare (`1800–4700`) with a real estimate |

### Commission
`GET /commission/summary` → `{earned, pending, paid}` · `GET /commission/ledger` → completed trips with fare/commission · `POST /trips/{id}/commission/pay` → `commissionPaid=true`

### Campaigns
`GET /campaigns` · `POST /campaigns` (compute `recipients` server-side, §7.3) · `POST /campaigns/{id}/send` (async → SENDING → COMPLETED) · `DELETE /campaigns/{id}`

### Homestay profile
`GET /homestay` · `PUT /homestay` · photo upload endpoint (multipart) for `photos[]`

### Analytics
`GET /analytics?period=7d|30d|6m|1y|all` → series + KPIs (revenue, occupancy, cab revenue, commission, repeat-guest %, ADR). Client renders existing bar/line/pie views.

---

## 7. Key flows

### 7.1 Create booking
1. App sends `POST /bookings` with `guestId` (existing) **or** inline new-guest fields, `roomNumber`, dates, headcount, `amount`, `source`.
2. Server validates: room exists, no date overlap on that room (exclude CANCELLED), checkOut > checkIn.
3. Server creates guest if new; creates booking `status=CONFIRMED`, `paid=false`.
4. Server sets room → `RESERVED`.
5. Returns booking (app refreshes list in `onResume`).

### 7.2 Cab request → completed trip
1. App sends `POST /trips` (guest quick-action or Cabs tab): pickup, destination, date/time, passengers, vehicleType, tripType. Server quotes `estFare`, status `NEW_REQUEST`.
2. App calls `GET /trips/match` → shows driver cards (profile pop-up: rating, vehicle, trips done, area).
3. Owner taps **Request** → `POST /trips/{id}/assign` → `DRIVER_ASSIGNED`, then (simulated accept) → `CONFIRMED`; app shows the "request sent" animated dialog.
4. `start` → `TRIP_STARTED`; `complete` → `COMPLETED` with `fare` (final) and `commission = round(fare * 0.12)`.
5. Trip appears in Commission ledger as pending until owner taps **Mark paid**.

### 7.3 Campaign send
1. `POST /campaigns` with title/message/audience. Server computes `recipients`:
   - **All guests** → every guest
   - **Repeat guests** → `visits > 1`
   - **Recent guests** → `lastVisit ≥ today − 60 days`
   - **Inactive guests** → `lastVisit < today − 90 days`
   - Only guests with `marketingConsent = true` should actually receive it.
2. `POST /campaigns/{id}/send` → status `SENDING`, worker dispatches (SMS/WhatsApp provider or stub), then `COMPLETED` with `sent`/`failed` counts. Client polls or the app just re-fetches on resume.

### 7.4 Check-in / check-out side effects (server-authoritative)
- **check-in**: booking → `CHECKED_IN`; its room → `OCCUPIED` (if another room was `RESERVED` for a same-day arrival, leave it).
- **check-out**: booking → `CHECKED_OUT`; room → `AVAILABLE`; update guest's `visits/totalSpending/firstVisit/lastVisit`.
- **cancel**: booking → `CANCELLED`; room → `AVAILABLE`; revenue metrics must **exclude** cancelled bookings.

### 7.5 Dashboard aggregation (reimplement from `DataStore.java`)
- **monthRevenue** = Σ `amount` of non-cancelled bookings whose `checkIn` is in the current calendar month.
- **occupancy** = round((OCCUPIED + RESERVED) / totalRooms × 100).
- **currentGuests** = Σ `guests` over `CHECKED_IN` bookings.
- **todayCheckIns** = bookings with `checkIn == today` and status ≠ CANCELLED.
- **avgBookingValue** = monthRevenue / monthBookingCount.
- **commissionEarned/Pending/Paid** = Σ `commission` over `COMPLETED` trips, split by `commissionPaid`.
- **series**: revenue/booking/cab counts bucketed by period (7d → daily, 30d → weekly?, 6m/1y → monthly). App currently hardcodes 5 monthly points; API should generate real ones.
- **guest stats** (new vs repeat) derived from `visits`.

---

## 8. Screens → endpoints map

| App screen | Endpoints |
|---|---|
| Home dashboard | `/dashboard/*` |
| Rooms tab (+ Add-Room sheet) | `/rooms` |
| New Booking form | `POST /bookings`, `GET /guests?q=`, `GET /rooms` |
| Booking detail (actions) | `/bookings/{id}/*` |
| Guests tab + search/filter | `/guests` |
| Guest profile page | `/guests/{id}`, `/guests/{id}/bookings`, `POST /trips` |
| Cabs tab (Requests/Trips) | `/trips?tab=` |
| Cab request detail | `/trips/{id}/*` |
| Find Driver (matching + animated popup) | `/trips/match`, `/trips/{id}/assign` |
| My Drivers | `/drivers?scope=mine` |
| Commission | `/commission/*` |
| Campaigns | `/campaigns` |
| Analytics | `/analytics` |
| Homestay profile / QR | `/homestay` |
| More tab (static links) | none |

---

## 9. Notes & non-goals

- **Offline:** the app loads everything in `onResume()` with no cache layer. Keep responses self-contained; a simple `GET` per screen is fine for v1.
- **Seed data:** app currently ships rich demo data (108 historical bookings, 72 completed trips, etc.). Provide a `POST /dev/seed` or fixture loader so the demo experience survives — or the frontend `Seeder` will be replaced by an onboarding flow (empty states already exist).
- **Driver app / driver auth, real SMS/WhatsApp sending, payments, public booking web page** (`stayride.in/h/...`) are **out of scope** for v1 — stub them behind the same endpoints where noted.
- **No geolocation/live tracking** — trips are static route records.
- Status strings and field names above are **exact matches** to the Android models (`data/Models.java`) — keep API keys identical so the frontend integration is a straight swap of `DataStore` for HTTP calls.
