package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.model.PreparedVerificationEmail;
import br.com.fuelfinder.identity.application.port.in.PrepareEmailVerificationUseCase;
import br.com.fuelfinder.identity.application.port.out.VerificationEmailSender;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DispatchVerificationEmailServiceTest {

    private final UserId userId = UserId.newId();

    private final FakePreparation preparation =
            new FakePreparation();

    private final RecordingEmailSender sender =
            new RecordingEmailSender();

    private DispatchVerificationEmailService service() {
        return new DispatchVerificationEmailService(
                preparation,
                sender
        );
    }

    @Test
    void shouldDispatchPreparedVerificationEmail() {
        boolean dispatched = service().execute(userId);

        assertTrue(dispatched);
        assertEquals(1, preparation.calls);
        assertEquals(userId, preparation.receivedUserId);
        assertEquals(1, sender.calls);
        assertSame(preparation.email, sender.receivedEmail);
    }

    @Test
    void shouldNotSendWhenPreparationReturnsEmpty() {
        preparation.empty = true;

        boolean dispatched = service().execute(userId);

        assertFalse(dispatched);
        assertEquals(1, preparation.calls);
        assertEquals(0, sender.calls);
    }

    @Test
    void shouldNotSendWhenPreparationFails() {
        preparation.fail = true;

        assertThrows(
                IllegalStateException.class,
                () -> service().execute(userId)
        );

        assertEquals(0, sender.calls);
    }

    @Test
    void shouldPropagateEmailSendingFailure() {
        sender.fail = true;

        assertThrows(
                IllegalStateException.class,
                () -> service().execute(userId)
        );

        assertEquals(1, sender.calls);
        assertSame(preparation.email, sender.receivedEmail);
    }

    @Test
    void shouldRejectNullUserId() {
        assertThrows(
                NullPointerException.class,
                () -> service().execute(null)
        );

        assertEquals(0, preparation.calls);
        assertEquals(0, sender.calls);
    }

    @Test
    void shouldRejectNullPreparationResult() {
        preparation.returnNull = true;

        assertThrows(
                NullPointerException.class,
                () -> service().execute(userId)
        );

        assertEquals(0, sender.calls);
    }

    @Test
    void shouldRejectNullDependencies() {
        assertThrows(
                NullPointerException.class,
                () -> new DispatchVerificationEmailService(
                        null,
                        sender
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new DispatchVerificationEmailService(
                        preparation,
                        null
                )
        );
    }

    private static final class FakePreparation
            implements PrepareEmailVerificationUseCase {

        int calls;
        boolean empty;
        boolean fail;
        boolean returnNull;

        UserId receivedUserId;

        final PreparedVerificationEmail email =
                new PreparedVerificationEmail(
                        Email.of("maria@example.com"),
                        "token-de-teste",
                        Instant.parse("2026-10-10T12:00:00Z")
                );

        @Override
        public Optional<PreparedVerificationEmail> execute(
                UserId userId
        ) {
            calls++;
            receivedUserId = userId;

            if (fail) {
                throw new IllegalStateException(
                        "Falha na preparacao."
                );
            }

            if (returnNull) {
                return null;
            }

            return empty
                    ? Optional.empty()
                    : Optional.of(email);
        }
    }

    private static final class RecordingEmailSender
            implements VerificationEmailSender {

        int calls;
        boolean fail;

        PreparedVerificationEmail receivedEmail;

        @Override
        public void send(PreparedVerificationEmail email) {
            calls++;
            receivedEmail = email;

            if (fail) {
                throw new IllegalStateException(
                        "Falha ao solicitar envio."
                );
            }
        }
    }
}
