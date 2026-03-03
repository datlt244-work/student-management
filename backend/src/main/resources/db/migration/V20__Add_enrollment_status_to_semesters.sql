-- =============================================
-- V20: Add enrollment status tracking to semesters
-- =============================================
-- Cho phép Admin "Publish" semester trước khi mở đăng ký tín chỉ.
-- Trạng thái: DRAFT (chưa publish) → PUBLISHED (đã sync Redis, sẵn sàng đăng ký) → CLOSED (đã đóng đăng ký)

ALTER TABLE semesters ADD COLUMN IF NOT EXISTS
    enrollment_status VARCHAR(20) DEFAULT 'DRAFT' NOT NULL;

ALTER TABLE semesters ADD COLUMN IF NOT EXISTS
    published_at TIMESTAMP;
