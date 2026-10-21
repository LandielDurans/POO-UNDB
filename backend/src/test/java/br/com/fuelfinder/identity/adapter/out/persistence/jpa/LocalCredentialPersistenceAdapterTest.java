package br.com.fuelfinder.identity.adapter.out.persistence.jpa;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.LocalCredentialJpaEntity;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository.SpringDataLocalCredentialJpaRepository;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LocalCredentialPersistenceAdapterTest {

    private static final Instant NOW =
            Instant.parse("2026-10-10T12:00:00Z");

    private final SpringDataLocalCredentialJpaRepository repository =
            mock(SpringDataLocalCredentialJpaRepository.class);

    private final Clock clock =
            Clock.fixed(NOW, ZoneOffset.UTC);

    private final LocalCredentialPersistenceAdapter adapter =
            new LocalCredentialPersistenceAdapter(repository, clock);

    @Test
    void shouldSaveNewCredential() {
        UserId userId = UserId.newId();
        PasswordHash hash = new PasswordHash("encoded-hash-123");

        adapter.save(userId, hash);

        ArgumentCaptor<LocalCredentialJpaEntity> captor =
                ArgumentCaptor.forClass(
                        LocalCredentialJpaEntity.class
                );

        verify(repository).findById(userId.value());
        verify(repository).saveAndFlush(captor.capture());

        LocalCredentialJpaEntity saved = captor.getValue();

        assertEquals(userId.value(), saved.getUserId());
        assertEquals(hash.value(), saved.getPasswordHash());
        assertEquals(NOW, saved.getCreatedAt());
        assertEquals(NOW, saved.getUpdatedAt());
    }

    @Test
    void shouldUpdateExistingCredential() {
        UserId userId = UserId.newId();
        Instant createdAt = NOW.minusSeconds(3600);

        LocalCredentialJpaEntity existing =
                new LocalCredentialJpaEntity(
                        userId.value(),
                        "old-encoded-hash",
                        createdAt
                );

        when(repository.findById(userId.value()))
                .thenReturn(Optional.of(existing));

        adapter.save(
                userId,
                new PasswordHash("new-encoded-hash")
        );

        verify(repository).saveAndFlush(same(existing));

        assertEquals(
                "new-encoded-hash",
                existing.getPasswordHash()
        );

        assertEquals(createdAt, existing.getCreatedAt());
        assertEquals(NOW, existing.getUpdatedAt());
    }

    @Test
    void shouldFindExistingPasswordHash() {
        UserId userId = UserId.newId();

        LocalCredentialJpaEntity entity =
                new LocalCredentialJpaEntity(
                        userId.value(),
                        "encoded-hash",
                        NOW
                );

        when(repository.findById(userId.value()))
                .thenReturn(Optional.of(entity));

        PasswordHash restored =
                adapter.findPasswordHashByUserId(userId)
                        .orElseThrow();

        assertEquals("encoded-hash", restored.value());
    }

    @Test
    void shouldReturnEmptyWhenCredentialDoesNotExist() {
        UserId userId = UserId.newId();

        when(repository.findById(userId.value()))
                .thenReturn(Optional.empty());

        assertTrue(
                adapter.findPasswordHashByUserId(userId).isEmpty()
        );
    }

    @Test
    void shouldRejectNullUserIdWhenSaving() {
        assertThrows(
                NullPointerException.class,
                () -> adapter.save(
                        null,
                        new PasswordHash("encoded-hash")
                )
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectNullPasswordHashWhenSaving() {
        assertThrows(
                NullPointerException.class,
                () -> adapter.save(UserId.newId(), null)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectNullUserIdWhenReading() {
        assertThrows(
                NullPointerException.class,
                () -> adapter.findPasswordHashByUserId(null)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectHashLongerThanDatabaseColumn() {
        UserId userId = UserId.newId();

        PasswordHash oversized =
                new PasswordHash("x".repeat(256));

        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.save(userId, oversized)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldPropagatePersistenceFailure() {
        when(repository.saveAndFlush(
                any(LocalCredentialJpaEntity.class)
        )).thenThrow(
                new IllegalStateException(
                        "Falha simulada de persistencia."
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> adapter.save(
                        UserId.newId(),
                        new PasswordHash("encoded-hash")
                )
        );
    }

    @Test
    void shouldNotExposePasswordHashInToString() {
        LocalCredentialJpaEntity entity =
                new LocalCredentialJpaEntity(
                        UserId.newId().value(),
                        "sensitive-encoded-hash",
                        NOW
                );

        assertFalse(
                entity.toString().contains(
                        "sensitive-encoded-hash"
                )
        );

        assertTrue(
                entity.toString().contains("REDACTED")
        );
    }
}
