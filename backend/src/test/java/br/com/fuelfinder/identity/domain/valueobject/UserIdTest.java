package br.com.fuelfinder.identity.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserIdTest {

    @Test
    void shouldGenerateNewUserId() {
        UserId userId = UserId.newId();

        assertNotNull(userId);
        assertNotNull(userId.value());
    }

    @Test
    void shouldCreateUserIdFromExistingUuid() {
        UUID uuid = UUID.randomUUID();

        UserId userId = UserId.from(uuid);

        assertEquals(uuid, userId.value());
    }

    @Test
    void shouldRejectNullUuid() {
        assertThrows(
                NullPointerException.class,
                () -> UserId.from(null)
        );
    }

    @Test
    void shouldRepresentIdAsString() {
        UUID uuid = UUID.randomUUID();
        UserId userId = UserId.from(uuid);

        assertEquals(uuid.toString(), userId.toString());
    }
}