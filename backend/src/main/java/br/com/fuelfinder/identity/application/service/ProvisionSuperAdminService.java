package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.port.in.ProvisionSuperAdminCommand;
import br.com.fuelfinder.identity.application.port.in.ProvisionSuperAdminUseCase;
import br.com.fuelfinder.identity.application.port.out.PasswordHasher;
import br.com.fuelfinder.identity.application.port.out.PrivilegedAccountPersistence;
import br.com.fuelfinder.identity.application.port.out.UserRepository;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.time.Clock;
import java.util.Objects;

public final class ProvisionSuperAdminService
        implements ProvisionSuperAdminUseCase {

    private final PrivilegedAccountPersistence persistence;
    private final UserRepository userRepository;
    private final PasswordPolicy passwordPolicy;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public ProvisionSuperAdminService(
            PrivilegedAccountPersistence persistence,
            UserRepository userRepository,
            PasswordPolicy passwordPolicy,
            PasswordHasher passwordHasher,
            Clock clock
    ) {
        this.persistence = Objects.requireNonNull(persistence);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordPolicy = Objects.requireNonNull(passwordPolicy);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public boolean execute(ProvisionSuperAdminCommand command) {
        Objects.requireNonNull(command, "Comando nao pode ser nulo.");

        if (persistence.existsActiveSuperAdmin()) {
            return false;
        }

        // Nunca promove silenciosamente uma conta existente.
        if (userRepository.existsByEmail(command.email())) {
            throw new IllegalStateException(
                    "O e-mail do SUPER_ADMIN ja pertence a outra conta."
            );
        }

        passwordPolicy.validate(command.password());

        User superAdmin = User.provisionSuperAdmin(
                UserId.newId(),
                command.name(),
                command.email(),
                clock.instant()
        );

        persistence.createActiveAccount(
                superAdmin,
                passwordHasher.hash(command.password())
        );

        return true;
    }
}
