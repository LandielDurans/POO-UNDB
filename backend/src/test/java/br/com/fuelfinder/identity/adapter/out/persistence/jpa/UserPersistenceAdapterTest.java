
package br.com.fuelfinder.identity.adapter.out.persistence.jpa;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.UserJpaEntity;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.mapper.UserJpaMapper;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository.SpringDataUserJpaRepository;
import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserPersistenceAdapterTest {

    private static final Instant NOW =
            Instant.parse("2026-10-10T12:00:00Z");

    private final SpringDataUserJpaRepository repository =
            mock(SpringDataUserJpaRepository.class);

    private final UserJpaMapper mapper =
            new UserJpaMapper();

    private final UserPersistenceAdapter adapter =
            new UserPersistenceAdapter(repository, mapper);

    @Test
    void shouldFindUserById() {
        User user = pendingUser();

        when(repository.findById(user.getId().value()))
                .thenReturn(
                        Optional.of(mapper.toEntity(user))
                );

        User restored = adapter.findById(user.getId())
                .orElseThrow();

        assertEquals(user.getId(), restored.getId());
        assertEquals(user.getName(), restored.getName());
        assertEquals(user.getEmail(), restored.getEmail());
        assertEquals(user.getRoles(), restored.getRoles());

        verify(repository).findById(user.getId().value());
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {
        UserId id = UserId.newId();

        when(repository.findById(id.value()))
                .thenReturn(Optional.empty());

        assertTrue(adapter.findById(id).isEmpty());
    }

    @Test
    void shouldFindUserByNormalizedEmail() {
        User user = pendingUser();

        when(repository.findByEmail("maria@example.com"))
                .thenReturn(
                        Optional.of(mapper.toEntity(user))
                );

        Email email = Email.of("  MARIA@EXAMPLE.COM  ");

        User restored = adapter.findByEmail(email)
                .orElseThrow();

        assertEquals(user.getId(), restored.getId());

        verify(repository).findByEmail(
                "maria@example.com"
        );
    }

    @Test
    void shouldReturnEmailExistence() {
        Email email = Email.of("maria@example.com");

        when(repository.existsByEmail(email.value()))
                .thenReturn(true);

        assertTrue(adapter.existsByEmail(email));

        when(repository.existsByEmail(email.value()))
                .thenReturn(false);

        assertFalse(adapter.existsByEmail(email));
    }

    @Test
    void shouldSaveMappedUser() {
        User user = pendingUser();

        adapter.save(user);

        ArgumentCaptor<UserJpaEntity> captor =
                ArgumentCaptor.forClass(
                        UserJpaEntity.class
                );

        verify(repository).saveAndFlush(
                captor.capture()
        );

        UserJpaEntity saved = captor.getValue();

        assertEquals(user.getId().value(), saved.getId());
        assertEquals(user.getName(), saved.getName());
        assertEquals("maria@example.com", saved.getEmail());
        assertEquals(
                UserStatus.PENDING_VERIFICATION,
                saved.getStatus()
        );
        assertEquals(Set.of(UserRole.USER), saved.getRoles());
    }

    @Test
    void shouldSaveAnonymizedDeletedUser() {
        User user = pendingUser();
        user.delete(NOW.plusSeconds(60));

        adapter.save(user);

        ArgumentCaptor<UserJpaEntity> captor =
                ArgumentCaptor.forClass(
                        UserJpaEntity.class
                );

        verify(repository).saveAndFlush(
                captor.capture()
        );

        assertNull(captor.getValue().getEmail());
        assertEquals(
                UserStatus.DELETED,
                captor.getValue().getStatus()
        );
        assertEquals(
                Set.of(UserRole.USER),
                captor.getValue().getRoles()
        );
    }

    @Test
    void shouldRejectNullArguments() {
        assertThrows(
                NullPointerException.class,
                () -> adapter.findById(null)
        );

        assertThrows(
                NullPointerException.class,
                () -> adapter.findByEmail(null)
        );

        assertThrows(
                NullPointerException.class,
                () -> adapter.existsByEmail(null)
        );

        assertThrows(
                NullPointerException.class,
                () -> adapter.save(null)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldPropagatePersistenceFailures() {
        when(repository.saveAndFlush(any(UserJpaEntity.class)))
                .thenThrow(
                        new IllegalStateException(
                                "Falha simulada de persistencia."
                        )
                );

        assertThrows(
                IllegalStateException.class,
                () -> adapter.save(pendingUser())
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
