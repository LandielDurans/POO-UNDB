package br.com.fuelfinder.identity.adapter.out.persistence.jpa;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.LocalCredentialJpaEntity;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.UserJpaEntity;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.VerificationEmailOutboxJpaEntity;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.mapper.UserJpaMapper;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository.SpringDataLocalCredentialJpaRepository;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository.SpringDataUserJpaRepository;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository.SpringDataVerificationEmailOutboxJpaRepository;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.port.out.LocalRegistrationPersistence;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.model.User;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Repository
public class LocalRegistrationPersistenceAdapter
        implements LocalRegistrationPersistence {

    private final SpringDataUserJpaRepository userRepository;
    private final SpringDataLocalCredentialJpaRepository credentialRepository;
    private final SpringDataVerificationEmailOutboxJpaRepository outboxRepository;
    private final UserJpaMapper userMapper;
    private final Clock clock;

    public LocalRegistrationPersistenceAdapter(
            SpringDataUserJpaRepository userRepository,
            SpringDataLocalCredentialJpaRepository credentialRepository,
            SpringDataVerificationEmailOutboxJpaRepository outboxRepository,
            UserJpaMapper userMapper,
            Clock clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.credentialRepository = Objects.requireNonNull(credentialRepository);
        this.outboxRepository = Objects.requireNonNull(outboxRepository);
        this.userMapper = Objects.requireNonNull(userMapper);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public void register(
            User user,
            PasswordHash passwordHash
    ) {
        Objects.requireNonNull(
                user,
                "Usuario nao pode ser nulo."
        );

        Objects.requireNonNull(
                passwordHash,
                "PasswordHash nao pode ser nulo."
        );

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new IllegalArgumentException(
                    "Cadastro exige usuario pendente de verificacao."
            );
        }

        Instant now = clock.instant();
        UUID userId = user.getId().value();

        UserJpaEntity userEntity =
                userMapper.toEntity(user);

        LocalCredentialJpaEntity credentialEntity =
                new LocalCredentialJpaEntity(
                        userId,
                        passwordHash.value(),
                        now
                );

        VerificationEmailOutboxJpaEntity outboxEntity =
                new VerificationEmailOutboxJpaEntity(
                        UUID.randomUUID(),
                        userId,
                        now
                );

        userRepository.saveAndFlush(userEntity);
        credentialRepository.saveAndFlush(credentialEntity);
        outboxRepository.saveAndFlush(outboxEntity);
    }
}
