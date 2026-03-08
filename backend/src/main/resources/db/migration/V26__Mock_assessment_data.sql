-- Thêm Assessment Items cho một số môn học phổ biến
INSERT INTO assessment_items (course_id, category, name, weight, description)
SELECT course_id, 'Participation', 'Participation', 10.0, 'Attendance and participation'
FROM courses WHERE code IN ('HCM202', 'MLN131', 'VNR202', 'SEP490') ON CONFLICT DO NOTHING;

INSERT INTO assessment_items (course_id, category, name, weight, description)
SELECT course_id, 'Progress test', 'Progress test', 20.0, 'In-class quiz or test'
FROM courses WHERE code IN ('HCM202', 'MLN131', 'VNR202', 'SEP490') ON CONFLICT DO NOTHING;

INSERT INTO assessment_items (course_id, category, name, weight, description)
SELECT course_id, 'Assignment', 'Assignment 1', 20.0, 'First assignment'
FROM courses WHERE code IN ('HCM202', 'MLN131', 'VNR202', 'SEP490') ON CONFLICT DO NOTHING;

INSERT INTO assessment_items (course_id, category, name, weight, description)
SELECT course_id, 'Assignment', 'Assignment 2', 20.0, 'Second assignment'
FROM courses WHERE code IN ('HCM202', 'MLN131', 'VNR202', 'SEP490') ON CONFLICT DO NOTHING;

INSERT INTO assessment_items (course_id, category, name, weight, description)
SELECT course_id, 'Final exam', 'Final exam', 30.0, 'Final comprehensive exam'
FROM courses WHERE code IN ('HCM202', 'MLN131', 'VNR202', 'SEP490') ON CONFLICT DO NOTHING;

-- Mock điểm cho sinh viên đầu tiên tìm thấy
DO $$
DECLARE
    v_enrollment_id INTEGER;
    v_item RECORD;
BEGIN
    FOR v_enrollment_id IN (SELECT enrollment_id FROM enrollments LIMIT 10) LOOP
        FOR v_item IN (
            SELECT item_id FROM assessment_items ai
            JOIN enrollments e ON ai.course_id = (SELECT course_id FROM scheduled_classes sc WHERE sc.class_id = e.class_id)
            WHERE e.enrollment_id = v_enrollment_id
        ) LOOP
            -- Random score between 7 and 9.5
            INSERT INTO student_scores (enrollment_id, assessment_item_id, score_value, comment)
            VALUES (v_enrollment_id, v_item.item_id, round((random() * 3 + 7)::numeric, 1), 'Góp ý tích cực từ giảng viên.')
            ON CONFLICT DO NOTHING;
        END LOOP;
    END LOOP;
END $$;
