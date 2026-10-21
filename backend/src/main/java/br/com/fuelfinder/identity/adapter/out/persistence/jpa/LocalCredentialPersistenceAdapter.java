package br.com.fuelfinder.identity.adapter.out.persistence.jpa;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.LocalCredentialJpaEntity;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository.SpringDataLocalCredentialJpaRepository;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.port.out.LocalCredentialRepository;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class LocalCredentialPersistenceAdapter
        implements LocalCredentialRepository {

    private final SpringDataLocalCredentialJpaRepository repository;
    private final Clock clock;

    public LocalCredentialPersistenceAdapter(
            SpringDataLocalCredentialJpaRepository repository,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "Repositorio de credenciais nao pode ser nulo."
        );

        this.clock = Objects.requireNonNull(
                clock,
                "Clock nao pode ser nulo."
        );
    }

    @Override
    @Transactional
    public void save(
            UserId userId,
            PasswordHash passwordHash
    ) {
        Objects.requireNonNull(
                userId,
                "UserId nao pode ser nulo."
        );

        Objects.requireNonNull(
                passwordHash,
                "PasswordHash nao pode ser nulo."
        );

        Instant now = clock.instant();

        LocalCredentialJpaEntity incoming =
                new LocalCredentialJpaEntity(
                        userId.value(),
                        passwordHash.value(),
                        now
                );

        LocalCredentialJpaEntity entity = repository
                .findById(userId.value())
                .map(existing -> {
                    existing.replacePasswordHash(
                            incoming.getPasswordHash(),
                            now
                    );
                    return existing;
                })
                .orElse(incoming);

        repository.saveAndFlush(entity);
    }

    @Override
    public Optional<PasswordHash> findPasswordHashByUserId(
            UserId userId
    ) {
        Objects.requireNonNull(
                userId,
                "UserId nao pode ser nulo."
        );

        return repository.findById(userId.value())
                .map(entity ->
                        new PasswordHash(
                                entity.getPasswordHash()
                        )
                );
    }
}
