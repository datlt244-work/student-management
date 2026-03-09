-- ==========================================================
-- V31: Add audit identity fields to attendance_appeals
-- ==========================================================

ALTER TABLE attendance_appeals 
ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
