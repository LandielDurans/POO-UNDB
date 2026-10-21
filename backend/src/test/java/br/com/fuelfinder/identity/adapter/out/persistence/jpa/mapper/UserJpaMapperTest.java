package br.com.fuelfinder.identity.adapter.out.persistence.jpa.mapper;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.UserJpaEntity;
import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserJpaMapperTest {

    private static final Instant CREATED_AT =
            Instant.parse("2026-10-09T12:00:00Z");

    private static final Instant UPDATED_AT =
            CREATED_AT.plusSeconds(3600);

    private final UserJpaMapper mapper =
            new UserJpaMapper();

    @Test
    void shouldMapPendingUserToJpaEntity() {
        User user = pendingUser();

        UserJpaEntity entity = mapper.toEntity(user);

        assertEquals(user.getId().value(), entity.getId());
        assertEquals(user.getName(), entity.getName());
        assertEquals(
                "maria@example.com",
                entity.getEmail()
        );

        assertEquals(
                UserStatus.PENDING_VERIFICATION,
                entity.getStatus()
        );

        assertEquals(Set.of(UserRole.USER), entity.getRoles());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(CREATED_AT, entity.getUpdatedAt());
    }

    @Test
    void shouldRestorePendingUserFromJpaEntity() {
        User original = pendingUser();

        User restored = mapper.toDomain(
                mapper.toEntity(original)
        );

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getEmail(), restored.getEmail());
        assertEquals(original.getRoles(), restored.getRoles());
        assertEquals(original.getStatus(), restored.getStatus());
        assertEquals(
                original.getCreatedAt(),
                restored.getCreatedAt()
        );
    }

    @Test
    void shouldPreserveMultipleRoles() {
        User original = User.reconstitute(
                UserId.newId(),
                "Maria Silva",
                Email.of("maria@example.com"),
                Set.of(UserRole.USER, UserRole.ADMIN),
                UserStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT
        );

        User restored = mapper.toDomain(
                mapper.toEntity(original)
        );

        assertEquals(
                Set.of(UserRole.USER, UserRole.ADMIN),
                restored.getRoles()
        );

        assertEquals(UserStatus.ACTIVE, restored.getStatus());
    }

    @Test
    void shouldPreserveDeletedUserAnonymization() {
        User original = pendingUser();

        original.delete(UPDATED_AT);

        UserJpaEntity entity = mapper.toEntity(original);

        assertNull(entity.getEmail());
        assertEquals(UserStatus.DELETED, entity.getStatus());

        User restored = mapper.toDomain(entity);

        assertTrue(restored.getEmail().isEmpty());
        assertEquals(UserStatus.DELETED, restored.getStatus());
        assertEquals("Usuário excluído", restored.getName());
        assertEquals(Set.of(UserRole.USER), restored.getRoles());
    }

    @Test
    void shouldRejectMissingUserRole() {
        UserJpaEntity entity = new UserJpaEntity(
                UserId.newId().value(),
                "Maria Silva",
                "maria@example.com",
                UserStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT,
                Set.of(UserRole.ADMIN)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toDomain(entity)
        );
    }

    @Test
    void shouldRejectMissingEmailForActiveUser() {
        UserJpaEntity entity = new UserJpaEntity(
                UserId.newId().value(),
                "Maria Silva",
                null,
                UserStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT,
                Set.of(UserRole.USER)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toDomain(entity)
        );
    }

    @Test
    void shouldPreventExternalModificationOfRoles() {
        UserJpaEntity entity = mapper.toEntity(pendingUser());

        assertThrows(
                UnsupportedOperationException.class,
                () -> entity.getRoles().clear()
        );
    }

    private User pendingUser() {
        return User.registerLocal(
                UserId.newId(),
                "Maria Silva",
                Email.of("maria@example.com"),
                CREATED_AT
        );
    }
}