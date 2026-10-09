
package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.exception.EmailAlreadyRegisteredException;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserCommand;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserUseCase;
import br.com.fuelfinder.identity.application.port.out.LocalRegistrationPersistence;
import br.com.fuelfinder.identity.application.port.out.PasswordHasher;
import br.com.fuelfinder.identity.application.port.out.UserRepository;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.time.Clock;
import java.util.Objects;

public final class RegisterLocalUserService
        implements RegisterLocalUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final LocalRegistrationPersistence registrationPersistence;
    private final Clock clock;

    public RegisterLocalUserService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            PasswordPolicy passwordPolicy,
            LocalRegistrationPersistence registrationPersistence,
            Clock clock
    ) {
        this.userRepository = Objects.requireNonNull(
                userRepository
        );

        this.passwordHasher = Objects.requireNonNull(
                passwordHasher
        );

        this.passwordPolicy = Objects.requireNonNull(
                passwordPolicy
        );

        this.registrationPersistence = Objects.requireNonNull(
                registrationPersistence
        );

        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public UserId execute(RegisterLocalUserCommand command) {
        Objects.requireNonNull(
                command,
                "Comando de cadastro nao pode ser nulo."
        );

        passwordPolicy.validate(command.plainPassword());

        User user = User.registerLocal(
                UserId.newId(),
                command.name(),
                command.email(),
                clock.instant()
        );

        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyRegisteredException();
        }

        PasswordHash passwordHash = Objects.requireNonNull(
                passwordHasher.hash(command.plainPassword()),
                "PasswordHasher retornou um hash nulo."
        );

        registrationPersistence.register(user, passwordHash);

        return user.getId();
    }
}
