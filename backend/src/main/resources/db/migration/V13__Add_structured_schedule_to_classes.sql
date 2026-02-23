-- =============================================
-- V13: Add structured schedule to scheduled_classes
-- =============================================

-- 1. Add new structured schedule columns
ALTER TABLE scheduled_classes
    ADD COLUMN IF NOT EXISTS day_of_week INT,
    ADD COLUMN IF NOT EXISTS start_time TIME,
    ADD COLUMN IF NOT EXISTS end_time TIME;

-- 2. Clean up legacy columns
-- Drop the single string 'schedule' column
ALTER TABLE scheduled_classes DROP COLUMN IF EXISTS schedule;

-- Also clean up legacy semester/year string columns if they weren't dropped in V12
ALTER TABLE scheduled_classes DROP COLUMN IF EXISTS semester;
ALTER TABLE scheduled_classes DROP COLUMN IF EXISTS year;

-- 3. Set constraints for new columns (making them non-nullable after potential data migration/cleanup)
-- Note: Assuming we are okay with making these required for all classes.
-- If there were existing data, we would need to populate them first.
-- Since this is development/early stage, we'll enforce NOT NULL.

-- For safety, we can fill with defaults if we want to avoid errors on existing rows, 
-- but it's better to force correct data.
UPDATE scheduled_classes SET day_of_week = 1, start_time = '08:00', end_time = '10:00' WHERE day_of_week IS NULL;

ALTER TABLE scheduled_classes 
    ALTER COLUMN day_of_week SET NOT NULL,
    ALTER COLUMN start_time SET NOT NULL,
    ALTER COLUMN end_time SET NOT NULL;

-- 4. Re-verify indexes (V12 already added some, but let's ensure they are clean)
CREATE INDEX IF NOT EXISTS idx_scheduled_classes_schedule_lookup 
    ON scheduled_classes(teacher_id, semester_id, day_of_week, start_time, end_time);
