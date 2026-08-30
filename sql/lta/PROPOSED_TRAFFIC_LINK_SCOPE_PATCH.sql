-- Proposed future patch only. Do not execute automatically in API01 Gate 1.
-- Purpose: make manual INCLUDE/EXCLUDE semantics explicit while preserving the
-- current many-to-many traffic_link_scope model.

ALTER TABLE lta.traffic_link_scope
    ADD COLUMN IF NOT EXISTS scope_action VARCHAR(10) NOT NULL DEFAULT 'INCLUDE';

ALTER TABLE lta.traffic_link_scope
    DROP CONSTRAINT IF EXISTS ck_traffic_link_scope_action;

ALTER TABLE lta.traffic_link_scope
    ADD CONSTRAINT ck_traffic_link_scope_action
        CHECK (scope_action IN ('INCLUDE','EXCLUDE'));

COMMENT ON COLUMN lta.traffic_link_scope.scope_action IS
    'Future manual correction action. INCLUDE preserves current behavior; EXCLUDE marks an explicit manual removal from a study area.';

