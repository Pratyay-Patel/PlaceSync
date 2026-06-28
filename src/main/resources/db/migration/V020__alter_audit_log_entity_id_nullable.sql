-- entity_id must be nullable to support LOGIN_FAILURE audit entries
-- where no entity can be identified (user may not exist)
ALTER TABLE audit_log ALTER COLUMN entity_id DROP NOT NULL;
