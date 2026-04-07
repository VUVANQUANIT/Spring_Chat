ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS title varchar(100),
    ADD COLUMN IF NOT EXISTS avatar_url varchar(500),
    ADD COLUMN IF NOT EXISTS owner_id bigint;

ALTER TABLE conversations
    ADD CONSTRAINT fk_conversations_owner
    FOREIGN KEY (owner_id) REFERENCES users (id);
