-- FuelFinder - Identity
-- V1: estrutura inicial de usuarios, credenciais,
-- verificacao de email e outbox.
-- PostgreSQL

CREATE TABLE identity_users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) UNIQUE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_identity_users_name
        CHECK (
            char_length(btrim(name)) BETWEEN 2 AND 100
        ),

    CONSTRAINT ck_identity_users_status
        CHECK (
            status IN (
                'PENDING_VERIFICATION',
                'ACTIVE',
                'BLOCKED',
                'DELETED'
            )
        ),

    CONSTRAINT ck_identity_users_email
        CHECK (
            email IS NULL
            OR (
                email = lower(btrim(email))
                AND email ~
                    '^[a-z0-9._%+-]+@[a-z0-9.-]+[.][a-z]{2,}$'
            )
        ),

    CONSTRAINT ck_identity_users_deletion
        CHECK (
            (
                status = 'DELETED'
                AND email IS NULL
                AND name = 'Usuário excluído'
            )
            OR (
                status <> 'DELETED'
                AND email IS NOT NULL
            )
        ),

    CONSTRAINT ck_identity_users_timestamps
        CHECK (updated_at >= created_at)
);

CREATE TABLE identity_user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,

    PRIMARY KEY (user_id, role),

    CONSTRAINT fk_identity_roles_user
        FOREIGN KEY (user_id)
        REFERENCES identity_users(id),

    CONSTRAINT ck_identity_roles_value
        CHECK (
            role IN (
                'USER',
                'STATION_MANAGER',
                'ADMIN',
                'SUPER_ADMIN'
            )
        )
);

CREATE TABLE identity_local_credentials (
    user_id UUID PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_identity_credentials_user
        FOREIGN KEY (user_id)
        REFERENCES identity_users(id),

    CONSTRAINT ck_identity_credentials_hash
        CHECK (
            char_length(btrim(password_hash)) > 0
        ),

    CONSTRAINT ck_identity_credentials_timestamps
        CHECK (updated_at >= created_at)
);

CREATE TABLE identity_email_verification_tokens (
    digest VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,

    CONSTRAINT fk_identity_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES identity_users(id),

    CONSTRAINT ck_identity_tokens_digest
        CHECK (
            digest ~ '^[0-9a-f]{64}$'
        ),

    CONSTRAINT ck_identity_tokens_expiration
        CHECK (expires_at > issued_at),

    CONSTRAINT ck_identity_tokens_consumed
        CHECK (
            consumed_at IS NULL
            OR consumed_at >= issued_at
        )
);

CREATE INDEX idx_identity_tokens_pending_user
    ON identity_email_verification_tokens (
        user_id,
        expires_at
    )
    WHERE consumed_at IS NULL;

CREATE TABLE identity_verification_email_outbox (
    event_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    lease_id UUID,
    lease_expires_at TIMESTAMPTZ,
    processing_result VARCHAR(32),
    completed_at TIMESTAMPTZ,
    exhausted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_identity_outbox_user
        FOREIGN KEY (user_id)
        REFERENCES identity_users(id),

    CONSTRAINT ck_identity_outbox_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'COMPLETED',
                'EXHAUSTED'
            )
        ),

    CONSTRAINT ck_identity_outbox_attempts
        CHECK (attempt_count >= 0),

    CONSTRAINT ck_identity_outbox_lease
        CHECK (
            (
                status = 'PROCESSING'
                AND lease_id IS NOT NULL
                AND lease_expires_at IS NOT NULL
            )
            OR (
                status <> 'PROCESSING'
                AND lease_id IS NULL
                AND lease_expires_at IS NULL
            )
        ),

    CONSTRAINT ck_identity_outbox_completion
        CHECK (
            (
                status = 'COMPLETED'
                AND completed_at IS NOT NULL
                AND processing_result IN (
                    'DISPATCH_REQUESTED',
                    'NO_ACTION_REQUIRED'
                )
            )
            OR (
                status <> 'COMPLETED'
                AND completed_at IS NULL
                AND processing_result IS NULL
            )
        ),

    CONSTRAINT ck_identity_outbox_exhaustion
        CHECK (
            (
                status = 'EXHAUSTED'
                AND exhausted_at IS NOT NULL
            )
            OR (
                status <> 'EXHAUSTED'
                AND exhausted_at IS NULL
            )
        ),

    CONSTRAINT ck_identity_outbox_timestamps
        CHECK (updated_at >= created_at)
);

CREATE INDEX idx_identity_outbox_pending
    ON identity_verification_email_outbox (
        next_attempt_at,
        created_at
    )
    WHERE status = 'PENDING';

CREATE INDEX idx_identity_outbox_expired_leases
    ON identity_verification_email_outbox (
        lease_expires_at,
        created_at
    )
    WHERE status = 'PROCESSING';

CREATE INDEX idx_identity_outbox_user
    ON identity_verification_email_outbox (user_id);
