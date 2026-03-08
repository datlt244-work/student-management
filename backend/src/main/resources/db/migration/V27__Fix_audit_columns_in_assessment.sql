ALTER TABLE assessment_items 
ADD COLUMN created_by VARCHAR(255),
ADD COLUMN updated_by VARCHAR(255);

ALTER TABLE student_scores 
ADD COLUMN created_by VARCHAR(255),
ADD COLUMN updated_by VARCHAR(255);
