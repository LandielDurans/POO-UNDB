package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.model.PreparedVerificationEmail;
import br.com.fuelfinder.identity.application.port.in.DispatchVerificationEmailUseCase;
import br.com.fuelfinder.identity.application.port.in.PrepareEmailVerificationUseCase;
import br.com.fuelfinder.identity.application.port.out.VerificationEmailSender;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.util.Objects;
import java.util.Optional;

public final class DispatchVerificationEmailService
        implements DispatchVerificationEmailUseCase {

    private final PrepareEmailVerificationUseCase preparation;
    private final VerificationEmailSender emailSender;

    public DispatchVerificationEmailService(
            PrepareEmailVerificationUseCase preparation,
            VerificationEmailSender emailSender
    ) {
        this.preparation = Objects.requireNonNull(
                preparation,
                "Preparacao de email nao pode ser nula."
        );

        this.emailSender = Objects.requireNonNull(
                emailSender,
                "EmailSender nao pode ser nulo."
        );
    }

    @Override
    public boolean execute(UserId userId) {
        Objects.requireNonNull(
                userId,
                "UserId nao pode ser nulo."
        );

        Optional<PreparedVerificationEmail> prepared =
                Objects.requireNonNull(
                        preparation.execute(userId),
                        "Preparacao retornou resultado nulo."
                );

        if (prepared.isEmpty()) {
            return false;
        }

        emailSender.send(prepared.get());

        return true;
    }
}
