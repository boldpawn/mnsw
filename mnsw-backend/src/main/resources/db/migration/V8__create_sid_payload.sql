-- V8: SID Payload tabel
-- Ship/Security Information Document (ISPS) — conform EMSWe MIG v2.0.1 datacategorie "Security"
-- last_10_ports is JSONB: gerechtvaardigde uitzondering op type-safe payload principe
-- (dynamische lijst van complexe havenobjecten met aankomst/vertrek tijden; geen eigen join-tabel
--  vanwege lees-intensief karakter en beperkte schrijffrequentie)

CREATE TABLE sid_payload (
    formality_id           UUID     PRIMARY KEY REFERENCES formality(id),
    isps_level             INTEGER  NOT NULL CHECK (isps_level IN (1, 2, 3)),  -- ISPS niveau 1, 2 of 3
    last_10_ports          JSONB,   -- Lijst van laatste 10 aanloophavens [{locode, arrival, departure, ispsLevel}]
    security_declaration   VARCHAR(50),    -- Beveiligingsverklaring type
    ship_to_ship_activities BOOLEAN NOT NULL DEFAULT false,  -- Schip-tot-schip activiteiten gehad
    designated_authority   VARCHAR(255),   -- Bevoegde ISPS-autoriteit (verplicht bij isps_level >= 2)
    ssas_activated         BOOLEAN  NOT NULL DEFAULT false   -- Ship Security Alert System geactiveerd
);
