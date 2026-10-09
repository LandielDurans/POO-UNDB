
package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.model.PreparedVerificationEmail;
import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.in.PrepareEmailVerificationUseCase;
import br.com.fuelfinder.identity.application.port.out.EmailVerificationTokenIssuer;
import br.com.fuelfinder.identity.application.port.out.UserRepository;
import br.com.fuelfinder.identity.application.port.out.VerificationTokenDigester;
import br.com.fuelfinder.identity.application.port.out.VerificationTokenGenerator;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class PrepareEmailVerificationService
        implements PrepareEmailVerificationUseCase {

    private static final Duration TOKEN_TTL =
            Duration.ofHours(24);

    private final UserRepository userRepository;
    private final VerificationTokenGenerator tokenGenerator;
    private final VerificationTokenDigester tokenDigester;
    private final EmailVerificationTokenIssuer tokenIssuer;
    private final Clock clock;

    public PrepareEmailVerificationService(
            UserRepository userRepository,
            VerificationTokenGenerator tokenGenerator,
            VerificationTokenDigester tokenDigester,
            EmailVerificationTokenIssuer tokenIssuer,
            Clock clock
    ) {
        this.userRepository = Objects.requireNonNull(
                userRepository
        );

        this.tokenGenerator = Objects.requireNonNull(
                tokenGenerator
        );

        this.tokenDigester = Objects.requireNonNull(
                tokenDigester
        );

        this.tokenIssuer = Objects.requireNonNull(
                tokenIssuer
        );

        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Optional<PreparedVerificationEmail> execute(
            UserId userId
    ) {
        Objects.requireNonNull(
                userId, "UserId nao pode ser nulo."
        );

        Optional<User> existingUser =
                userRepository.findById(userId);

        if (existingUser.isEmpty()) {
            return Optional.empty();
        }

        User user = existingUser.get();

        if (user.getStatus()
                != UserStatus.PENDING_VERIFICATION) {
            return Optional.empty();
        }

        Email recipient = user.getEmail().orElseThrow(
                () -> new IllegalStateException(
                        "Usuario pendente deve possuir email."
                )
        );

        String rawToken = Objects.requireNonNull(
                tokenGenerator.generate(),
                "Gerador retornou token nulo."
        );

        if (rawToken.isBlank()) {
            throw new IllegalStateException(
                    "Gerador retornou token vazio."
            );
        }

        VerificationTokenDigest digest =
                Objects.requireNonNull(
                        tokenDigester.digest(rawToken),
                        "Digester retornou digest nulo."
                );

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(TOKEN_TTL);

        boolean issued = tokenIssuer.issueIfPending(
                userId,
                digest,
                issuedAt,
                expiresAt
        );

        if (!issued) {
            return Optional.empty();
        }

        return Optional.of(
                new PreparedVerificationEmail(
                        recipient,
                        rawToken,
                        expiresAt
                )
        );
    }
}
