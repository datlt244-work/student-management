-- ==========================================================
-- V30: Create attendance_appeals table for student reporting
-- ==========================================================

CREATE TABLE IF NOT EXISTS attendance_appeals (
    appeal_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(student_id),
    class_id INTEGER NOT NULL REFERENCES scheduled_classes(class_id),
    attendance_date DATE NOT NULL,
    reason TEXT NOT NULL,
    evidence_url TEXT NOT NULL, -- Mandatory evidence image
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    teacher_response TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraint: Each student can only appeal a specific session once
    UNIQUE (student_id, class_id, attendance_date)
);

-- Index for performance
CREATE INDEX idx_appeals_student ON attendance_appeals(student_id);
CREATE INDEX idx_appeals_class ON attendance_appeals(class_id);
CREATE INDEX idx_appeals_status ON attendance_appeals(status);
