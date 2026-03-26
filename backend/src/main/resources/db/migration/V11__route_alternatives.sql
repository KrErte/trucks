-- Route alternatives fields (already added in V10 entity changes, ensure columns exist)
ALTER TABLE calculations ADD COLUMN IF NOT EXISTS route_index INTEGER;
ALTER TABLE calculations ADD COLUMN IF NOT EXISTS route_label VARCHAR(100);
ALTER TABLE calculations ADD COLUMN IF NOT EXISTS route_alternatives JSONB;
