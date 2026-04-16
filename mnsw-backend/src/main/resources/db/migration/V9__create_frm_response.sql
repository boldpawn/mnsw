-- V9: FRM Response tabel
-- Formality Response Message — uitgaand antwoord van MNSW naar de indiener
-- FRM is GEEN inkomende formality — het is een systeem-gegenereerd antwoord

CREATE TABLE frm_response (
    id                  UUID               PRIMARY KEY DEFAULT gen_random_uuid(),
    formality_id        UUID               NOT NULL REFERENCES formality(id),
    status              frm_status         NOT NULL,
    reason_code         VARCHAR(50),        -- EMSWe foutcode bij REJECTED
    reason_description  TEXT,               -- Nederlandse omschrijving
    sent_at             TIMESTAMPTZ,        -- Tijdstip verzending FRM (NULL totdat verzonden)
    channel             submission_channel NOT NULL   -- Terugkanaal: WEB of RIM
);
