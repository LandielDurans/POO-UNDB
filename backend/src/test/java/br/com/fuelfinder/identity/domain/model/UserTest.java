package br.com.fuelfinder.identity.domain.model;

import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private static final Instant NOW =
            Instant.parse("2026-10-08T12:00:00Z");

    private User createUser() {
        return User.registerLocal(
                UserId.newId(),
                "Maria Silva",
                Email.of("maria@example.com"),
                NOW
        );
    }

    private User createSuperAdmin() {
        return User.reconstitute(
                UserId.newId(),
                "Root Admin",
                Email.of("root@example.com"),
                Set.of(UserRole.USER, UserRole.SUPER_ADMIN),
                UserStatus.ACTIVE,
                NOW.minus(1, ChronoUnit.DAYS),
                NOW
        );
    }

    @Test
    void shouldRegisterUserPendingVerification() {
        User user = createUser();

        assertEquals("Maria Silva", user.getName());
        assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains(UserRole.USER));
        assertEquals(NOW, user.getCreatedAt());
    }

    @Test
    void shouldActivateUserAfterEmailVerification() {
        User user = createUser();
        Instant verifiedAt = NOW.plus(1, ChronoUnit.MINUTES);

        user.verifyEmail(verifiedAt);

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(verifiedAt, user.getUpdatedAt());
    }

    @Test
    void shouldNotVerifyEmailTwice() {
        User user = createUser();

        user.verifyEmail(NOW.plus(1, ChronoUnit.MINUTES));

        assertThrows(
                IllegalStateException.class,
                () -> user.verifyEmail(
                        NOW.plus(2, ChronoUnit.MINUTES)
                )
        );
    }

    @Test
    void shouldChangeNameOfActiveUser() {
        User user = createUser();
        user.verifyEmail(NOW);

        user.changeName("  Maria Oliveira  ", NOW);

        assertEquals("Maria Oliveira", user.getName());
    }

    @Test
    void shouldNotChangeNameOfPendingUser() {
        User user = createUser();

        assertThrows(
                IllegalStateException.class,
                () -> user.changeName("Novo Nome", NOW)
        );
    }

    @Test
    void shouldBlockAndUnblockActiveUser() {
        User user = createUser();
        user.verifyEmail(NOW);

        user.block(NOW);
        assertEquals(UserStatus.BLOCKED, user.getStatus());

        user.unblock(NOW);
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void shouldRejectInvalidName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> User.registerLocal(
                        UserId.newId(),
                        " ",
                        Email.of("maria@example.com"),
                        NOW
                )
        );
    }

    @Test
    void shouldRejectChangeWithPastTimestamp() {
        User user = createUser();
        user.verifyEmail(NOW);

        assertThrows(
                IllegalArgumentException.class,
                () -> user.changeName(
                        "Maria Oliveira",
                        NOW.minus(1, ChronoUnit.SECONDS)
                )
        );

        assertEquals("Maria Silva", user.getName());
    }

    @Test
    void shouldReconstituteExistingActiveUser() {
        UserId id = UserId.newId();
        Instant createdAt = NOW.minus(2, ChronoUnit.DAYS);

        User user = User.reconstitute(
                id,
                "Maria Silva",
                Email.of("maria@example.com"),
                Set.of(UserRole.USER, UserRole.ADMIN),
                UserStatus.ACTIVE,
                createdAt,
                NOW
        );

        assertEquals(id, user.getId());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.getRoles().contains(UserRole.ADMIN));
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(NOW, user.getUpdatedAt());
    }

    @Test
    void shouldRejectUserWithoutBaseRole() {
        assertThrows(
                IllegalArgumentException.class,
                () -> User.reconstitute(
                        UserId.newId(),
                        "Maria Silva",
                        Email.of("maria@example.com"),
                        Set.of(UserRole.ADMIN),
                        UserStatus.ACTIVE,
                        NOW,
                        NOW
                )
        );
    }

    @Test
    void shouldRejectInconsistentTimestamps() {
        assertThrows(
                IllegalArgumentException.class,
                () -> User.reconstitute(
                        UserId.newId(),
                        "Maria Silva",
                        Email.of("maria@example.com"),
                        Set.of(UserRole.USER),
                        UserStatus.ACTIVE,
                        NOW,
                        NOW.minus(1, ChronoUnit.DAYS)
                )
        );
    }

    @Test
    void shouldNotAllowExternalRoleModification() {
        User user = createUser();

        assertThrows(
                UnsupportedOperationException.class,
                () -> user.getRoles().add(UserRole.ADMIN)
        );

        assertEquals(Set.of(UserRole.USER), user.getRoles());
    }

    @Test
    void shouldAllowSuperAdminToGrantAdminRole() {
        User actor = createSuperAdmin();
        User target = createUser();
        target.verifyEmail(NOW);

        target.grantAdminRole(
                actor,
                NOW.plus(1, ChronoUnit.MINUTES)
        );

        assertTrue(target.getRoles().contains(UserRole.ADMIN));
        assertTrue(target.getRoles().contains(UserRole.USER));
    }

    @Test
    void shouldAllowSuperAdminToRevokeAdminRole() {
        User actor = createSuperAdmin();
        User target = createUser();
        target.verifyEmail(NOW);

        target.grantAdminRole(actor, NOW);

        target.revokeAdminRole(
                actor,
                NOW.plus(1, ChronoUnit.MINUTES)
        );

        assertFalse(target.getRoles().contains(UserRole.ADMIN));
        assertTrue(target.getRoles().contains(UserRole.USER));
    }

    @Test
    void shouldRejectRoleGrantByRegularUser() {
        User actor = createUser();
        actor.verifyEmail(NOW);

        User target = createUser();
        target.verifyEmail(NOW);

        assertThrows(
                IllegalStateException.class,
                () -> target.grantAdminRole(actor, NOW)
        );

        assertFalse(target.getRoles().contains(UserRole.ADMIN));
    }

    @Test
    void shouldRejectRoleGrantByBlockedSuperAdmin() {
        User actor = createSuperAdmin();
        actor.block(NOW);

        User target = createUser();
        target.verifyEmail(NOW);

        assertThrows(
                IllegalStateException.class,
                () -> target.grantAdminRole(actor, NOW)
        );
    }

    @Test
    void shouldRejectDuplicateAdminRole() {
        User actor = createSuperAdmin();
        User target = createUser();
        target.verifyEmail(NOW);

        target.grantAdminRole(actor, NOW);

        assertThrows(
                IllegalStateException.class,
                () -> target.grantAdminRole(
                        actor,
                        NOW.plus(1, ChronoUnit.MINUTES)
                )
        );
    }

    @Test
    void shouldDeleteAndAnonymizeActiveUser() {
        User user = createUser();
        user.verifyEmail(NOW);

        Instant deletedAt = NOW.plus(1, ChronoUnit.MINUTES);

        user.delete(deletedAt);

        assertEquals(UserStatus.DELETED, user.getStatus());
        assertEquals("Usuário excluído", user.getName());
        assertTrue(user.getEmail().isEmpty());
        assertEquals(Set.of(UserRole.USER), user.getRoles());
        assertEquals(deletedAt, user.getUpdatedAt());
    }

    @Test
    void shouldRejectDeletingUserTwice() {
        User user = createUser();
        user.verifyEmail(NOW);
        user.delete(NOW);

        assertThrows(
                IllegalStateException.class,
                () -> user.delete(NOW.plus(1, ChronoUnit.MINUTES))
        );
    }

    @Test
    void shouldNotAllowDeletedUserToChangeName() {
        User user = createUser();
        user.verifyEmail(NOW);
        user.delete(NOW);

        assertThrows(
                IllegalStateException.class,
                () -> user.changeName(
                        "Novo Nome",
                        NOW.plus(1, ChronoUnit.MINUTES)
                )
        );
    }

    @Test
    void shouldRejectDeletionOfPrivilegedUser() {
        User user = createSuperAdmin();

        assertThrows(
                IllegalStateException.class,
                () -> user.delete(NOW.plus(1, ChronoUnit.MINUTES))
        );

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.getEmail().isPresent());
    }

    @Test
    void shouldReconstituteAnonymizedUser() {
        User user = User.reconstitute(
                UserId.newId(),
                "Usuário excluído",
                null,
                Set.of(UserRole.USER),
                UserStatus.DELETED,
                NOW.minus(1, ChronoUnit.DAYS),
                NOW
        );

        assertEquals(UserStatus.DELETED, user.getStatus());
        assertTrue(user.getEmail().isEmpty());
    }

    @Test
    void shouldRejectDeletedUserWithPersonalEmail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> User.reconstitute(
                        UserId.newId(),
                        "Usuário excluído",
                        Email.of("maria@example.com"),
                        Set.of(UserRole.USER),
                        UserStatus.DELETED,
                        NOW,
                        NOW
                )
        );
    }

    @Test
    void shouldRejectActiveUserWithoutEmail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> User.reconstitute(
                        UserId.newId(),
                        "Maria Silva",
                        null,
                        Set.of(UserRole.USER),
                        UserStatus.ACTIVE,
                        NOW,
                        NOW
                )
        );
    }
}