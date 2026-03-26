ALTER TABLE calculations ADD COLUMN co2_emissions_kg NUMERIC(10,2) DEFAULT 0;
ALTER TABLE calculations ADD COLUMN driver_days INTEGER DEFAULT 0;
ALTER TABLE calculations ADD COLUMN cost_per_km NUMERIC(10,4) DEFAULT 0;
