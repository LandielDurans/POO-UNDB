package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.port.in.ProvisionSuperAdminCommand;
import br.com.fuelfinder.identity.application.port.out.PasswordHasher;
import br.com.fuelfinder.identity.application.port.out.PrivilegedAccountPersistence;
import br.com.fuelfinder.identity.application.port.out.UserRepository;
import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProvisionSuperAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final Email EMAIL = Email.of("root@fuelfinder.local");

    private final PrivilegedAccountPersistence persistence =
            mock(PrivilegedAccountPersistence.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);

    private final ProvisionSuperAdminService service =
            new ProvisionSuperAdminService(
                    persistence,
                    users,
                    new PasswordPolicy(),
                    hasher,
                    Clock.fixed(NOW, ZoneOffset.UTC)
            );

    @Test
    void shouldCreateActiveSuperAdmin() {
        when(hasher.hash("SenhaForte2026")).thenReturn(new PasswordHash("hash"));

        assertTrue(service.execute(command("SenhaForte2026")));

        ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
        verify(persistence).createActiveAccount(
                created.capture(),
                eq(new PasswordHash("hash"))
        );

        User user = created.getValue();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(
                Set.of(UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN),
                user.getRoles()
        );
        assertEquals(EMAIL, user.getEmail().orElseThrow());
        assertEquals(NOW, user.getCreatedAt());
    }

    @Test
    void shouldDoNothingWhenSuperAdminAlreadyExists() {
        when(persistence.existsActiveSuperAdmin()).thenReturn(true);

        assertFalse(service.execute(command("SenhaForte2026")));

        verify(persistence, never()).createActiveAccount(any(), any());
        verifyNoInteractions(hasher);
    }

    @Test
    void shouldNotPromoteExistingAccount() {
        when(users.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> service.execute(command("SenhaForte2026"))
        );
        verify(persistence, never()).createActiveAccount(any(), any());
    }

    @Test
    void shouldApplyPasswordPolicy() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(command("curta"))
        );
        verify(persistence, never()).createActiveAccount(any(), any());
    }

    private static ProvisionSuperAdminCommand command(String password) {
        return new ProvisionSuperAdminCommand("Super Admin", EMAIL, password);
    }
}
