-- Thêm Assessment Item Resit cho môn HCM202
INSERT INTO assessment_items (course_id, category, name, weight, description)
SELECT course_id, 'Final exam Resit', 'Final exam Resit', 30.0, 'Resit for final exam'
FROM courses WHERE code = 'HCM202' ON CONFLICT DO NOTHING;
