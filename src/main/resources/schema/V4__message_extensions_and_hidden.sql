-- V4 — Message extensions and MessageHidden table
-- Add edit and delete tracking to messages
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS is_edited boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS edited_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS edited_by bigint,
    ADD COLUMN IF NOT EXISTS deleted_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS deleted_by bigint;

-- Add foreign keys for edited_by and deleted_by
ALTER TABLE messages
    ADD CONSTRAINT fk_messages_edited_by
    FOREIGN KEY (edited_by) REFERENCES users (id);

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_deleted_by
    FOREIGN KEY (deleted_by) REFERENCES users (id);

-- Create message_hidden table
CREATE TABLE IF NOT EXISTS message_hidden (
    id bigserial PRIMARY KEY,
    message_id bigint NOT NULL,
    user_id bigint NOT NULL,
    hidden_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_message_hidden UNIQUE (message_id, user_id),
    CONSTRAINT fk_message_hidden_message FOREIGN KEY (message_id) REFERENCES messages (id),
    CONSTRAINT fk_message_hidden_user FOREIGN KEY (user_id) REFERENCES users (id)
);
