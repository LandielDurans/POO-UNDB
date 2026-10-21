package br.com.fuelfinder.identity.adapter.out.persistence.jpa.mapper;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.UserJpaEntity;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.util.Objects;

public final class UserJpaMapper {

    public UserJpaEntity toEntity(User user) {
        Objects.requireNonNull(
                user,
                "Usuario nao pode ser nulo."
        );

        return new UserJpaEntity(
                user.getId().value(),
                user.getName(),
                user.getEmail()
                        .map(Email::value)
                        .orElse(null),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getRoles()
        );
    }

    public User toDomain(UserJpaEntity entity) {
        Objects.requireNonNull(
                entity,
                "Entidade JPA nao pode ser nula."
        );

        Email email = entity.getEmail() == null
                ? null
                : Email.of(entity.getEmail());

        return User.reconstitute(
                UserId.from(entity.getId()),
                entity.getName(),
                email,
                entity.getRoles(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}