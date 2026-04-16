-- V3: Maak de formality tabel aan
-- Supertype tabel voor alle formality types (NOA, NOS, NOD, VID, SID)
-- Bevat alle gemeenschappelijke velden conform EMSWe MIG v2.0.1 MAI-header

CREATE TABLE formality (
    id                 UUID               PRIMARY KEY DEFAULT gen_random_uuid(),
    visit_id           UUID               NOT NULL REFERENCES visit(id),
    type               formality_type     NOT NULL,
    version            INTEGER            NOT NULL DEFAULT 1,
    status             formality_status   NOT NULL DEFAULT 'SUBMITTED',
    submitter_id       UUID               NOT NULL REFERENCES app_user(id),
    lrn                VARCHAR(255),                         -- Local Reference Number (indiener-gekozen)
    message_identifier VARCHAR(255)       NOT NULL,          -- MAI berichtidentificatie
    submitted_at       TIMESTAMPTZ        NOT NULL DEFAULT now(),
    superseded_by      UUID               REFERENCES formality(id),  -- NULL tenzij gecorrigeerd
    channel            submission_channel NOT NULL DEFAULT 'WEB'
);
