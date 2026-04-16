-- V10: Database indexes voor query-optimalisatie
-- Gebaseerd op primaire querypaden conform data-model.md

-- formality: meest gebruikte filterkolommen
CREATE INDEX idx_formality_submitter_id    ON formality(submitter_id);
CREATE INDEX idx_formality_visit_id        ON formality(visit_id);
CREATE INDEX idx_formality_status          ON formality(status);
CREATE INDEX idx_formality_type            ON formality(type);
CREATE INDEX idx_formality_message_identifier ON formality(message_identifier);

-- visit: gefilterd zoeken per haven en schip
CREATE INDEX idx_visit_port_locode         ON visit(port_locode);
CREATE INDEX idx_visit_imo_number          ON visit(imo_number);

-- app_user: login en gebruikersopzoekingen
CREATE INDEX idx_app_user_email            ON app_user(email);
