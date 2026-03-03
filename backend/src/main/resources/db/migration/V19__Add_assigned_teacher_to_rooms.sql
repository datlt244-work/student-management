-- =============================================
-- V19: Add assigned_teacher_id to rooms table
-- Allows tracking which teacher "owns" an office room
-- =============================================

ALTER TABLE rooms ADD COLUMN IF NOT EXISTS assigned_teacher_id UUID;

ALTER TABLE rooms ADD CONSTRAINT fk_rooms_assigned_teacher
    FOREIGN KEY (assigned_teacher_id) REFERENCES teachers(teacher_id)
    ON DELETE SET NULL;

-- Unique constraint: one teacher per room (only for non-null values)
CREATE UNIQUE INDEX IF NOT EXISTS uk_rooms_assigned_teacher
    ON rooms(assigned_teacher_id) WHERE assigned_teacher_id IS NOT NULL AND deleted_at IS NULL;
