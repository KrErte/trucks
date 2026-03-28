-- V16: Drivers, Customers, Maintenance, Documents, Notifications

-- 1. Drivers
CREATE TABLE drivers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    license_number VARCHAR(100),
    license_expiry DATE,
    license_categories VARCHAR(50),
    id_card_number VARCHAR(100),
    id_card_expiry DATE,
    adr_certificate_expiry DATE,
    driver_card_number VARCHAR(100),
    driver_card_expiry DATE,
    daily_rate NUMERIC(10,2) DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_drivers_company ON drivers(company_id);

-- 2. Driver-Vehicle Assignments
CREATE TABLE driver_vehicle_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id UUID NOT NULL REFERENCES drivers(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id),
    assigned_from DATE NOT NULL DEFAULT CURRENT_DATE,
    assigned_to DATE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_dva_driver ON driver_vehicle_assignments(driver_id);
CREATE INDEX idx_dva_vehicle ON driver_vehicle_assignments(vehicle_id);

-- 3. Customers
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    vat_number VARCHAR(50),
    reg_code VARCHAR(50),
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    country VARCHAR(100),
    contact_person VARCHAR(255),
    payment_term_days INTEGER DEFAULT 14,
    notes TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_customers_company ON customers(company_id);

-- 4. Maintenance Records
CREATE TABLE maintenance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id),
    company_id UUID NOT NULL REFERENCES companies(id),
    type VARCHAR(50) NOT NULL,
    description TEXT,
    cost NUMERIC(10,2),
    odometer_km INTEGER,
    performed_at DATE NOT NULL,
    next_due_date DATE,
    next_due_km INTEGER,
    performed_by VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_maintenance_vehicle ON maintenance_records(vehicle_id);
CREATE INDEX idx_maintenance_company ON maintenance_records(company_id);

-- 5. Vehicle Documents
CREATE TABLE vehicle_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID REFERENCES vehicles(id),
    company_id UUID NOT NULL REFERENCES companies(id),
    driver_id UUID REFERENCES drivers(id),
    type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    expiry_date DATE,
    issue_date DATE,
    document_number VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_vdocs_vehicle ON vehicle_documents(vehicle_id);
CREATE INDEX idx_vdocs_company ON vehicle_documents(company_id);
CREATE INDEX idx_vdocs_driver ON vehicle_documents(driver_id);
CREATE INDEX idx_vdocs_expiry ON vehicle_documents(expiry_date);

-- 6. Notifications
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    user_id UUID REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    read BOOLEAN DEFAULT FALSE,
    email_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_notifications_company ON notifications(company_id);
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(company_id, read);

-- 7. Alter existing tables
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS inspection_expiry DATE;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS insurance_expiry DATE;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS insurance_policy_number VARCHAR(100);
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS registration_number VARCHAR(50);
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS vin VARCHAR(50);
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS current_odometer_km INTEGER;

ALTER TABLE calculations ADD COLUMN IF NOT EXISTS customer_id UUID REFERENCES customers(id);

ALTER TABLE invoices ADD COLUMN IF NOT EXISTS customer_id UUID REFERENCES customers(id);
