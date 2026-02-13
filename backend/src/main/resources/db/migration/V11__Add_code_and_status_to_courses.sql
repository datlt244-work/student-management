-- Add code and status columns to courses table

ALTER TABLE courses ADD COLUMN code VARCHAR(20);
ALTER TABLE courses ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL;

-- ALTER TABLE courses ALTER COLUMN code SET NOT NULL; -- Commented out to avoid failure on existing data
ALTER TABLE courses ADD CONSTRAINT uk_courses_code UNIQUE (code);
