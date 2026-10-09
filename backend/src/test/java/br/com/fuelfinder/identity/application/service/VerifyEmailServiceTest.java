
package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.exception.InvalidEmailVerificationTokenException;
import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailCommand;
import br.com.fuelfinder.identity.application.port.out.EmailVerificationPersistence;
import br.com.fuelfinder.identity.application.port.out.VerificationTokenDigester;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class VerifyEmailServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-10-09T12:00:00Z");

    private static final String RAW_TOKEN =
            "opaque-token-for-testing";

    private final FakeTokenDigester digester =
            new FakeTokenDigester();

    private final FakeVerificationPersistence persistence =
            new FakeVerificationPersistence();

    private VerifyEmailService service() {
        return new VerifyEmailService(
                digester,
                persistence,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldVerifyEmailSuccessfully() {
        service().execute(
                new VerifyEmailCommand(RAW_TOKEN)
        );

        assertEquals(RAW_TOKEN, digester.receivedToken);
        assertEquals(1, persistence.calls);
        assertEquals(
                new VerificationTokenDigest("a".repeat(64)),
                persistence.receivedDigest
        );
        assertEquals(NOW, persistence.receivedTime);
    }

    @Test
    void shouldRejectInvalidExpiredOrConsumedToken() {
        persistence.success = false;

        assertThrows(
                InvalidEmailVerificationTokenException.class,
                () -> service().execute(
                        new VerifyEmailCommand(RAW_TOKEN)
                )
        );

        assertEquals(1, persistence.calls);
    }

    @Test
    void shouldNotPersistWhenDigestingFails() {
        digester.fail = true;

        assertThrows(
                IllegalStateException.class,
                () -> service().execute(
                        new VerifyEmailCommand(RAW_TOKEN)
                )
        );

        assertEquals(0, persistence.calls);
    }

    @Test
    void shouldRejectNullDigest() {
        digester.returnNull = true;

        assertThrows(
                NullPointerException.class,
                () -> service().execute(
                        new VerifyEmailCommand(RAW_TOKEN)
                )
        );

        assertEquals(0, persistence.calls);
    }

    @Test
    void shouldPropagatePersistenceFailure() {
        persistence.fail = true;

        assertThrows(
                IllegalStateException.class,
                () -> service().execute(
                        new VerifyEmailCommand(RAW_TOKEN)
                )
        );

        assertEquals(1, persistence.calls);
    }

    @Test
    void shouldRejectNullCommand() {
        assertThrows(
                NullPointerException.class,
                () -> service().execute(null)
        );

        assertEquals(0, persistence.calls);
        assertNull(digester.receivedToken);
    }

    private static final class FakeTokenDigester
            implements VerificationTokenDigester {

        String receivedToken;
        boolean fail;
        boolean returnNull;

        @Override
        public VerificationTokenDigest digest(String rawToken) {
            receivedToken = rawToken;

            if (fail) {
                throw new IllegalStateException(
                        "Falha ao calcular digest."
                );
            }

            if (returnNull) {
                return null;
            }

            return new VerificationTokenDigest(
                    "a".repeat(64)
            );
        }
    }

    private static final class FakeVerificationPersistence
            implements EmailVerificationPersistence {

        int calls;
        boolean success = true;
        boolean fail;

        VerificationTokenDigest receivedDigest;
        Instant receivedTime;

        @Override
        public boolean consumeAndActivate(
                VerificationTokenDigest digest,
                Instant verifiedAt
        ) {
            calls++;

            receivedDigest = digest;
            receivedTime = verifiedAt;

            if (fail) {
                throw new IllegalStateException(
                        "Falha de persistencia."
                );
            }

            return success;
        }
    }
}
