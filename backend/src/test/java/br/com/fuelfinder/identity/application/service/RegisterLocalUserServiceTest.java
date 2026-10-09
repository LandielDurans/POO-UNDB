
package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.exception.EmailAlreadyRegisteredException;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserCommand;
import br.com.fuelfinder.identity.application.port.out.LocalRegistrationPersistence;
import br.com.fuelfinder.identity.application.port.out.PasswordHasher;
import br.com.fuelfinder.identity.application.port.out.UserRepository;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RegisterLocalUserServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-10-09T12:00:00Z");

    private final FakeUserRepository users =
            new FakeUserRepository();

    private final FakePasswordHasher hasher =
            new FakePasswordHasher();

    private final RecordingRegistrationPersistence persistence =
            new RecordingRegistrationPersistence();

    private RegisterLocalUserService service() {
        return new RegisterLocalUserService(
                users,
                hasher,
                new PasswordPolicy(),
                persistence,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private RegisterLocalUserCommand command(String password) {
        return new RegisterLocalUserCommand(
                "Maria Silva",
                Email.of("maria@example.com"),
                password
        );
    }

    @Test
    void shouldRegisterPendingUserWithHashedPassword() {
        String password = "uma senha bastante segura";

        UserId id = service().execute(command(password));

        assertEquals(1, persistence.calls);
        assertEquals(id, persistence.user.getId());
        assertEquals(
                UserStatus.PENDING_VERIFICATION,
                persistence.user.getStatus()
        );
        assertEquals(NOW, persistence.user.getCreatedAt());
        assertEquals(password, hasher.receivedPassword);
        assertEquals(
                "test-only-encoded-hash",
                persistence.passwordHash.value()
        );
        assertNotEquals(
                password,
                persistence.passwordHash.value()
        );
    }

    @Test
    void shouldRejectAlreadyRegisteredEmail() {
        users.emailExists = true;

        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> service().execute(
                        command("uma senha bastante segura")
                )
        );

        assertEquals(0, persistence.calls);
        assertNull(hasher.receivedPassword);
    }

    @Test
    void shouldRejectWeakPasswordBeforePersistence() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service().execute(command("123456"))
        );

        assertEquals(0, users.emailChecks);
        assertEquals(0, persistence.calls);
        assertNull(hasher.receivedPassword);
    }

    @Test
    void shouldRejectInvalidUserName() {
        RegisterLocalUserCommand invalid =
                new RegisterLocalUserCommand(
                        " ",
                        Email.of("maria@example.com"),
                        "uma senha bastante segura"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service().execute(invalid)
        );

        assertEquals(0, persistence.calls);
        assertNull(hasher.receivedPassword);
    }

    @Test
    void shouldNotPersistWhenHashingFails() {
        hasher.fail = true;

        assertThrows(
                IllegalStateException.class,
                () -> service().execute(
                        command("uma senha bastante segura")
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
                        command("uma senha bastante segura")
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
    }

    private static final class FakeUserRepository
            implements UserRepository {

        boolean emailExists;
        int emailChecks;

        @Override
        public Optional<User> findById(UserId id) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return Optional.empty();
        }

        @Override
        public boolean existsByEmail(Email email) {
            emailChecks++;
            return emailExists;
        }

        @Override
        public void save(User user) {
            throw new AssertionError(
                    "Cadastro nao deve usar gravacao isolada."
            );
        }
    }

    private static final class FakePasswordHasher
            implements PasswordHasher {

        String receivedPassword;
        boolean fail;

        @Override
        public PasswordHash hash(String plainPassword) {
            receivedPassword = plainPassword;

            if (fail) {
                throw new IllegalStateException(
                        "Falha na geracao do hash."
                );
            }

            return new PasswordHash("test-only-encoded-hash");
        }

        @Override
        public boolean matches(
                String plainPassword,
                PasswordHash storedHash
        ) {
            return false;
        }
    }

    private static final class RecordingRegistrationPersistence
            implements LocalRegistrationPersistence {

        User user;
        PasswordHash passwordHash;
        int calls;
        boolean fail;

        @Override
        public void register(
                User user,
                PasswordHash passwordHash
        ) {
            calls++;

            if (fail) {
                throw new IllegalStateException(
                        "Falha de persistencia."
                );
            }

            this.user = user;
            this.passwordHash = passwordHash;
        }
    }
}
