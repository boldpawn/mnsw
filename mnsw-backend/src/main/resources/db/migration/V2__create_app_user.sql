-- V2: Maak de app_user tabel aan
-- app_user moet voor formality worden aangemaakt vanwege de FK submitter_id -> app_user.id

CREATE TABLE app_user (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,              -- BCrypt hash
    full_name     VARCHAR(255) NOT NULL,
    eori          VARCHAR(17),                        -- Economic Operators Registration and Identification
                                                      -- Nullable: wordt later ingevuld door ADMIN
    role          user_role    NOT NULL,
    port_locode   VARCHAR(10),                        -- Alleen verplicht voor HAVENAUTORITEIT
    active        BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
