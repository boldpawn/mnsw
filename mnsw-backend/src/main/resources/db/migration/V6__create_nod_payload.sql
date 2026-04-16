-- V6: NOD Payload tabel
-- Notification of Departure — conform EMSWe MIG v2.0.1 datacategorie "Notification of Departure"

CREATE TABLE nod_payload (
    formality_id          UUID        PRIMARY KEY REFERENCES formality(id),
    expected_departure    TIMESTAMPTZ NOT NULL,   -- Geplande vertrektijd (moet in toekomst liggen)
    next_port_locode      VARCHAR(10),             -- UN/LOCODE volgende haven
    destination_country   VARCHAR(3),              -- ISO 3166-1 alpha-3
    last_cargo_operations TIMESTAMPTZ              -- Einde laatste laad-/losoperatie
);
