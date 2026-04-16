-- V1: Maak ENUM types en de visit tabel aan
-- MNSW — Maritime National Single Window
-- EMSWe Richtlijn 2019/1239/EU

-- Enum: Formality type codes (inkomende meldingen)
CREATE TYPE formality_type AS ENUM ('NOA', 'NOS', 'NOD', 'VID', 'SID');

-- Enum: Status van een formality
CREATE TYPE formality_status AS ENUM ('SUBMITTED', 'ACCEPTED', 'REJECTED', 'UNDER_REVIEW', 'SUPERSEDED');

-- Enum: Indieningskanaal
CREATE TYPE submission_channel AS ENUM ('WEB', 'RIM');

-- Enum: Status van een FRM-response (Formality Response Message)
CREATE TYPE frm_status AS ENUM ('ACCEPTED', 'REJECTED', 'UNDER_REVIEW');

-- Enum: Gebruikersrollen
CREATE TYPE user_role AS ENUM ('SCHEEPSAGENT', 'LADINGAGENT', 'HAVENAUTORITEIT', 'ADMIN');

-- Tabel: visit (havenbezoek) — centrale correlatie-aggregaat
CREATE TABLE visit (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    imo_number   VARCHAR(10) NOT NULL,
    vessel_name  VARCHAR(255) NOT NULL,
    vessel_flag  VARCHAR(3),                          -- ISO 3166-1 alpha-3
    port_locode  VARCHAR(10) NOT NULL,                -- UN/LOCODE, bijv. NLRTM
    eta          TIMESTAMPTZ,                         -- Verwachte aankomsttijd
    etd          TIMESTAMPTZ,                         -- Verwachte vertrektijd
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
