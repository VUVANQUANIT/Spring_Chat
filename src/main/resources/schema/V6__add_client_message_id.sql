-- V6 — Add client_message_id for idempotent send-message retries
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS client_message_id varchar(100);

-- Supports fast idempotency lookup in 30-second retry window
CREATE INDEX IF NOT EXISTS idx_messages_dedupe_window
    ON messages (conversation_id, sender_id, client_message_id, created_at DESC);
