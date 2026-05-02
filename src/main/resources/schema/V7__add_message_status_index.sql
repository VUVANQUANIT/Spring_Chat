-- V7 — Add index for fast retrieval of unread messages by user
CREATE INDEX idx_message_statuses_user_status ON message_statuses (user_id, status);
