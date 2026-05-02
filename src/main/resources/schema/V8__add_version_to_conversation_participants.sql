-- V8 — Add version for Optimistic Locking in conversation_participants
ALTER TABLE conversation_participants ADD COLUMN version BIGINT DEFAULT 0;
