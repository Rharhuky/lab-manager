-- CampusLab Initial Schema Migration
-- Creates tables for users, laboratories, and reservations

-- Table: users
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ALUNO', 'PROFESSOR')),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Table: laboratories
CREATE TABLE laboratories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    block       VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Table: reservations
CREATE TABLE reservations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    laboratory_id  UUID NOT NULL REFERENCES laboratories(id),
    user_id        UUID NOT NULL REFERENCES users(id),
    start_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_reservation_order CHECK (end_at > start_at)
);

-- Index for conflict checking queries on reservations
CREATE INDEX idx_reservations_lab_time
    ON reservations(laboratory_id, start_at, end_at);

-- Comments for documentation
COMMENT ON TABLE users IS 'Stores user accounts with authentication credentials';
COMMENT ON TABLE laboratories IS 'Stores laboratory information for the campus';
COMMENT ON TABLE reservations IS 'Stores laboratory reservation records';
COMMENT ON CONSTRAINT chk_reservation_order ON reservations IS 'Ensures end_at is always after start_at';
COMMENT ON INDEX idx_reservations_lab_time IS 'Optimizes conflict detection queries for overlapping reservations';
