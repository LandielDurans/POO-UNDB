package br.com.fuelfinder.identity.adapter.out.persistence.jpa;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.LocalCredentialJpaEntity;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.UserJpaEntity;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.VerificationEmailOutboxJpaEntity;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.VerificationEmailOutboxStatus;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.mapper.UserJpaMapper;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository.SpringDataLocalCredentialJpaRepository;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository.SpringDataUserJpaRepository;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository.SpringDataVerificationEmailOutboxJpaRepository;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LocalRegistrationPersistenceAdapterTest {

    private static final Instant NOW =
            Instant.parse("2026-10-10T12:00:00Z");

    private final SpringDataUserJpaRepository users =
            mock(SpringDataUserJpaRepository.class);

    private final SpringDataLocalCredentialJpaRepository credentials =
            mock(SpringDataLocalCredentialJpaRepository.class);

    private final SpringDataVerificationEmailOutboxJpaRepository outbox =
            mock(SpringDataVerificationEmailOutboxJpaRepository.class);

    private final Clock clock =
            Clock.fixed(NOW, ZoneOffset.UTC);

    private final LocalRegistrationPersistenceAdapter adapter =
            new LocalRegistrationPersistenceAdapter(
                    users,
                    credentials,
                    outbox,
                    new UserJpaMapper(),
                    clock
            );

    @Test
    void shouldPersistUserCredentialAndOutboxInOrder() {
        User user = pendingUser();

        adapter.register(
                user,
                new PasswordHash("encoded-password-hash")
        );

        ArgumentCaptor<UserJpaEntity> userCaptor =
                ArgumentCaptor.forClass(UserJpaEntity.class);

        ArgumentCaptor<LocalCredentialJpaEntity> credentialCaptor =
                ArgumentCaptor.forClass(
                        LocalCredentialJpaEntity.class
                );

        ArgumentCaptor<VerificationEmailOutboxJpaEntity> outboxCaptor =
                ArgumentCaptor.forClass(
                        VerificationEmailOutboxJpaEntity.class
                );

        InOrder order = inOrder(users, credentials, outbox);

        order.verify(users).saveAndFlush(
                userCaptor.capture()
        );

        order.verify(credentials).saveAndFlush(
                credentialCaptor.capture()
        );

        order.verify(outbox).saveAndFlush(
                outboxCaptor.capture()
        );

        order.verifyNoMoreInteractions();

        UserJpaEntity savedUser = userCaptor.getValue();

        assertEquals(user.getId().value(), savedUser.getId());
        assertEquals(
                UserStatus.PENDING_VERIFICATION,
                savedUser.getStatus()
        );
        assertEquals(
                Set.of(UserRole.USER),
                savedUser.getRoles()
        );

        LocalCredentialJpaEntity savedCredential =
                credentialCaptor.getValue();

        assertEquals(
                user.getId().value(),
                savedCredential.getUserId()
        );
        assertEquals(
                "encoded-password-hash",
                savedCredential.getPasswordHash()
        );
        assertEquals(NOW, savedCredential.getCreatedAt());

        VerificationEmailOutboxJpaEntity savedEvent =
                outboxCaptor.getValue();

        assertNotNull(savedEvent.getEventId());
        assertEquals(
                user.getId().value(),
                savedEvent.getUserId()
        );
        assertEquals(
                VerificationEmailOutboxStatus.PENDING,
                savedEvent.getStatus()
        );
        assertEquals(0, savedEvent.getAttemptCount());
        assertEquals(NOW, savedEvent.getNextAttemptAt());
        assertEquals(NOW, savedEvent.getCreatedAt());
    }

    @Test
    void shouldRejectNullUserBeforeWriting() {
        assertThrows(
                NullPointerException.class,
                () -> adapter.register(
                        null,
                        new PasswordHash("encoded-hash")
                )
        );

        verifyNoInteractions(users, credentials, outbox);
    }

    @Test
    void shouldRejectNullPasswordHashBeforeWriting() {
        assertThrows(
                NullPointerException.class,
                () -> adapter.register(pendingUser(), null)
        );

        verifyNoInteractions(users, credentials, outbox);
    }

    @Test
    void shouldRejectActiveUserBeforeWriting() {
        User active = User.reconstitute(
                UserId.newId(),
                "Maria Silva",
                Email.of("maria@example.com"),
                Set.of(UserRole.USER),
                UserStatus.ACTIVE,
                NOW,
                NOW
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.register(
                        active,
                        new PasswordHash("encoded-hash")
                )
        );

        verifyNoInteractions(users, credentials, outbox);
    }

    @Test
    void shouldRejectOversizedHashBeforeWriting() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.register(
                        pendingUser(),
                        new PasswordHash("x".repeat(256))
                )
        );

        verifyNoInteractions(users, credentials, outbox);
    }

    @Test
    void shouldStopIfUserPersistenceFails() {
        when(users.saveAndFlush(any(UserJpaEntity.class)))
                .thenThrow(
                        new IllegalStateException(
                                "Falha ao salvar usuario."
                        )
                );

        assertThrows(
                IllegalStateException.class,
                () -> adapter.register(
                        pendingUser(),
                        new PasswordHash("encoded-hash")
                )
        );

        verifyNoInteractions(credentials, outbox);
    }

    @Test
    void shouldNotCreateOutboxWhenCredentialFails() {
        when(credentials.saveAndFlush(
                any(LocalCredentialJpaEntity.class)
        )).thenThrow(
                new IllegalStateException(
                        "Falha ao salvar credencial."
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> adapter.register(
                        pendingUser(),
                        new PasswordHash("encoded-hash")
                )
        );

        verify(users).saveAndFlush(
                any(UserJpaEntity.class)
        );

        verifyNoInteractions(outbox);
    }

    @Test
    void shouldPropagateOutboxPersistenceFailure() {
        when(outbox.saveAndFlush(
                any(VerificationEmailOutboxJpaEntity.class)
        )).thenThrow(
                new IllegalStateException(
                        "Falha ao salvar evento."
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> adapter.register(
                        pendingUser(),
                        new PasswordHash("encoded-hash")
                )
        );

        verify(users).saveAndFlush(
                any(UserJpaEntity.class)
        );

        verify(credentials).saveAndFlush(
                any(LocalCredentialJpaEntity.class)
        );
    }

    private User pendingUser() {
        return User.registerLocal(
                UserId.newId(),
                "Maria Silva",
                Email.of("maria@example.com"),
                NOW
        );
    }
}
