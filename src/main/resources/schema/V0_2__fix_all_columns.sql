-- V0.2 — Fix all table column names to match entity quoted camelCase annotations.
-- All tables were originally created with PostgreSQL default lowercase column names.

-- ── conversation ──────────────────────────────────────────────────────────────
ALTER TABLE conversation RENAME COLUMN createdat TO "createdAt";

-- ── conversationparticipant ───────────────────────────────────────────────────
ALTER TABLE conversationparticipant RENAME COLUMN conversationid     TO "conversationId";
ALTER TABLE conversationparticipant RENAME COLUMN userid             TO "userId";
ALTER TABLE conversationparticipant RENAME COLUMN joinedat           TO "joinedAt";
ALTER TABLE conversationparticipant RENAME COLUMN leftat             TO "leftAt";
ALTER TABLE conversationparticipant RENAME COLUMN lastreadmessageid  TO "lastReadMessageId";

-- ── friendship ────────────────────────────────────────────────────────────────
ALTER TABLE friendship RENAME COLUMN requesterid  TO "requesterId";
ALTER TABLE friendship RENAME COLUMN addresseeid  TO "addresseeId";
ALTER TABLE friendship RENAME COLUMN createdat    TO "createdAt";
ALTER TABLE friendship RENAME COLUMN updatedat    TO "updatedAt";

-- ── message ───────────────────────────────────────────────────────────────────
ALTER TABLE message RENAME COLUMN conversationid TO "conversationId";
ALTER TABLE message RENAME COLUMN senderid       TO "senderId";
ALTER TABLE message RENAME COLUMN replytoid      TO "replyToId";
ALTER TABLE message RENAME COLUMN isdeleted      TO "isDeleted";
ALTER TABLE message RENAME COLUMN createdat      TO "createdAt";

-- ── messagestatus ─────────────────────────────────────────────────────────────
ALTER TABLE messagestatus RENAME COLUMN messageid TO "messageId";
ALTER TABLE messagestatus RENAME COLUMN userid    TO "userId";
ALTER TABLE messagestatus RENAME COLUMN updatedat TO "updatedAt";
