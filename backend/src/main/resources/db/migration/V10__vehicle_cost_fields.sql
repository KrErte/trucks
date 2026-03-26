-- Vehicle additional cost fields
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS maintenance_cost_per_km DECIMAL(10,4) DEFAULT 0;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS tire_cost_per_km DECIMAL(10,4) DEFAULT 0;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS depreciation_per_km DECIMAL(10,4) DEFAULT 0;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS insurance_per_day DECIMAL(10,2) DEFAULT 0;

-- Calculation additional cost fields
ALTER TABLE calculations ADD COLUMN IF NOT EXISTS maintenance_cost DECIMAL(10,2) DEFAULT 0;
ALTER TABLE calculations ADD COLUMN IF NOT EXISTS tire_cost DECIMAL(10,2) DEFAULT 0;
ALTER TABLE calculations ADD COLUMN IF NOT EXISTS depreciation_cost DECIMAL(10,2) DEFAULT 0;
ALTER TABLE calculations ADD COLUMN IF NOT EXISTS insurance_cost DECIMAL(10,2) DEFAULT 0;
