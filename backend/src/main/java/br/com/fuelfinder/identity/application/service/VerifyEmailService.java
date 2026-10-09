
package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.exception.InvalidEmailVerificationTokenException;
import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailCommand;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailUseCase;
import br.com.fuelfinder.identity.application.port.out.EmailVerificationPersistence;
import br.com.fuelfinder.identity.application.port.out.VerificationTokenDigester;

import java.time.Clock;
import java.util.Objects;

public final class VerifyEmailService
        implements VerifyEmailUseCase {

    private final VerificationTokenDigester tokenDigester;
    private final EmailVerificationPersistence persistence;
    private final Clock clock;

    public VerifyEmailService(
            VerificationTokenDigester tokenDigester,
            EmailVerificationPersistence persistence,
            Clock clock
    ) {
        this.tokenDigester = Objects.requireNonNull(
                tokenDigester,
                "TokenDigester nao pode ser nulo."
        );

        this.persistence = Objects.requireNonNull(
                persistence,
                "Persistence nao pode ser nula."
        );

        this.clock = Objects.requireNonNull(
                clock,
                "Clock nao pode ser nulo."
        );
    }

    @Override
    public void execute(VerifyEmailCommand command) {
        Objects.requireNonNull(
                command,
                "Comando de verificacao nao pode ser nulo."
        );

        VerificationTokenDigest digest =
                Objects.requireNonNull(
                        tokenDigester.digest(command.token()),
                        "TokenDigester retornou digest nulo."
                );

        boolean activated = persistence.consumeAndActivate(
                digest,
                clock.instant()
        );

        if (!activated) {
            throw new InvalidEmailVerificationTokenException();
        }
    }
}
