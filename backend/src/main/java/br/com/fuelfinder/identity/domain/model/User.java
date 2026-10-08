package br.com.fuelfinder.identity.domain.model;

import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;

public final class User {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 100;
    private static final String DELETED_USER_NAME = "Usuário excluído";

    private final UserId id;
    private Email email;
    private Set<UserRole> roles;
    private final Instant createdAt;

    private String name;
    private UserStatus status;
    private Instant updatedAt;

    private User(
            UserId id,
            String name,
            Email email,
            Set<UserRole> roles,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Id nao pode ser nulo.");
        this.name = validateName(name);
        this.status = Objects.requireNonNull(status, "Status nao pode ser nulo.");

        this.createdAt = Objects.requireNonNull(
                createdAt, "Data de criacao nao pode ser nula."
        );

        this.updatedAt = Objects.requireNonNull(
                updatedAt, "Data de atualizacao nao pode ser nula."
        );

        Set<UserRole> validatedRoles = Set.copyOf(
                Objects.requireNonNull(roles, "Roles nao podem ser nulas.")
        );

        if (!validatedRoles.contains(UserRole.USER)) {
            throw new IllegalArgumentException(
                    "Usuario deve possuir a role USER."
            );
        }

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "Atualizacao nao pode ser anterior a criacao."
            );
        }

        if (status == UserStatus.DELETED) {
            if (!DELETED_USER_NAME.equals(this.name)
                    || email != null
                    || !validatedRoles.equals(Set.of(UserRole.USER))) {
                throw new IllegalArgumentException(
                        "Usuario excluido deve estar anonimizado e sem privilegios"
                );
            }
        } else if (email == null) {
            throw new IllegalArgumentException(
                    "Usuario não excluido deve possuir email"
            );
        }

        this.email = email;

        this.roles = validatedRoles;
    }

    public static User registerLocal(
            UserId id,
            String name,
            Email email,
            Instant registeredAt
    ) {
        return new User(
                id,
                name,
                email,
                Set.of(UserRole.USER),
                UserStatus.PENDING_VERIFICATION,
                registeredAt,
                registeredAt
        );
    }

    public static User reconstitute(
            UserId id,
            String name,
            Email email,
            Set<UserRole> roles,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new User(
                id,
                name,
                email,
                roles,
                status,
                createdAt,
                updatedAt
        );
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

    public void delete(Instant deletedAt) {
        if (status == UserStatus.DELETED) {
            throw new IllegalStateException(
                    "Usuario ja foi excluido."
            );
        }

        if (!roles.equals(Set.of(UserRole.USER))) {
            throw new IllegalStateException(
                    "Usuario deve ter apenas a role USER antes da exclusao."
            );
        }

        validateChangeTime(deletedAt);

        this.name = DELETED_USER_NAME;
        this.email = null;
        this.roles = Set.of(UserRole.USER);
        this.status = UserStatus.DELETED;
        this.updatedAt = deletedAt;
    }

    public void grantAdminRole(User actor, Instant changedAt) {
        requireActiveSuperAdmin(actor);
        requireStatus(UserStatus.ACTIVE);
        requireNotSuperAdmin();
        validateChangeTime(changedAt);

        if (roles.contains(UserRole.ADMIN)) {
            throw new IllegalStateException(
                    "Usuario ja possui a role ADMIN."
            );
        }

        Set<UserRole> updatedRoles = new HashSet<>(roles);
        updatedRoles.add(UserRole.ADMIN);

        this.roles = Set.copyOf(updatedRoles);
        this.updatedAt = changedAt;
    }

    public void revokeAdminRole(User actor, Instant changedAt) {
        requireActiveSuperAdmin(actor);
        requireStatus(UserStatus.ACTIVE);
        requireNotSuperAdmin();
        validateChangeTime(changedAt);

        if (!roles.contains(UserRole.ADMIN)) {
            throw new IllegalStateException(
                    "Usuario nao possui a role ADMIN."
            );
        }

        Set<UserRole> updatedRoles = new HashSet<>(roles);
        updatedRoles.remove(UserRole.ADMIN);

        this.roles = Set.copyOf(updatedRoles);
        this.updatedAt = changedAt;
    }

    private static void requireActiveSuperAdmin(User actor) {
        Objects.requireNonNull(actor, "Ator nao pode ser nulo.");

        if (actor.status != UserStatus.ACTIVE ||
                !actor.roles.contains(UserRole.SUPER_ADMIN)) {
            throw new IllegalStateException(
                    "Operacao permitida apenas para SUPER_ADMIN ativo."
            );
        }
    }

    private void requireNotSuperAdmin() {
        if (roles.contains(UserRole.SUPER_ADMIN)) {
            throw new IllegalStateException(
                    "Nao e permitido alterar privilegios de SUPER_ADMIN."
            );
        }
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

    public Optional<Email> getEmail() {
        return Optional.ofNullable(email);
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