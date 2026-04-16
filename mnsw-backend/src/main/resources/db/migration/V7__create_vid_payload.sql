-- V7: VID Payload tabel
-- Vessel Identification Documents — conform EMSWe MIG v2.0.1 datacategorie "Ship/Vessel Identification"

CREATE TABLE vid_payload (
    formality_id             UUID         PRIMARY KEY REFERENCES formality(id),
    certificate_nationality  VARCHAR(255),            -- Nationaliteitscertificaat
    gross_tonnage            DECIMAL(10,2),           -- Brutotonnemaat
    net_tonnage              DECIMAL(10,2),           -- Nettotonnemaat
    deadweight               DECIMAL(10,2),           -- Draagvermogen (DWT)
    length_overall           DECIMAL(8,2),            -- Lengte over alles (LOA) in meters
    ship_type                VARCHAR(100),            -- Scheepstype (bijv. "BULK CARRIER")
    call_sign                VARCHAR(20),             -- Roepletters (3-7 alfanumeriek indien opgegeven)
    mmsi                     VARCHAR(15)              -- MMSI-nummer (exact 9 cijfers indien opgegeven)
);
