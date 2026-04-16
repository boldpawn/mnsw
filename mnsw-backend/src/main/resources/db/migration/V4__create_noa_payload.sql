-- V4: NOA Payload tabel
-- Notification of Arrival — conform EMSWe MIG v2.0.1 datacategorie "Notification of Arrival"

CREATE TABLE noa_payload (
    formality_id       UUID         PRIMARY KEY REFERENCES formality(id),
    expected_arrival   TIMESTAMPTZ  NOT NULL,                -- Verwachte aankomsttijd (mag niet in verleden)
    last_port_locode   VARCHAR(10),                          -- UN/LOCODE vorige haven
    next_port_locode   VARCHAR(10),                          -- UN/LOCODE volgende haven
    purpose_of_call    VARCHAR(255),                         -- Doel van havenbezoek
    persons_on_board   INTEGER,                              -- Aantal personen aan boord (>= 0)
    dangerous_goods    BOOLEAN      NOT NULL DEFAULT false,  -- Gevaarlijke stoffen aan boord
    waste_delivery     BOOLEAN      NOT NULL DEFAULT false,  -- Afvalafgifte gewenst
    max_static_draught DECIMAL(5,2)                          -- Maximale statische diepgang in meters
);
