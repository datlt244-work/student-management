-- =============================================
-- V8: Allow reusing department name after soft-delete
-- Convert global UNIQUE constraint to partial UNIQUE index (PostgreSQL)
-- =============================================

-- Drop global unique constraint (created in V1) so soft-deleted rows don't block reuse
ALTER TABLE departments DROP CONSTRAINT IF EXISTS departments_name_key;

-- Recreate uniqueness only for active (non-deleted) rows
DROP INDEX IF EXISTS uq_departments_name_active;

CREATE UNIQUE INDEX uq_departments_name_active
    ON departments (LOWER(name))
    WHERE deleted_at IS NULL;

