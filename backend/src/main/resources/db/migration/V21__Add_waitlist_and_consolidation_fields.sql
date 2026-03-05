-- Thêm cột min_students vào scheduled_classes
ALTER TABLE scheduled_classes ADD COLUMN min_students INT DEFAULT 30;

-- Thêm cột status vào enrollments, với default là ENROLLED để tương thích với dữ liệu hiện tại
ALTER TABLE enrollments ADD COLUMN status VARCHAR(20) DEFAULT 'ENROLLED' NOT NULL;
