package br.com.fuelfinder.identity.domain.model;

import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public final class User {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 100;

    private final UserId id;
    private final Email email;
    private final Set<UserRole> roles;
    private final Instant createdAt;

    private String name;
    private UserStatus status;
    private Instant updatedAt;

    private User(
            UserId id,
            String name,
            Email email,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.createdAt = Objects.requireNonNull(createdAt);

        this.name = validateName(name);
        this.roles = Set.of(UserRole.USER);
        this.status = UserStatus.PENDING_VERIFICATION;
        this.updatedAt = createdAt;
    }

    public static User registerLocal(
            UserId id,
            String name,
            Email email,
            Instant registeredAt
    ) {
        return new User(id, name, email, registeredAt);
    }

    public void verifyEmail(Instant verifiedAt) {
        requireStatus(UserStatus.PENDING_VERIFICATION);
        validateChangeTime(verifiedAt);

        this.status = UserStatus.ACTIVE;
        this.updatedAt = verifiedAt;
    }

    public void changeName(String newName, Instant changedAt) {
        requireStatus(UserStatus.ACTIVE);

        String validatedName = validateName(newName);
        validateChangeTime(changedAt);

        this.name = validatedName;
        this.updatedAt = changedAt;
    }

    public void block(Instant blockedAt) {
        requireStatus(UserStatus.ACTIVE);
        validateChangeTime(blockedAt);

        this.status = UserStatus.BLOCKED;
        this.updatedAt = blockedAt;
    }

    public void unblock(Instant unblockedAt) {
        requireStatus(UserStatus.BLOCKED);
        validateChangeTime(unblockedAt);

        this.status = UserStatus.ACTIVE;
        this.updatedAt = unblockedAt;
    }

    private static String validateName(String name) {
        Objects.requireNonNull(name, "Nome nao pode ser nulo.");

        String normalized = name.trim();

        if (normalized.length() < MIN_NAME_LENGTH ||
                normalized.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Nome deve conter entre 2 e 100 caracteres."
            );
        }

        return normalized;
    }

    private void requireStatus(UserStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Operacao nao permitida para o status: " + status
            );
        }
    }

    private void validateChangeTime(Instant changedAt) {
        Objects.requireNonNull(
                changedAt,
                "Data da alteracao nao pode ser nula."
        );

        if (changedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException(
                    "Data da alteracao nao pode ser anterior a ultima atualizacao."
            );
        }
    }

    public UserId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public Set<UserRole> getRoles() {
        return roles;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}