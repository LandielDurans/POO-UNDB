package br.com.fuelfinder.identity.adapter.out.persistence.jpa;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.UserJpaEntity;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.mapper.UserJpaMapper;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository.SpringDataUserJpaRepository;
import br.com.fuelfinder.identity.application.port.out.UserRepository;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class UserPersistenceAdapter implements UserRepository {

    private final SpringDataUserJpaRepository repository;
    private final UserJpaMapper mapper;

    public UserPersistenceAdapter(
            SpringDataUserJpaRepository repository,
            UserJpaMapper mapper
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "Repositorio JPA nao pode ser nulo."
        );

        this.mapper = Objects.requireNonNull(
                mapper,
                "Mapper nao pode ser nulo."
        );
    }

    @Override
    public Optional<User> findById(UserId id) {
        Objects.requireNonNull(
                id,
                "UserId nao pode ser nulo."
        );

        return repository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        Objects.requireNonNull(
                email,
                "Email nao pode ser nulo."
        );

        return repository.findByEmail(email.value())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        Objects.requireNonNull(
                email,
                "Email nao pode ser nulo."
        );

        return repository.existsByEmail(email.value());
    }

    @Override
    @Transactional
    public void save(User user) {
        Objects.requireNonNull(
                user,
                "Usuario nao pode ser nulo."
        );

        UserJpaEntity entity = mapper.toEntity(user);

        repository.saveAndFlush(entity);
    }
}
