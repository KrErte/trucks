CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Companies (multi-tenant)
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    vat_number VARCHAR(50),
    country VARCHAR(3),
    default_driver_daily_rate NUMERIC(10,2) DEFAULT 60.00,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_company ON users(company_id);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- Vehicles
CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    fuel_type VARCHAR(20) NOT NULL DEFAULT 'DIESEL',
    consumption_loaded NUMERIC(5,2) NOT NULL,
    consumption_empty NUMERIC(5,2) NOT NULL,
    tank_capacity NUMERIC(7,2),
    euro_class VARCHAR(10),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vehicles_company ON vehicles(company_id);

-- Fuel prices
CREATE TABLE fuel_prices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    country_code VARCHAR(3) NOT NULL,
    fuel_type VARCHAR(20) NOT NULL DEFAULT 'DIESEL',
    price_per_liter NUMERIC(6,3) NOT NULL,
    valid_from DATE NOT NULL DEFAULT CURRENT_DATE,
    source VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fuel_prices_country ON fuel_prices(country_code, fuel_type, valid_from DESC);

-- Toll rates
CREATE TABLE toll_rates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    country_code VARCHAR(3) NOT NULL,
    vehicle_class VARCHAR(20) NOT NULL DEFAULT 'TRUCK_40T',
    cost_per_km NUMERIC(8,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    valid_from DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_toll_rates_country ON toll_rates(country_code, vehicle_class, valid_from DESC);

-- Calculations (order history)
CREATE TABLE calculations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    vehicle_id UUID REFERENCES vehicles(id),
    user_id UUID NOT NULL REFERENCES users(id),
    origin VARCHAR(500) NOT NULL,
    destination VARCHAR(500) NOT NULL,
    distance_km NUMERIC(10,2) NOT NULL,
    estimated_hours NUMERIC(6,2),
    cargo_weight_t NUMERIC(6,2),
    order_price NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    fuel_cost NUMERIC(12,2) NOT NULL,
    toll_cost NUMERIC(12,2) NOT NULL DEFAULT 0,
    driver_daily_cost NUMERIC(12,2) NOT NULL DEFAULT 0,
    other_costs NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_cost NUMERIC(12,2) NOT NULL,
    profit NUMERIC(12,2) NOT NULL,
    profit_margin_pct NUMERIC(6,2) NOT NULL,
    revenue_per_km NUMERIC(8,4) NOT NULL,
    fuel_breakdown JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_calculations_company ON calculations(company_id, created_at DESC);
CREATE INDEX idx_calculations_user ON calculations(user_id);

-- Subscriptions
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL UNIQUE REFERENCES companies(id),
    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    plan VARCHAR(20) NOT NULL DEFAULT 'STARTER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_period_end TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_stripe ON subscriptions(stripe_customer_id);

-- Seed default fuel prices for Baltic + Nordic countries
INSERT INTO fuel_prices (country_code, fuel_type, price_per_liter, source) VALUES
('EE', 'DIESEL', 1.459, 'manual'),
('LV', 'DIESEL', 1.419, 'manual'),
('LT', 'DIESEL', 1.389, 'manual'),
('FI', 'DIESEL', 1.689, 'manual'),
('PL', 'DIESEL', 1.359, 'manual'),
('DE', 'DIESEL', 1.659, 'manual'),
('SE', 'DIESEL', 1.789, 'manual'),
('FR', 'DIESEL', 1.709, 'manual'),
('NL', 'DIESEL', 1.619, 'manual'),
('BE', 'DIESEL', 1.639, 'manual'),
('AT', 'DIESEL', 1.519, 'manual'),
('CZ', 'DIESEL', 1.399, 'manual'),
('HU', 'DIESEL', 1.549, 'manual'),
('IT', 'DIESEL', 1.719, 'manual');

-- Seed toll rates
INSERT INTO toll_rates (country_code, vehicle_class, cost_per_km, currency) VALUES
('DE', 'TRUCK_40T', 0.190, 'EUR'),
('AT', 'TRUCK_40T', 0.215, 'EUR'),
('PL', 'TRUCK_40T', 0.095, 'EUR'),
('CZ', 'TRUCK_40T', 0.110, 'EUR'),
('HU', 'TRUCK_40T', 0.125, 'EUR'),
('FR', 'TRUCK_40T', 0.160, 'EUR'),
('IT', 'TRUCK_40T', 0.175, 'EUR'),
('BE', 'TRUCK_40T', 0.145, 'EUR'),
('NL', 'TRUCK_40T', 0.000, 'EUR'),
('SE', 'TRUCK_40T', 0.000, 'EUR'),
('FI', 'TRUCK_40T', 0.000, 'EUR'),
('EE', 'TRUCK_40T', 0.000, 'EUR'),
('LV', 'TRUCK_40T', 0.000, 'EUR'),
('LT', 'TRUCK_40T', 0.000, 'EUR');
