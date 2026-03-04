-- V0 — Fix User table column names to match entity quoted camelCase annotations.
-- The table was originally created with PostgreSQL's default lowercase column names,
-- but the JPA entity uses double-quoted camelCase (e.g. "passwordHash", "createdAt").

ALTER TABLE "User" RENAME COLUMN passwordhash    TO "passwordHash";
ALTER TABLE "User" RENAME COLUMN fullname        TO "fullName";
ALTER TABLE "User" RENAME COLUMN avatarurl       TO "avatarUrl";
ALTER TABLE "User" RENAME COLUMN lastseen        TO "lastSeen";
ALTER TABLE "User" RENAME COLUMN createdat       TO "createdAt";
ALTER TABLE "User" RENAME COLUMN updatedat       TO "updatedAt";
