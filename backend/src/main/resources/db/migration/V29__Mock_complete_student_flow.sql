-- ==========================================================
-- V29: Standardized Grade Categories and Order for HE170014
-- ==========================================================

DO $$
DECLARE
    v_student_id UUID := '00000000-0000-0000-0000-000000002014';
    v_semester_spring_2026_id INTEGER;
    v_semester_fall_2025_id INTEGER;
    v_course_mkt301_id INTEGER;
    v_course_mkt101_id INTEGER;
    v_class_mkt301_id INTEGER;
    v_class_mkt101_id INTEGER;
    v_enroll_spring_id INTEGER;
    v_enroll_fall_id INTEGER;
BEGIN
    -- 1. Get Semester IDs
    SELECT semester_id INTO v_semester_spring_2026_id FROM semesters WHERE name = 'SPRING' AND year = 2026;
    SELECT semester_id INTO v_semester_fall_2025_id FROM semesters WHERE name = 'FALL' AND year = 2025;

    -- 2. GLOBAL STANDARDIZATION: Update all assessment categories in DB to match standard casing
    UPDATE assessment_items SET category = 'Participation' WHERE category ILIKE 'Participation' OR category ILIKE 'Attendance%';
    UPDATE assessment_items SET category = 'Progress Test' WHERE category ILIKE 'Progress test%';
    UPDATE assessment_items SET category = 'Assignment' WHERE category ILIKE 'Assignment%';
    UPDATE assessment_items SET category = 'Final Exam' WHERE category ILIKE 'Final exam%';

    -- 3. Cleanup specific courses we are mocking for this flow
    DELETE FROM student_scores WHERE assessment_item_id IN (
        SELECT item_id FROM assessment_items WHERE course_id IN (
            SELECT course_id FROM courses WHERE code IN ('MKT301', 'MKT101')
        ) OR course_id IN (4, 8)
    );
    DELETE FROM assessment_items WHERE course_id IN (SELECT course_id FROM courses WHERE code IN ('MKT301', 'MKT101')) OR course_id IN (4, 8);

    -- Cleanup enrollments/classes for current semester to avoid duplicates
    DELETE FROM grades WHERE enrollment_id IN (SELECT enrollment_id FROM enrollments WHERE student_id = v_student_id);
    DELETE FROM attendances WHERE student_id = v_student_id;
    
    DELETE FROM class_sessions WHERE class_id IN (
        SELECT class_id FROM scheduled_classes WHERE semester_id = v_semester_spring_2026_id 
        AND course_id IN (SELECT course_id FROM courses WHERE code IN ('MKT301', 'MKT101'))
    );
    
    DELETE FROM enrollments WHERE student_id = v_student_id AND class_id IN (
        SELECT class_id FROM scheduled_classes WHERE semester_id = v_semester_spring_2026_id 
        OR course_id IN (4, 8)
    );

    DELETE FROM scheduled_classes WHERE semester_id = v_semester_spring_2026_id 
    AND course_id IN (SELECT course_id FROM courses WHERE code IN ('MKT301', 'MKT101'));

    -- 4. Setup Courses with EXACT 4 Standard Items
    -- MKT301
    INSERT INTO courses (name, code, credits, description)
    VALUES ('Digital Marketing Strategy', 'MKT301', 3, 'Strategic digital marketing.')
    ON CONFLICT (code) WHERE deleted_at IS NULL DO UPDATE SET name = EXCLUDED.name
    RETURNING course_id INTO v_course_mkt301_id;

    INSERT INTO assessment_items (course_id, category, name, weight) VALUES
    (v_course_mkt301_id, 'Participation', 'Class Participation', 10.0),
    (v_course_mkt301_id, 'Progress Test', 'Mid-term Test', 20.0),
    (v_course_mkt301_id, 'Assignment', 'Project Report', 30.0),
    (v_course_mkt301_id, 'Final Exam', 'Final Examination', 40.0);

    -- MKT101
    INSERT INTO courses (name, code, credits, description)
    VALUES ('Principles of Marketing', 'MKT101', 3, 'Foundational marketing.')
    ON CONFLICT (code) WHERE deleted_at IS NULL DO UPDATE SET name = EXCLUDED.name
    RETURNING course_id INTO v_course_mkt101_id;

    INSERT INTO assessment_items (course_id, category, name, weight) VALUES
    (v_course_mkt101_id, 'Participation', 'Participation', 10.0),
    (v_course_mkt101_id, 'Progress Test', 'Mid-term quiz', 20.0),
    (v_course_mkt101_id, 'Assignment', 'Market Research', 30.0),
    (v_course_mkt101_id, 'Final Exam', 'Final Exam', 40.0);

    -- 5. Calculus I (ID 4) & Principles of Management (ID 8)
    INSERT INTO assessment_items (course_id, category, name, weight) VALUES
    (4, 'Participation', 'Attendance', 10.0),
    (4, 'Progress Test', 'Quiz 1', 20.0),
    (4, 'Assignment', 'Problem Set', 30.0),
    (4, 'Final Exam', 'Final Exam', 40.0),
    (8, 'Participation', 'Participation', 10.0),
    (8, 'Progress Test', 'Mid-term Test', 20.0),
    (8, 'Assignment', 'Group Project', 30.0),
    (8, 'Final Exam', 'Final Exam', 40.0);

    -- 6. Re-create Classes and Enrollments
    INSERT INTO scheduled_classes (course_id, teacher_id, semester_id, status, max_students)
    VALUES (v_course_mkt301_id, '00000000-0000-0000-0000-000000001005', v_semester_spring_2026_id, 'OPEN', 40)
    RETURNING class_id INTO v_class_mkt301_id;

    INSERT INTO scheduled_classes (course_id, teacher_id, semester_id, status, max_students)
    VALUES (v_course_mkt101_id, '00000000-0000-0000-0000-000000001004', v_semester_spring_2026_id, 'OPEN', 40);

    INSERT INTO enrollments (student_id, class_id, enrollment_date, status)
    VALUES (v_student_id, v_class_mkt301_id, '2026-01-10', 'ENROLLED') RETURNING enrollment_id INTO v_enroll_spring_id;

    -- History for Fall 2025
    INSERT INTO enrollments (student_id, class_id, enrollment_date, status)
    VALUES (v_student_id, 2, '2025-08-20', 'ENROLLED') RETURNING enrollment_id INTO v_enroll_fall_id;
    INSERT INTO grades (enrollment_id, grade_value, feedback) VALUES (v_enroll_fall_id, 9.5, 'Excellent.');
    INSERT INTO student_scores (enrollment_id, assessment_item_id, score_value)
    SELECT v_enroll_fall_id, item_id, CASE WHEN category='Participation' THEN 10 WHEN category='Progress Test' THEN 9 WHEN category='Assignment' THEN 9.5 ELSE 9.7 END
    FROM assessment_items WHERE course_id = 4;

    INSERT INTO enrollments (student_id, class_id, enrollment_date, status)
    VALUES (v_student_id, 5, '2025-08-20', 'ENROLLED') RETURNING enrollment_id INTO v_enroll_fall_id;
    INSERT INTO grades (enrollment_id, grade_value, feedback) VALUES (v_enroll_fall_id, 8.8, 'Great leader.');
    INSERT INTO student_scores (enrollment_id, assessment_item_id, score_value)
    SELECT v_enroll_fall_id, item_id, CASE WHEN category='Participation' THEN 9 WHEN category='Progress Test' THEN 8.5 WHEN category='Assignment' THEN 9 ELSE 8.7 END
    FROM assessment_items WHERE course_id = 8;

END $$;
