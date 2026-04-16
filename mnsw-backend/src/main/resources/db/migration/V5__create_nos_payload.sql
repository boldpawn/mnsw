-- V5: NOS Payload tabel
-- Notification of Sailing — conform EMSWe MIG v2.0.1 datacategorie "Notification of Sailing"

CREATE TABLE nos_payload (
    formality_id        UUID        PRIMARY KEY REFERENCES formality(id),
    actual_sailing      TIMESTAMPTZ NOT NULL,   -- Feitelijk vertrektijdstip (max 48u in toekomst)
    next_port_locode    VARCHAR(10),             -- UN/LOCODE volgende haven
    destination_country VARCHAR(3)               -- ISO 3166-1 alpha-3
);
