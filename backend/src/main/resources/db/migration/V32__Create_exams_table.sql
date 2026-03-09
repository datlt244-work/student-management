-- ==========================================================
-- V32: Create exams table for exam scheduling
-- ==========================================================

DROP TABLE IF EXISTS exams CASCADE;

CREATE TABLE exams (
    exam_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id INTEGER NOT NULL REFERENCES scheduled_classes(class_id),
    room_id INTEGER REFERENCES rooms(room_id),
    exam_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    exam_type VARCHAR(20) NOT NULL DEFAULT 'FINAL', -- FINAL, RESIT
    note TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Index for performance
CREATE INDEX idx_exams_class ON exams(class_id);
CREATE INDEX idx_exams_date ON exams(exam_date);

-- Mock data for the current semester classes
-- FINAL exams should be at the end of the semester
INSERT INTO exams (class_id, room_id, exam_date, start_time, end_time, exam_type, note)
SELECT 
    sc.class_id, 
    1, 
    s.end_date - INTERVAL '7 days', -- Giả định tuần thi cuối cùng
    '08:00:00', 
    '10:00:00', 
    'FINAL', 
    'Thi tập trung tại phòng ' || r.name || '. Mang theo thẻ sinh viên.'
FROM scheduled_classes sc
JOIN semesters s ON sc.semester_id = s.semester_id
JOIN rooms r ON r.room_id = 1
WHERE s.is_current = true
LIMIT 5;
