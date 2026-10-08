package br.com.fuelfinder.identity.domain.model;

import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
}