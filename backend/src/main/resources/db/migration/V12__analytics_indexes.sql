-- Analytics indexes
CREATE INDEX IF NOT EXISTS idx_calculations_origin_dest ON calculations(origin, destination);
CREATE INDEX IF NOT EXISTS idx_calculations_created_month ON calculations(company_id, created_at);
