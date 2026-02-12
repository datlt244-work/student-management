-- =============================================
-- V9: Add department relation to courses
-- Each course can belong to a department (optional).
-- =============================================

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS department_id INT;

-- FK: if a department is hard-deleted (rare), keep course but detach department
ALTER TABLE courses
    DROP CONSTRAINT IF EXISTS fk_courses_department;

ALTER TABLE courses
    ADD CONSTRAINT fk_courses_department
        FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_courses_department_id ON courses(department_id);


