package br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "identity_local_credentials")
public class LocalCredentialJpaEntity {

    private static final int MAX_HASH_LENGTH = 255;

    @Id
    @Column(
            name = "user_id",
            nullable = false,
            updatable = false
    )
    private UUID userId;

    @Column(
            name = "password_hash",
            nullable = false,
            length = MAX_HASH_LENGTH
    )
    private String passwordHash;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LocalCredentialJpaEntity() {
    }

    public LocalCredentialJpaEntity(
            UUID userId,
            String passwordHash,
            Instant createdAt
    ) {
        this.userId = Objects.requireNonNull(
                userId,
                "UserId nao pode ser nulo."
        );

        this.passwordHash = validateHash(passwordHash);

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Data de criacao nao pode ser nula."
        );

        this.updatedAt = createdAt;
    }

    public void replacePasswordHash(
            String newPasswordHash,
            Instant changedAt
    ) {
        String validatedHash = validateHash(newPasswordHash);

        Objects.requireNonNull(
                changedAt,
                "Data de atualizacao nao pode ser nula."
        );

        if (changedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException(
                    "Atualizacao anterior a ultima alteracao."
            );
        }

        this.passwordHash = validatedHash;
        this.updatedAt = changedAt;
    }

    private static String validateHash(String value) {
        Objects.requireNonNull(
                value,
                "Hash de senha nao pode ser nulo."
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Hash de senha nao pode ser vazio."
            );
        }

        if (value.length() > MAX_HASH_LENGTH) {
            throw new IllegalArgumentException(
                    "Hash excede o limite de 255 caracteres."
            );
        }

        return value;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "LocalCredentialJpaEntity[passwordHash=REDACTED]";
    }
}
