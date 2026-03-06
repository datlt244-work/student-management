-- V23__Populate_mock_attendances.sql
-- Insert mock attendances for student HE170001 (user 5, student_id = 1) in math101 class (class_id = 1)
INSERT INTO attendances (student_id, class_id, date, status, record_time)
VALUES
  ((SELECT student_id FROM students WHERE student_code = 'HE170001'), 1, CURRENT_DATE - INTERVAL '14 days', 'ATTENDED', CURRENT_TIMESTAMP - INTERVAL '14 days'),
  ((SELECT student_id FROM students WHERE student_code = 'HE170001'), 1, CURRENT_DATE - INTERVAL ' 7 days', 'ABSENT',  CURRENT_TIMESTAMP - INTERVAL ' 7 days'),
  ((SELECT student_id FROM students WHERE student_code = 'HE170001'), 1, CURRENT_DATE,                'ATTENDED', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Some mock for cs101 class (class_id = 2) for the same student
INSERT INTO attendances (student_id, class_id, date, status, record_time)
VALUES
  ((SELECT student_id FROM students WHERE student_code = 'HE170001'), 2, CURRENT_DATE - INTERVAL '12 days', 'ATTENDED',    CURRENT_TIMESTAMP - INTERVAL '12 days'),
  ((SELECT student_id FROM students WHERE student_code = 'HE170001'), 2, CURRENT_DATE - INTERVAL ' 5 days', 'ATTENDED', CURRENT_TIMESTAMP - INTERVAL ' 5 days')
ON CONFLICT DO NOTHING;
