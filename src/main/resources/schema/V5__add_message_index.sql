-- V5 — Add index for message retrieval by conversation
CREATE INDEX idx_messages_conv_created_id ON messages (conversation_id, created_at DESC, id DESC);
