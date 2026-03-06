-- V22__Create_attendances_table.sql
CREATE TABLE IF NOT EXISTS attendances (
    attendance_id BIGSERIAL PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(student_id),
    class_id INT NOT NULL REFERENCES scheduled_classes(class_id),
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    record_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, class_id, date)
);
