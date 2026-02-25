ALTER TABLE sent_notifications ADD COLUMN status VARCHAR(20) DEFAULT 'SENT';
ALTER TABLE sent_notifications ADD COLUMN scheduled_at TIMESTAMP;
ALTER TABLE sent_notifications ADD COLUMN sent_at TIMESTAMP;
ALTER TABLE sent_notifications ADD COLUMN target_role VARCHAR(50);
ALTER TABLE sent_notifications ADD COLUMN target_department_id BIGINT;
ALTER TABLE sent_notifications ADD COLUMN target_class_code VARCHAR(50);
ALTER TABLE sent_notifications ADD COLUMN target_recipient_id VARCHAR(255);

-- Update existing records
UPDATE sent_notifications SET sent_at = created_at, status = 'SENT' WHERE status IS NULL;
