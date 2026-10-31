-- FuelFinder - Identity
-- V2: sessoes de autenticacao (refresh tokens).
-- PostgreSQL
--
-- Cada linha e uma sessao de um dispositivo/navegador.
-- O refresh token nunca e armazenado: apenas seu
-- SHA-256 hexadecimal, trocado a cada renovacao.

CREATE TABLE identity_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    refresh_token_digest VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_refreshed_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT fk_identity_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES identity_users(id),

    CONSTRAINT uk_identity_sessions_digest
        UNIQUE (refresh_token_digest),

    CONSTRAINT ck_identity_sessions_digest
        CHECK (
            refresh_token_digest ~ '^[0-9a-f]{64}$'
        ),

    CONSTRAINT ck_identity_sessions_refresh
        CHECK (last_refreshed_at >= created_at),

    CONSTRAINT ck_identity_sessions_expiration
        CHECK (expires_at > last_refreshed_at),

    CONSTRAINT ck_identity_sessions_revocation
        CHECK (
            revoked_at IS NULL
            OR revoked_at >= created_at
        )
);

CREATE INDEX idx_identity_sessions_active_user
    ON identity_sessions (user_id)
    WHERE revoked_at IS NULL;
