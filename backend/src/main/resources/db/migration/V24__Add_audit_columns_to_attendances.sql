-- V24__Add_audit_columns_to_attendances.sql
ALTER TABLE attendances
ADD COLUMN created_by VARCHAR(255),
ADD COLUMN updated_by VARCHAR(255),
ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
