# FuelFleet – Tellimuse Kasumlikkuse Kalkulaator

## Projekti ülevaade

SaaS platvorm veoettevõtetele, mis vastab küsimusele **"Kas see tellimus tasub ennast ära?"** enne tellimuse vastuvõtmist. Dispetšer sisestab marsruudi ja tellimuse hinna, süsteem arvutab kulud ja näitab kasumit reaalajas.

## Tehniline stack

- **Backend:** Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA
- **Frontend:** Angular 18+, Angular Material
- **Andmebaas:** PostgreSQL 16
- **Auth:** Spring Security + JWT (refresh token rotation)
- **Maksed:** Stripe (Checkout + Customer Portal)
- **Kaardid/kaugus:** Google Maps Distance Matrix API
- **Deploy:** Docker Compose, Caddy reverse proxy, Contabo VPS
- **CI:** GitHub Actions

## Projekti struktuur

```
fuelfleet/
├── backend/                    # Spring Boot
│   ├── src/main/java/eu/fuelfleet/
│   │   ├── auth/               # JWT, refresh tokens, user registration
│   │   ├── calculation/        # Kulu kalkulatsiooni loogika
│   │   ├── vehicle/            # Sõiduki profiilid
│   │   ├── fuel/               # Kütusehindade haldus
│   │   ├── tollcost/           # Teemaksud riigiti
│   │   ├── order/              # Tellimuste ajalugu
│   │   ├── subscription/       # Stripe integratsioon, plaanid
│   │   └── company/            # Multi-tenant, ettevõtte haldus
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/       # Flyway migratsioonid
├── frontend/                   # Angular
│   └── src/app/
│       ├── auth/
│       ├── calculator/         # Põhivaade – kalkulaator
│       ├── vehicles/           # Sõidukite haldus
│       ├── history/            # Tellimuste ajalugu
│       ├── settings/           # Ettevõtte seaded, kütusehind
│       └── subscription/       # Plaanid, Stripe portal
├── docker-compose.yml
├── docker-compose.prod.yml
└── Caddyfile
```

## Andmebaasi skeema (põhi)

```sql
-- Multi-tenant
companies (id, name, vat_number, country, created_at)

-- Auth
users (id, company_id, email, password_hash, role, created_at)
refresh_tokens (id, user_id, token_hash, expires_at, revoked)

-- Sõidukid
vehicles (id, company_id, name, fuel_type, consumption_loaded, consumption_empty, tank_capacity, euro_class, active)

-- Kütusehindade haldus (admin uuendab käsitsi või scheduled job)
fuel_prices (id, country_code, fuel_type, price_per_liter, valid_from, source)

-- Teemaksud (fikseeritud tabel riigiti, uuendatav admin paneelist)
toll_rates (id, country_code, vehicle_class, cost_per_km, currency, valid_from)

-- Kalkulatsioonid / tellimuste ajalugu
calculations (
  id, company_id, vehicle_id, user_id,
  origin, destination, distance_km,
  cargo_weight_t, order_price, currency,
  fuel_cost, toll_cost, driver_daily_cost, other_costs, total_cost,
  profit, profit_margin_pct, revenue_per_km,
  created_at
)

-- Subscriptionid
subscriptions (id, company_id, stripe_customer_id, stripe_subscription_id, plan, status, current_period_end)
```

## API endpointid

```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout

GET  /api/vehicles
POST /api/vehicles
PUT  /api/vehicles/{id}
DELETE /api/vehicles/{id}

POST /api/calculate            # Põhi endpoint – tagastab kalkulatsiooni tulemuse
GET  /api/calculations         # Ajalugu (pagineeritud)
GET  /api/calculations/{id}

GET  /api/fuel-prices          # Aktiivsed kütusehindade
GET  /api/fuel-prices/{country}

POST /api/subscriptions/checkout   # Stripe Checkout session
POST /api/subscriptions/portal     # Stripe Customer Portal
POST /api/webhooks/stripe          # Stripe webhook

GET  /api/admin/fuel-prices        # Admin: kütusehindade haldus
POST /api/admin/fuel-prices
GET  /api/admin/toll-rates         # Admin: teemaksud
POST /api/admin/toll-rates
```

## Kalkulatsiooni loogika

```java
// POST /api/calculate sisend
{
  "vehicleId": "uuid",
  "originAddress": "Tallinn, Estonia",
  "destinationAddress": "Hamburg, Germany",
  "cargoWeightTons": 18.0,
  "orderPrice": 2400.00,
  "currency": "EUR",
  "isLoaded": true,
  "driverDailyRate": 60.00,   // optional, default ettevõtte seadest
  "otherCosts": 90.00         // optional (fähri, muu)
}

// Kalkulatsiooni sammud
1. Google Maps Distance Matrix API → distanceKm, estimatedHours
2. fuelCost = (distanceKm / 100) * vehicle.consumptionLoaded * fuelPrice.pricePerLiter
3. tollCost = SUM(toll_rates riigiti marsruudil * distanceKm riigis)
4. driverCost = CEIL(estimatedHours / 24) * driverDailyRate
5. totalCost = fuelCost + tollCost + driverCost + otherCosts
6. profit = orderPrice - totalCost
7. profitMarginPct = (profit / orderPrice) * 100
8. revenuePerKm = orderPrice / distanceKm

// Vastus
{
  "distanceKm": 1840,
  "estimatedHours": 22,
  "fuelCost": 680.00,
  "tollCost": 210.00,
  "driverCost": 180.00,
  "otherCosts": 90.00,
  "totalCost": 1160.00,
  "profit": 1240.00,
  "profitMarginPct": 51.67,
  "revenuePerKm": 1.30,
  "isProfit": true,
  "fuelBreakdown": {
    "liters": 101.2,
    "pricePerLiter": 1.55,
    "countries": [
      { "country": "EE", "km": 50, "cost": 42.50 },
      { "country": "LV", "km": 310, "cost": 120.00 },
      ...
    ]
  }
}
```

## Subscription plaanid

| Plaan | Hind | Sõidukid | Kalkulatsioonid/kuu |
|-------|------|----------|---------------------|
| Starter | €29/kuu | kuni 5 | piiramata |
| Growth | €79/kuu | kuni 25 | piiramata |
| Enterprise | €199/kuu | piiramata | piiramata + API ligipääs |

Subscription enforcement toimub Spring Security tasandil – `@PreAuthorize` + custom `SubscriptionGuard`.

## Frontend vaated

### 1. Kalkulaator (põhivaade `/calculator`)
- Suur, selge form: marsruut, sõiduk, kauba kaal, tellimuse hind
- Google Places Autocomplete origin/destination jaoks
- "Arvuta" → tulemuste kaart kohe all
- Roheline ✅ kui kasumlik, punane ❌ kui kahjumlik
- Kulude breakdown accordion (küte, toll, juht, muu)
- "Salvesta tellimus" nupp

### 2. Tellimuste ajalugu (`/history`)
- Tabel kõigist kalkulatsioonidest
- Filter marsruudi, sõiduki, kuupäeva järgi
- Kasumlikkuse statistika (avg margin, parimad marsruudid)
- CSV eksport

### 3. Sõidukite haldus (`/vehicles`)
- CRUD sõiduki profiilid
- Eelseadistatud mudelid (Volvo FH, Scania R, MAN TGX vms)

### 4. Seaded (`/settings`)
- Ettevõtte andmed
- Vaikimisi juhi päevaraha
- Kütusehinna override (kui ettevõttel oma lepinguhind tanklaga)

## MVP arenduse järjekord

### Faas 1 – Kalkulaator töötab (2 nädalat)
- [ ] Spring Boot projekt, PostgreSQL, Flyway
- [ ] Auth (register, login, JWT)
- [ ] Sõiduki CRUD
- [ ] Google Maps Distance Matrix integratsioon
- [ ] Kalkulatsiooni endpoint (kütus + juht + muu, toll käsitsi)
- [ ] Angular kalkulaatori UI
- [ ] Docker Compose deploy

### Faas 2 – SaaS infrastruktuur (1 nädal)
- [ ] Stripe integratsioon (Checkout + webhook + portal)
- [ ] Subscription enforcement
- [ ] Tellimuste ajalugu ja statistika
- [ ] Admin paneel kütusehindade halduseks

### Faas 3 – Täpsemad kulud (1 nädal)
- [ ] Teemaksud riigiti (Maut DE, Vignette AT/CH/HU, Eurotoll FR)
- [ ] Marsruudi riigipõhine kütusehind (tankida odavamalt)
- [ ] Email notifikatsioonid (kütusehind muutus)

### Faas 4 – Kasv
- [ ] API ligipääs Enterprise plaanile (webhook + token)
- [ ] Benchmarking – €/km tööstuse keskmisega võrreldes
- [ ] Kliendi kasumlikkuse raport (milline klient = parim marginaal)
- [ ] Mobile-friendly (PWA)

## Keskkonna muutujad

```env
# Backend
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fuelfleet
SPRING_DATASOURCE_USERNAME=fuelfleet
SPRING_DATASOURCE_PASSWORD=secret

JWT_SECRET=your-secret-key
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000

GOOGLE_MAPS_API_KEY=your-key

STRIPE_SECRET_KEY=sk_live_xxx
STRIPE_WEBHOOK_SECRET=whsec_xxx
STRIPE_PRICE_STARTER=price_xxx
STRIPE_PRICE_GROWTH=price_xxx
STRIPE_PRICE_ENTERPRISE=price_xxx

# Frontend
GOOGLE_MAPS_API_KEY=your-key (Angular environment.ts)
API_BASE_URL=https://api.fuelfleet.eu
```

## Sihtturg ja positsioneerung

- **Peamine turg:** Balti riikide + Soome veoettevõtted (1–50 sõidukit)
- **Müügikanal:** LinkedIn outreach, veoettevõtete foorumid, Google Ads ("kütusekulu kalkulaator")
- **Võtmeargument:** "Dispetšer teab kasumi enne kui ütleb jah"
- **Konkurents:** Excel tabelid, peas arvutamine – pole otsest SaaS konkurrenti Baltikumis
