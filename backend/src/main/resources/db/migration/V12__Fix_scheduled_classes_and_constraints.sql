-- =============================================
-- V12: Fix scheduled_classes relationships & constraints
-- =============================================

-- 1. Thêm semester_id FK vào scheduled_classes
--    semester_id nullable trong bước đầu để tránh lỗi khi có dữ liệu cũ.
ALTER TABLE scheduled_classes
    ADD COLUMN IF NOT EXISTS semester_id INT;

ALTER TABLE scheduled_classes
    ADD CONSTRAINT fk_classes_semester
        FOREIGN KEY (semester_id) REFERENCES semesters(semester_id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_scheduled_classes_semester_id ON scheduled_classes(semester_id);

-- 2. Xóa các cột semester (VARCHAR) và year (INT) đã thừa
--    Mọi thông tin (name, year, start_date, end_date) lấy qua JOIN với semesters.
ALTER TABLE scheduled_classes DROP COLUMN IF EXISTS semester;
ALTER TABLE scheduled_classes DROP COLUMN IF EXISTS year;

-- 3. Fix ON DELETE CASCADE → RESTRICT trên fk_classes_course
--    (Không cho xóa course nếu còn scheduled_classes — xài soft delete thay thế)
ALTER TABLE scheduled_classes
    DROP CONSTRAINT IF EXISTS fk_classes_course;

ALTER TABLE scheduled_classes
    ADD CONSTRAINT fk_classes_course
        FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE RESTRICT;

-- 4. Fix ON DELETE CASCADE → RESTRICT trên fk_enrollments_class
--    (Không cho xóa lớp nếu còn enrollment — bảo toàn lịch sử học tập)
ALTER TABLE enrollments
    DROP CONSTRAINT IF EXISTS fk_enrollments_class;

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_class
        FOREIGN KEY (class_id) REFERENCES scheduled_classes(class_id) ON DELETE RESTRICT;

-- 5. Fix ON DELETE CASCADE → RESTRICT trên fk_enrollments_student
--    (Không cho xóa student nếu còn enrollment)
ALTER TABLE enrollments
    DROP CONSTRAINT IF EXISTS fk_enrollments_student;

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_student
        FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE RESTRICT;

-- 6. Fix ON DELETE CASCADE → RESTRICT trên fk_grades_enrollment
ALTER TABLE grades
    DROP CONSTRAINT IF EXISTS fk_grades_enrollment;

ALTER TABLE grades
    ADD CONSTRAINT fk_grades_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE RESTRICT;

-- 7. Fix courses.name UNIQUE → Partial unique (chỉ apply khi deleted_at IS NULL)
--    V1 tạo UNIQUE mức column level → cần drop + tạo lại dạng partial index
ALTER TABLE courses
    DROP CONSTRAINT IF EXISTS courses_name_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_courses_name_active
    ON courses(LOWER(name)) WHERE deleted_at IS NULL;

-- 8. Thêm index hỗ trợ query cho module Class
CREATE INDEX IF NOT EXISTS idx_scheduled_classes_course_id ON scheduled_classes(course_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_classes_teacher_id ON scheduled_classes(teacher_id);

-- 9. Add status column to scheduled_classes (OPEN / CLOSED / CANCELLED)
--    OPEN = đang có thể đăng ký
--    CLOSED = hết slot hoặc đã khoá
--    CANCELLED = lớp bị hủy (không xóa vật lý để giữ enrollment history)
ALTER TABLE scheduled_classes
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED'));

-- 10. Add max_students để kiểm soát sĩ số
ALTER TABLE scheduled_classes
    ADD COLUMN IF NOT EXISTS max_students INT DEFAULT 40 CHECK (max_students > 0);
