
package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.model.PreparedVerificationEmail;
import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.out.EmailVerificationTokenIssuer;
import br.com.fuelfinder.identity.application.port.out.UserRepository;
import br.com.fuelfinder.identity.application.port.out.VerificationTokenDigester;
import br.com.fuelfinder.identity.application.port.out.VerificationTokenGenerator;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PrepareEmailVerificationServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-10-09T12:00:00Z");

    private static final String RAW_TOKEN =
            "token-opaco-para-testes";

    private final UserId userId = UserId.newId();

    private final FakeUserRepository users =
            new FakeUserRepository(
                    User.registerLocal(
                            userId,
                            "Maria Silva",
                            Email.of("maria@example.com"),
                            NOW
                    )
            );

    private final FakeGenerator generator =
            new FakeGenerator();

    private final FakeDigester digester =
            new FakeDigester();

    private final FakeIssuer issuer =
            new FakeIssuer();

    private PrepareEmailVerificationService service() {
        return new PrepareEmailVerificationService(
                users,
                generator,
                digester,
                issuer,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldPrepareVerificationForPendingUser() {
        PreparedVerificationEmail email =
                service().execute(userId).orElseThrow();

        assertEquals(
                Email.of("maria@example.com"),
                email.recipient()
        );
        assertEquals(RAW_TOKEN, email.token());
        assertEquals(
                NOW.plusSeconds(24 * 60 * 60),
                email.expiresAt()
        );

        assertEquals(1, generator.calls);
        assertEquals(RAW_TOKEN, digester.receivedToken);
        assertEquals(1, issuer.calls);
        assertEquals(userId, issuer.receivedUserId);
        assertEquals(NOW, issuer.issuedAt);
        assertEquals(email.expiresAt(), issuer.expiresAt);

        assertEquals(
                new VerificationTokenDigest("a".repeat(64)),
                issuer.receivedDigest
        );
    }

    @Test
    void shouldIgnoreUnknownUser() {
        users.user = null;

        assertTrue(service().execute(userId).isEmpty());

        assertEquals(0, generator.calls);
        assertEquals(0, issuer.calls);
    }

    @Test
    void shouldIgnoreActiveUser() {
        users.user.verifyEmail(NOW);

        assertTrue(service().execute(userId).isEmpty());

        assertEquals(0, generator.calls);
        assertEquals(0, issuer.calls);
    }

    @Test
    void shouldIgnoreBlockedUser() {
        users.user.verifyEmail(NOW);
        users.user.block(NOW);

        assertTrue(service().execute(userId).isEmpty());

        assertEquals(0, generator.calls);
        assertEquals(0, issuer.calls);
    }

    @Test
    void shouldSkipWhenAccountChangesBeforeIssuance() {
        issuer.accept = false;

        assertTrue(service().execute(userId).isEmpty());

        assertEquals(1, issuer.calls);
    }

    @Test
    void shouldNotPersistWhenGenerationFails() {
        generator.fail = true;

        assertThrows(
                IllegalStateException.class,
                () -> service().execute(userId)
        );

        assertEquals(0, issuer.calls);
    }

    @Test
    void shouldNotPersistWhenDigestingFails() {
        digester.fail = true;

        assertThrows(
                IllegalStateException.class,
                () -> service().execute(userId)
        );

        assertEquals(0, issuer.calls);
    }

    @Test
    void shouldPropagatePersistenceFailure() {
        issuer.fail = true;

        assertThrows(
                IllegalStateException.class,
                () -> service().execute(userId)
        );

        assertEquals(1, issuer.calls);
    }

    @Test
    void shouldRejectNullUserId() {
        assertThrows(
                NullPointerException.class,
                () -> service().execute(null)
        );

        assertEquals(0, generator.calls);
    }

    @Test
    void shouldNotExposeTokenInToString() {
        PreparedVerificationEmail email =
                service().execute(userId).orElseThrow();

        assertFalse(email.toString().contains(RAW_TOKEN));
        assertFalse(
                email.toString().contains("maria@example.com")
        );
    }

    private static final class FakeUserRepository
            implements UserRepository {

        User user;

        FakeUserRepository(User user) {
            this.user = user;
        }

        @Override
        public Optional<User> findById(UserId id) {
            if (user != null && user.getId().equals(id)) {
                return Optional.of(user);
            }
            return Optional.empty();
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return Optional.empty();
        }

        @Override
        public boolean existsByEmail(Email email) {
            return false;
        }

        @Override
        public void save(User user) {
            throw new AssertionError(
                    "Gravacao nao esperada neste caso de uso."
            );
        }
    }

    private static final class FakeGenerator
            implements VerificationTokenGenerator {

        int calls;
        boolean fail;

        @Override
        public String generate() {
            calls++;

            if (fail) {
                throw new IllegalStateException(
                        "Falha na geracao do token."
                );
            }

            return RAW_TOKEN;
        }
    }

    private static final class FakeDigester
            implements VerificationTokenDigester {

        String receivedToken;
        boolean fail;

        @Override
        public VerificationTokenDigest digest(String token) {
            receivedToken = token;

            if (fail) {
                throw new IllegalStateException(
                        "Falha ao calcular digest."
                );
            }

            return new VerificationTokenDigest(
                    "a".repeat(64)
            );
        }
    }

    private static final class FakeIssuer
            implements EmailVerificationTokenIssuer {

        int calls;
        boolean accept = true;
        boolean fail;

        UserId receivedUserId;
        VerificationTokenDigest receivedDigest;
        Instant issuedAt;
        Instant expiresAt;

        @Override
        public boolean issueIfPending(
                UserId userId,
                VerificationTokenDigest digest,
                Instant issuedAt,
                Instant expiresAt
        ) {
            calls++;

            receivedUserId = userId;
            receivedDigest = digest;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;

            if (fail) {
                throw new IllegalStateException(
                        "Falha de persistencia."
                );
            }

            return accept;
        }
    }
}
