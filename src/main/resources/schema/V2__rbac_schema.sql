-- ============================================================
-- V2 — Role-Based Access Control (RBAC) schema
-- Table names are lowercase to match Hibernate 7 PhysicalNamingStrategyStandardImpl.
-- Column names use quoted camelCase to preserve mixed-case (consistent with V1).
-- Run this script against your PostgreSQL database BEFORE
-- starting the application with ddl-auto=validate.
-- ============================================================

-- ── permission ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS permission (
    id          BIGSERIAL    PRIMARY KEY,
    "name"      VARCHAR(50)  NOT NULL UNIQUE,
    "description" VARCHAR(255)
);

-- ── role ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS role (
    id          BIGSERIAL    PRIMARY KEY,
    "name"      VARCHAR(30)  NOT NULL UNIQUE,
    "description" VARCHAR(255)
);

-- ── role_permission (join table) ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS role_permission (
    "roleId"       BIGINT NOT NULL REFERENCES role(id)       ON DELETE CASCADE,
    "permissionId" BIGINT NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    PRIMARY KEY ("roleId", "permissionId")
);

-- ── user_role (join table) ────────────────────────────────────────────────────
-- Note: "User" table already exists from V1.
CREATE TABLE IF NOT EXISTS user_role (
    "userId" BIGINT NOT NULL REFERENCES "User"(id) ON DELETE CASCADE,
    "roleId" BIGINT NOT NULL REFERENCES role(id)   ON DELETE CASCADE,
    PRIMARY KEY ("userId", "roleId")
);

-- ── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_user_role_user_id ON user_role ("userId");
CREATE INDEX IF NOT EXISTS idx_user_role_role_id ON user_role ("roleId");
CREATE INDEX IF NOT EXISTS idx_role_perm_role_id ON role_permission ("roleId");
CREATE INDEX IF NOT EXISTS idx_role_perm_perm_id ON role_permission ("permissionId");
