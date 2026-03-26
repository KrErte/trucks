-- Route country segments for fuel optimization fallback
CREATE TABLE IF NOT EXISTS route_country_segments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    origin_country VARCHAR(5) NOT NULL,
    destination_country VARCHAR(5) NOT NULL,
    waypoint_countries TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed common corridor segments
INSERT INTO route_country_segments (origin_country, destination_country, waypoint_countries) VALUES
('EE', 'DE', 'EE,LV,LT,PL,DE'),
('EE', 'NL', 'EE,LV,LT,PL,DE,NL'),
('EE', 'FR', 'EE,LV,LT,PL,DE,FR'),
('EE', 'IT', 'EE,LV,LT,PL,CZ,AT,IT'),
('EE', 'SE', 'EE,SE'),
('EE', 'FI', 'EE,FI'),
('LV', 'DE', 'LV,LT,PL,DE'),
('LT', 'DE', 'LT,PL,DE'),
('PL', 'DE', 'PL,DE'),
('PL', 'FR', 'PL,DE,FR'),
('DE', 'FR', 'DE,FR'),
('DE', 'IT', 'DE,AT,IT'),
('DE', 'ES', 'DE,FR,ES'),
('DE', 'NL', 'DE,NL'),
('DE', 'BE', 'DE,BE')
ON CONFLICT DO NOTHING;
