-- V1.1 — RefreshToken table (missing from original schema)
CREATE TABLE IF NOT EXISTS refreshtoken (
    id                    BIGSERIAL    PRIMARY KEY,
    "version"             BIGINT       NOT NULL DEFAULT 0,
    "userId"              BIGINT       NOT NULL REFERENCES "User"(id) ON DELETE CASCADE,
    "tokenHash"           VARCHAR(255) NOT NULL UNIQUE,
    "expiresAt"           TIMESTAMPTZ  NOT NULL,
    "revokedAt"           TIMESTAMPTZ,
    "replacedByTokenHash" VARCHAR(255),
    "createdByIp"         VARCHAR(45),
    "userAgent"           VARCHAR(255),
    "createdAt"           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id    ON refreshtoken ("userId");
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON refreshtoken ("expiresAt");
