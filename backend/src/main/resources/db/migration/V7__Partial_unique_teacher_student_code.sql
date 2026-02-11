-- =============================================
-- V7: Allow reusing teacher_code/student_code after soft-delete
-- Convert global UNIQUE constraints to partial UNIQUE indexes (PostgreSQL)
-- =============================================

-- Drop global unique constraints (created in V2) so soft-deleted rows don't block reuse
ALTER TABLE teachers DROP CONSTRAINT IF EXISTS teachers_teacher_code_key;
ALTER TABLE students DROP CONSTRAINT IF EXISTS students_student_code_key;

-- Recreate uniqueness only for active (non-deleted) rows
DROP INDEX IF EXISTS uq_teachers_teacher_code_active;
DROP INDEX IF EXISTS uq_students_student_code_active;

CREATE UNIQUE INDEX uq_teachers_teacher_code_active
    ON teachers (teacher_code)
    WHERE deleted_at IS NULL AND teacher_code IS NOT NULL;

CREATE UNIQUE INDEX uq_students_student_code_active
    ON students (student_code)
    WHERE deleted_at IS NULL AND student_code IS NOT NULL;

