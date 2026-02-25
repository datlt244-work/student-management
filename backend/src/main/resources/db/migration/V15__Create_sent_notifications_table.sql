CREATE TABLE sent_notifications (
    sent_id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    action_url TEXT,
    notification_type VARCHAR(50),
    recipient_count INTEGER,
    target_group VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_sent_notifications_created_at ON sent_notifications(created_at);
