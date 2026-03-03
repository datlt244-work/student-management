-- =============================================
-- V18: Refactor class sessions and rooms
-- =============================================

-- 1. Create rooms table
CREATE TABLE IF NOT EXISTS rooms (
    room_id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    capacity INT NOT NULL,
    room_type VARCHAR(20) NOT NULL,
    has_projector BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_rooms_name_active
    ON rooms(LOWER(name)) WHERE deleted_at IS NULL;

-- 2. Insert some default rooms
INSERT INTO rooms (name, capacity, room_type, has_projector) VALUES
('A-101', 40, 'LECTURE_HALL', true),
('A-102', 60, 'LECTURE_HALL', true),
('Lab-201', 30, 'COMPUTER_LAB', true),
('Meeting-301', 20, 'MEETING_ROOM', false)
ON CONFLICT DO NOTHING;

-- 3. Create class_sessions table
CREATE TABLE IF NOT EXISTS class_sessions (
    session_id BIGSERIAL PRIMARY KEY,
    class_id INT NOT NULL,
    room_id INT NOT NULL,
    day_of_week INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT fk_sessions_class FOREIGN KEY (class_id) REFERENCES scheduled_classes(class_id) ON DELETE CASCADE,
    CONSTRAINT fk_sessions_room FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_session_room_datetime 
    ON class_sessions(room_id, day_of_week, start_time, end_time);

-- 4. Migrate existing data from scheduled_classes to class_sessions
-- We will assume there is a room 'A-101' created with ID 1, we will map existing classes to it for now
INSERT INTO class_sessions (class_id, room_id, day_of_week, start_time, end_time)
SELECT 
    class_id,
    1 as room_id, -- default room
    day_of_week,
    start_time,
    end_time
FROM scheduled_classes
WHERE day_of_week IS NOT NULL
ON CONFLICT DO NOTHING;

-- 5. Drop legacy columns from scheduled_classes
ALTER TABLE scheduled_classes DROP COLUMN IF EXISTS room_number;
ALTER TABLE scheduled_classes DROP COLUMN IF EXISTS day_of_week;
ALTER TABLE scheduled_classes DROP COLUMN IF EXISTS start_time;
ALTER TABLE scheduled_classes DROP COLUMN IF EXISTS end_time;

-- Drop legacy index
DROP INDEX IF EXISTS idx_scheduled_classes_schedule_lookup;
