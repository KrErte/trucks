-- Demo company
INSERT INTO companies (id, name, vat_number, country, default_driver_daily_rate) VALUES
('a0000000-0000-0000-0000-000000000001', 'Demo Transport OÜ', 'EE100000001', 'EE', 65.00)
ON CONFLICT DO NOTHING;

-- Demo users (password: demo1234)
-- BCrypt hash of "demo1234"
INSERT INTO users (id, company_id, email, password_hash, first_name, last_name, role) VALUES
('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',
 'admin@demo.ee', '$2b$10$cMDQThn3dtnayTYogmen0.Gkg5iUKwgFNSBtH3trOHwfIU.FFP24q',
 'Admin', 'Kasutaja', 'ADMIN'),
('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001',
 'juht@demo.ee', '$2b$10$cMDQThn3dtnayTYogmen0.Gkg5iUKwgFNSBtH3trOHwfIU.FFP24q',
 'Jüri', 'Mets', 'USER')
ON CONFLICT DO NOTHING;

-- Demo vehicles
INSERT INTO vehicles (id, company_id, name, fuel_type, consumption_loaded, consumption_empty, tank_capacity, euro_class, active) VALUES
('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',
 'Volvo FH 500', 'DIESEL', 32.50, 24.00, 600.00, 'EURO6', TRUE),
('c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001',
 'Scania R450', 'DIESEL', 30.00, 22.50, 500.00, 'EURO6', TRUE),
('c0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001',
 'MAN TGX 18.510', 'DIESEL', 33.00, 25.00, 600.00, 'EURO6', TRUE),
('c0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001',
 'DAF XF 480', 'DIESEL', 31.00, 23.00, 550.00, 'EURO5', TRUE),
('c0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001',
 'Mercedes Actros LNG', 'LNG', 28.00, 20.00, 450.00, 'EURO6', TRUE)
ON CONFLICT DO NOTHING;

-- Demo subscription (active starter plan)
INSERT INTO subscriptions (company_id, plan, status) VALUES
('a0000000-0000-0000-0000-000000000001', 'STARTER', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- Demo calculations (order history)
INSERT INTO calculations (company_id, vehicle_id, user_id, origin, destination, distance_km, estimated_hours, cargo_weight_t, order_price, fuel_cost, toll_cost, driver_daily_cost, other_costs, total_cost, profit, profit_margin_pct, revenue_per_km) VALUES
('a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001',
 'Tallinn, Eesti', 'Berlin, Saksamaa', 1650.00, 22.00, 24.00, 2800.00, 812.40, 195.00, 130.00, 50.00, 1187.40, 1612.60, 57.59, 1.6970),
('a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001',
 'Tallinn, Eesti', 'Helsinki, Soome', 85.00, 2.50, 20.00, 450.00, 38.25, 0.00, 65.00, 25.00, 128.25, 321.75, 71.50, 5.2941),
('a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002',
 'Tallinn, Eesti', 'Varssavi, Poola', 1050.00, 14.00, 22.00, 1900.00, 505.05, 99.75, 130.00, 40.00, 774.80, 1125.20, 59.22, 1.8095),
('a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001',
 'Riia, Läti', 'Pariis, Prantsusmaa', 2200.00, 28.00, 18.00, 3500.00, 1072.50, 385.00, 195.00, 75.00, 1727.50, 1772.50, 50.64, 1.5909),
('a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000002',
 'Tallinn, Eesti', 'Stockholm, Rootsi', 480.00, 8.00, 15.00, 950.00, 217.15, 0.00, 65.00, 30.00, 312.15, 637.85, 67.14, 1.9792);
