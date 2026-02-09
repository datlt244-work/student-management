-- ==========================================================
-- V3: Mock data for development and testing
-- ==========================================================
-- Default password for ALL users: ThanhDat003@
-- BCrypt hash: $2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei
-- Regenerate: PasswordHashGenerator.java
-- ==========================================================


-- ----------------------------------------------------------
-- 1. ROLES
-- ----------------------------------------------------------
INSERT INTO roles (role_name, description) VALUES
    ('ADMIN',   'System Administrator with full access'),
    ('TEACHER', 'Teacher / Instructor'),
    ('STUDENT', 'Enrolled Student');


-- ----------------------------------------------------------
-- 2. DEPARTMENTS
-- ----------------------------------------------------------
INSERT INTO departments (name, office_location) VALUES
    ('Computer Science',        'Building A, Room 101'),
    ('Mathematics',             'Building B, Room 201'),
    ('Physics',                 'Building C, Room 301'),
    ('Foreign Languages',       'Building D, Room 401'),
    ('Business Administration', 'Building E, Room 501');


-- ----------------------------------------------------------
-- 3. USERS  (password = Password@123)
-- ----------------------------------------------------------

-- Admin
INSERT INTO users (user_id, email, password_hash, role_id, status, email_verified, email_verified_at, login_count) VALUES
    ('00000000-0000-0000-0000-000000000001', 'admin@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     1, 'ACTIVE', true, '2025-09-01 08:00:00', 42);

-- Teachers (role_id = 2)
INSERT INTO users (user_id, email, password_hash, role_id, status, email_verified, email_verified_at, login_count) VALUES
    ('00000000-0000-0000-0000-000000000101', 'nguyen.van.an@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     2, 'ACTIVE', true, '2025-09-01 08:00:00', 120),

    ('00000000-0000-0000-0000-000000000102', 'tran.thi.bich@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     2, 'ACTIVE', true, '2025-09-02 09:00:00', 98),

    ('00000000-0000-0000-0000-000000000103', 'le.hoang.cuong@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     2, 'ACTIVE', true, '2025-09-03 10:00:00', 85),

    ('00000000-0000-0000-0000-000000000104', 'pham.minh.duc@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     2, 'ACTIVE', true, '2025-09-04 08:30:00', 76),

    ('00000000-0000-0000-0000-000000000105', 'vu.thi.huong@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     2, 'ACTIVE', true, '2025-09-05 09:15:00', 63);

-- Students (role_id = 3)
INSERT INTO users (user_id, email, password_hash, role_id, status, email_verified, email_verified_at, login_count) VALUES
    ('00000000-0000-0000-0000-000000000201', 'hoang.minh.tuan@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-10 07:30:00', 45),

    ('00000000-0000-0000-0000-000000000202', 'nguyen.thi.lan@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-10 08:00:00', 38),

    ('00000000-0000-0000-0000-000000000203', 'tran.duc.manh@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-11 09:00:00', 52),

    ('00000000-0000-0000-0000-000000000204', 'le.thi.hong@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-11 10:00:00', 29),

    ('00000000-0000-0000-0000-000000000205', 'pham.van.khoa@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-12 07:45:00', 61),

    ('00000000-0000-0000-0000-000000000206', 'do.thi.mai@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-12 08:30:00', 33),

    ('00000000-0000-0000-0000-000000000207', 'bui.quang.huy@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-13 09:00:00', 47),

    ('00000000-0000-0000-0000-000000000208', 'vo.thi.ngoc@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-13 10:30:00', 19),

    ('00000000-0000-0000-0000-000000000209', 'dang.thanh.long@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-14 07:00:00', 55),

    ('00000000-0000-0000-0000-000000000210', 'ngo.phuong.linh@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-14 08:15:00', 41),

    ('00000000-0000-0000-0000-000000000211', 'truong.van.nam@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-15 09:30:00', 22),

    ('00000000-0000-0000-0000-000000000212', 'ly.thi.oanh@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-15 10:00:00', 36),

    ('00000000-0000-0000-0000-000000000213', 'dinh.quoc.phong@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-16 07:30:00', 14),

    ('00000000-0000-0000-0000-000000000214', 'ha.thi.quynh@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-16 08:45:00', 27),

    ('00000000-0000-0000-0000-000000000215', 'luu.van.son@fpt.edu.vn',
     '$2a$10$edwZXyIeonftmWZ6QFGuIOvQqMvFtwK/bXsr2lWMvd5uYxqWloYei',
     3, 'ACTIVE', true, '2025-09-17 09:00:00', 50);


-- ----------------------------------------------------------
-- 4. TEACHERS
-- ----------------------------------------------------------
INSERT INTO teachers (teacher_id, user_id, department_id, teacher_code, first_name, last_name, email, phone, specialization, academic_rank, office_room) VALUES
    ('00000000-0000-0000-0000-000000001001',
     '00000000-0000-0000-0000-000000000101', 1,
     'HJ170001', 'An', 'Nguyen Van',
     'nguyen.van.an@fpt.edu.vn', '0901000001',
     'Artificial Intelligence', 'Associate Professor', 'A-201'),

    ('00000000-0000-0000-0000-000000001002',
     '00000000-0000-0000-0000-000000000102', 2,
     'HJ170002', 'Bich', 'Tran Thi',
     'tran.thi.bich@fpt.edu.vn', '0901000002',
     'Applied Mathematics', 'Lecturer', 'B-205'),

    ('00000000-0000-0000-0000-000000001003',
     '00000000-0000-0000-0000-000000000103', 3,
     'HJ170003', 'Cuong', 'Le Hoang',
     'le.hoang.cuong@fpt.edu.vn', '0901000003',
     'Quantum Physics', 'Professor', 'C-302'),

    ('00000000-0000-0000-0000-000000001004',
     '00000000-0000-0000-0000-000000000104', 4,
     'HJ170004', 'Duc', 'Pham Minh',
     'pham.minh.duc@fpt.edu.vn', '0901000004',
     'English Linguistics', 'Lecturer', 'D-403'),

    ('00000000-0000-0000-0000-000000001005',
     '00000000-0000-0000-0000-000000000105', 5,
     'HJ170005', 'Huong', 'Vu Thi',
     'vu.thi.huong@fpt.edu.vn', '0901000005',
     'Strategic Management', 'Associate Professor', 'E-504');


-- ----------------------------------------------------------
-- 5. STUDENTS
-- ----------------------------------------------------------
INSERT INTO students (student_id, user_id, department_id, student_code, first_name, last_name, dob, gender, major, email, phone, address, gpa) VALUES
    -- Computer Science students
    ('00000000-0000-0000-0000-000000002001',
     '00000000-0000-0000-0000-000000000201', 1,
     'HE170001', 'Tuan', 'Hoang Minh',
     '2003-03-15', 'MALE', 'Software Engineering',
     'hoang.minh.tuan@fpt.edu.vn', '0912000001',
     '12 Tran Phu, Ha Noi', 3.45),

    ('00000000-0000-0000-0000-000000002002',
     '00000000-0000-0000-0000-000000000202', 1,
     'HE170002', 'Lan', 'Nguyen Thi',
     '2003-07-22', 'FEMALE', 'Software Engineering',
     'nguyen.thi.lan@fpt.edu.vn', '0912000002',
     '45 Le Loi, Ha Noi', 3.72),

    ('00000000-0000-0000-0000-000000002003',
     '00000000-0000-0000-0000-000000000203', 1,
     'HE170003', 'Manh', 'Tran Duc',
     '2003-01-08', 'MALE', 'Information Security',
     'tran.duc.manh@fpt.edu.vn', '0912000003',
     '78 Nguyen Hue, HCM', 3.18),

    -- Mathematics students
    ('00000000-0000-0000-0000-000000002004',
     '00000000-0000-0000-0000-000000000204', 2,
     'HE170004', 'Hong', 'Le Thi',
     '2003-11-30', 'FEMALE', 'Applied Mathematics',
     'le.thi.hong@fpt.edu.vn', '0912000004',
     '23 Hai Ba Trung, Ha Noi', 3.85),

    ('00000000-0000-0000-0000-000000002005',
     '00000000-0000-0000-0000-000000000205', 2,
     'HE170005', 'Khoa', 'Pham Van',
     '2002-05-14', 'MALE', 'Statistics',
     'pham.van.khoa@fpt.edu.vn', '0912000005',
     '56 Ly Thuong Kiet, Ha Noi', 3.52),

    ('00000000-0000-0000-0000-000000002006',
     '00000000-0000-0000-0000-000000000206', 2,
     'HE170006', 'Mai', 'Do Thi',
     '2003-09-03', 'FEMALE', 'Applied Mathematics',
     'do.thi.mai@fpt.edu.vn', '0912000006',
     '89 Quang Trung, Da Nang', 3.30),

    -- Physics students
    ('00000000-0000-0000-0000-000000002007',
     '00000000-0000-0000-0000-000000000207', 3,
     'HE170007', 'Huy', 'Bui Quang',
     '2003-04-20', 'MALE', 'Applied Physics',
     'bui.quang.huy@fpt.edu.vn', '0912000007',
     '34 Bach Dang, Ha Noi', 3.10),

    ('00000000-0000-0000-0000-000000002008',
     '00000000-0000-0000-0000-000000000208', 3,
     'HE170008', 'Ngoc', 'Vo Thi',
     '2003-12-25', 'FEMALE', 'Nuclear Physics',
     'vo.thi.ngoc@fpt.edu.vn', '0912000008',
     '67 Phan Chu Trinh, Hue', 3.65),

    ('00000000-0000-0000-0000-000000002009',
     '00000000-0000-0000-0000-000000000209', 3,
     'HE170009', 'Long', 'Dang Thanh',
     '2002-08-17', 'MALE', 'Applied Physics',
     'dang.thanh.long@fpt.edu.vn', '0912000009',
     '90 Nguyen Trai, Ha Noi', 2.95),

    -- Foreign Languages students
    ('00000000-0000-0000-0000-000000002010',
     '00000000-0000-0000-0000-000000000210', 4,
     'HE170010', 'Linh', 'Ngo Phuong',
     '2003-06-11', 'FEMALE', 'English Studies',
     'ngo.phuong.linh@fpt.edu.vn', '0912000010',
     '12 Tran Hung Dao, Ha Noi', 3.78),

    ('00000000-0000-0000-0000-000000002011',
     '00000000-0000-0000-0000-000000000211', 4,
     'HE170011', 'Nam', 'Truong Van',
     '2003-02-28', 'MALE', 'English Studies',
     'truong.van.nam@fpt.edu.vn', '0912000011',
     '45 Le Duan, Da Nang', 3.20),

    ('00000000-0000-0000-0000-000000002012',
     '00000000-0000-0000-0000-000000000212', 4,
     'HE170012', 'Oanh', 'Ly Thi',
     '2003-10-05', 'FEMALE', 'Japanese Studies',
     'ly.thi.oanh@fpt.edu.vn', '0912000012',
     '78 Hoang Dieu, Hue', 3.55),

    -- Business Administration students
    ('00000000-0000-0000-0000-000000002013',
     '00000000-0000-0000-0000-000000000213', 5,
     'HE170013', 'Phong', 'Dinh Quoc',
     '2002-07-19', 'MALE', 'Business Management',
     'dinh.quoc.phong@fpt.edu.vn', '0912000013',
     '23 Vo Van Tan, HCM', 3.40),

    ('00000000-0000-0000-0000-000000002014',
     '00000000-0000-0000-0000-000000000214', 5,
     'HE170014', 'Quynh', 'Ha Thi',
     '2003-08-08', 'FEMALE', 'Marketing',
     'ha.thi.quynh@fpt.edu.vn', '0912000014',
     '56 Nguyen Van Cu, Ha Noi', 3.62),

    ('00000000-0000-0000-0000-000000002015',
     '00000000-0000-0000-0000-000000000215', 5,
     'HE170015', 'Son', 'Luu Van',
     '2002-12-01', 'MALE', 'Finance & Banking',
     'luu.van.son@fpt.edu.vn', '0912000015',
     '89 Dien Bien Phu, HCM', 3.25);


-- ----------------------------------------------------------
-- 6. COURSES
-- ----------------------------------------------------------
INSERT INTO courses (course_id, name, credits, description) VALUES
    (1,  'Introduction to Programming',       3, 'Fundamentals of programming using Java. Covers variables, control flow, OOP basics.'),
    (2,  'Data Structures and Algorithms',     4, 'Arrays, linked lists, trees, graphs, sorting, searching, and algorithm complexity analysis.'),
    (3,  'Database Systems',                   3, 'Relational database design, SQL, normalization, indexing, and transaction management.'),
    (4,  'Calculus I',                         4, 'Limits, derivatives, integrals, and their applications in science and engineering.'),
    (5,  'Linear Algebra',                     3, 'Vectors, matrices, determinants, eigenvalues, and linear transformations.'),
    (6,  'General Physics I',                  4, 'Mechanics, thermodynamics, and waves. Includes laboratory sessions.'),
    (7,  'English Communication',              2, 'Develop speaking, listening, reading, and writing skills for academic purposes.'),
    (8,  'Principles of Management',           3, 'Planning, organizing, leading, and controlling in business organizations.'),
    (9,  'Software Engineering',               3, 'Software development lifecycle, Agile, testing, version control, and project management.'),
    (10, 'Web Development',                    3, 'HTML, CSS, JavaScript, and modern frameworks. Build full-stack web applications.');


-- ----------------------------------------------------------
-- 7. SCHEDULED CLASSES  (Fall 2025 + Spring 2026)
-- ----------------------------------------------------------
INSERT INTO scheduled_classes (class_id, course_id, teacher_id, semester, year, room_number, schedule) VALUES
    -- ===== Fall 2025 (completed) =====
    (1,  1,  '00000000-0000-0000-0000-000000001001', 'FALL', 2025, 'A-101', 'Mon 08:00-10:00'),
    (2,  4,  '00000000-0000-0000-0000-000000001002', 'FALL', 2025, 'B-201', 'Tue 08:00-10:00'),
    (3,  6,  '00000000-0000-0000-0000-000000001003', 'FALL', 2025, 'C-301', 'Wed 10:00-12:00'),
    (4,  7,  '00000000-0000-0000-0000-000000001004', 'FALL', 2025, 'D-401', 'Thu 13:00-15:00'),
    (5,  8,  '00000000-0000-0000-0000-000000001005', 'FALL', 2025, 'E-501', 'Fri 08:00-10:00'),
    (6,  5,  '00000000-0000-0000-0000-000000001002', 'FALL', 2025, 'B-202', 'Thu 08:00-10:00'),

    -- ===== Spring 2026 (current) =====
    (7,  2,  '00000000-0000-0000-0000-000000001001', 'SPRING', 2026, 'A-102', 'Mon 10:00-12:00'),
    (8,  3,  '00000000-0000-0000-0000-000000001001', 'SPRING', 2026, 'A-103', 'Wed 08:00-10:00'),
    (9,  9,  '00000000-0000-0000-0000-000000001001', 'SPRING', 2026, 'A-104', 'Fri 10:00-12:00'),
    (10, 4,  '00000000-0000-0000-0000-000000001002', 'SPRING', 2026, 'B-203', 'Tue 10:00-12:00'),
    (11, 10, '00000000-0000-0000-0000-000000001001', 'SPRING', 2026, 'A-105', 'Thu 14:00-16:00'),
    (12, 7,  '00000000-0000-0000-0000-000000001004', 'SPRING', 2026, 'D-402', 'Mon 13:00-15:00'),
    (13, 6,  '00000000-0000-0000-0000-000000001003', 'SPRING', 2026, 'C-302', 'Wed 14:00-16:00'),
    (14, 8,  '00000000-0000-0000-0000-000000001005', 'SPRING', 2026, 'E-502', 'Tue 13:00-15:00'),
    (15, 5,  '00000000-0000-0000-0000-000000001002', 'SPRING', 2026, 'B-204', 'Fri 08:00-10:00');


-- ----------------------------------------------------------
-- 8. ENROLLMENTS
-- ----------------------------------------------------------

-- ===== Fall 2025 enrollments =====
INSERT INTO enrollments (enrollment_id, student_id, class_id, enrollment_date) VALUES
    -- Intro to Programming (class 1)
    (1,  '00000000-0000-0000-0000-000000002001', 1, '2025-08-25'),
    (2,  '00000000-0000-0000-0000-000000002002', 1, '2025-08-25'),
    (3,  '00000000-0000-0000-0000-000000002003', 1, '2025-08-26'),
    (4,  '00000000-0000-0000-0000-000000002007', 1, '2025-08-27'),

    -- Calculus I (class 2)
    (5,  '00000000-0000-0000-0000-000000002004', 2, '2025-08-25'),
    (6,  '00000000-0000-0000-0000-000000002005', 2, '2025-08-25'),
    (7,  '00000000-0000-0000-0000-000000002006', 2, '2025-08-26'),
    (8,  '00000000-0000-0000-0000-000000002001', 2, '2025-08-27'),

    -- General Physics I (class 3)
    (9,  '00000000-0000-0000-0000-000000002007', 3, '2025-08-25'),
    (10, '00000000-0000-0000-0000-000000002008', 3, '2025-08-25'),
    (11, '00000000-0000-0000-0000-000000002009', 3, '2025-08-26'),

    -- English Communication (class 4)
    (12, '00000000-0000-0000-0000-000000002010', 4, '2025-08-25'),
    (13, '00000000-0000-0000-0000-000000002011', 4, '2025-08-25'),
    (14, '00000000-0000-0000-0000-000000002012', 4, '2025-08-26'),
    (15, '00000000-0000-0000-0000-000000002002', 4, '2025-08-27'),

    -- Principles of Management (class 5)
    (16, '00000000-0000-0000-0000-000000002013', 5, '2025-08-25'),
    (17, '00000000-0000-0000-0000-000000002014', 5, '2025-08-25'),
    (18, '00000000-0000-0000-0000-000000002015', 5, '2025-08-26'),

    -- Linear Algebra (class 6)
    (19, '00000000-0000-0000-0000-000000002004', 6, '2025-08-25'),
    (20, '00000000-0000-0000-0000-000000002005', 6, '2025-08-26'),
    (21, '00000000-0000-0000-0000-000000002001', 6, '2025-08-27');


-- ===== Spring 2026 enrollments =====
INSERT INTO enrollments (enrollment_id, student_id, class_id, enrollment_date) VALUES
    -- Data Structures & Algorithms (class 7)
    (22, '00000000-0000-0000-0000-000000002001', 7, '2026-01-05'),
    (23, '00000000-0000-0000-0000-000000002002', 7, '2026-01-05'),
    (24, '00000000-0000-0000-0000-000000002003', 7, '2026-01-06'),

    -- Database Systems (class 8)
    (25, '00000000-0000-0000-0000-000000002001', 8, '2026-01-05'),
    (26, '00000000-0000-0000-0000-000000002002', 8, '2026-01-06'),
    (27, '00000000-0000-0000-0000-000000002003', 8, '2026-01-06'),

    -- Software Engineering (class 9)
    (28, '00000000-0000-0000-0000-000000002001', 9, '2026-01-05'),
    (29, '00000000-0000-0000-0000-000000002002', 9, '2026-01-05'),

    -- Calculus I - Spring (class 10)
    (30, '00000000-0000-0000-0000-000000002007', 10, '2026-01-05'),
    (31, '00000000-0000-0000-0000-000000002008', 10, '2026-01-06'),
    (32, '00000000-0000-0000-0000-000000002009', 10, '2026-01-06'),

    -- Web Development (class 11)
    (33, '00000000-0000-0000-0000-000000002001', 11, '2026-01-05'),
    (34, '00000000-0000-0000-0000-000000002003', 11, '2026-01-06'),

    -- English Communication - Spring (class 12)
    (35, '00000000-0000-0000-0000-000000002010', 12, '2026-01-05'),
    (36, '00000000-0000-0000-0000-000000002011', 12, '2026-01-05'),
    (37, '00000000-0000-0000-0000-000000002013', 12, '2026-01-06'),
    (38, '00000000-0000-0000-0000-000000002014', 12, '2026-01-06'),

    -- General Physics I - Spring (class 13)
    (39, '00000000-0000-0000-0000-000000002004', 13, '2026-01-05'),
    (40, '00000000-0000-0000-0000-000000002005', 13, '2026-01-06'),

    -- Principles of Management - Spring (class 14)
    (41, '00000000-0000-0000-0000-000000002013', 14, '2026-01-05'),
    (42, '00000000-0000-0000-0000-000000002014', 14, '2026-01-05'),
    (43, '00000000-0000-0000-0000-000000002015', 14, '2026-01-06'),

    -- Linear Algebra - Spring (class 15)
    (44, '00000000-0000-0000-0000-000000002006', 15, '2026-01-05'),
    (45, '00000000-0000-0000-0000-000000002009', 15, '2026-01-06');


-- ----------------------------------------------------------
-- 9. GRADES  (only for Fall 2025 - completed semester)
-- ----------------------------------------------------------
INSERT INTO grades (grade_id, enrollment_id, grade_value, feedback) VALUES
    -- Intro to Programming (class 1)
    (1,  1,  8.50, 'Good understanding of OOP concepts. Keep practicing design patterns.'),
    (2,  2,  9.20, 'Excellent work! Outstanding problem-solving skills.'),
    (3,  3,  7.00, 'Decent progress. Need to improve on recursion and data types.'),
    (4,  4,  6.50, 'Fair performance. Revise control flow and exception handling.'),

    -- Calculus I (class 2)
    (5,  5,  9.50, 'Outstanding performance in all topics. Excellent analytical skills.'),
    (6,  6,  8.00, 'Good grasp of derivatives. Needs more practice on integrals.'),
    (7,  7,  7.50, 'Steady improvement throughout the semester. Well done.'),
    (8,  8,  8.20, 'Strong in limits and continuity. Keep up the good work.'),

    -- General Physics I (class 3)
    (9,  9,  7.80, 'Good lab work. Theory needs strengthening.'),
    (10, 10, 8.80, 'Excellent understanding of mechanics and thermodynamics.'),
    (11, 11, 6.00, 'Needs significant improvement. Attend tutoring sessions.'),

    -- English Communication (class 4)
    (12, 12, 9.00, 'Fluent speaker with great presentation skills.'),
    (13, 13, 7.20, 'Good reading comprehension. Speaking needs more confidence.'),
    (14, 14, 8.50, 'Excellent writing skills. Active participation in class.'),
    (15, 15, 8.00, 'Good overall performance. Vocabulary is expanding well.'),

    -- Principles of Management (class 5)
    (16, 16, 7.50, 'Good case study analysis. Needs deeper critical thinking.'),
    (17, 17, 8.80, 'Outstanding group leader. Excellent strategic thinking.'),
    (18, 18, 7.00, 'Solid fundamentals. Participate more in discussions.'),

    -- Linear Algebra (class 6)
    (19, 19, 9.30, 'Exceptional mastery of eigenvalues and vector spaces.'),
    (20, 20, 8.50, 'Very good problem-solving. Minor errors in proofs.'),
    (21, 21, 7.80, 'Good work on matrices. Review linear transformations.');


-- ----------------------------------------------------------
-- 10. Reset sequences to avoid ID conflicts
-- ----------------------------------------------------------
SELECT setval('roles_role_id_seq',            (SELECT COALESCE(MAX(role_id), 0)       FROM roles));
SELECT setval('departments_department_id_seq',(SELECT COALESCE(MAX(department_id), 0) FROM departments));
SELECT setval('courses_course_id_seq',        (SELECT COALESCE(MAX(course_id), 0)     FROM courses));
SELECT setval('scheduled_classes_class_id_seq',(SELECT COALESCE(MAX(class_id), 0)     FROM scheduled_classes));
SELECT setval('enrollments_enrollment_id_seq',(SELECT COALESCE(MAX(enrollment_id), 0) FROM enrollments));
SELECT setval('grades_grade_id_seq',          (SELECT COALESCE(MAX(grade_id), 0)      FROM grades));
SELECT setval('semesters_semester_id_seq',    (SELECT COALESCE(MAX(semester_id), 0)   FROM semesters));
