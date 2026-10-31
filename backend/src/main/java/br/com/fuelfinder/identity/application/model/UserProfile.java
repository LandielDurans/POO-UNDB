package br.com.fuelfinder.identity.application.model;

import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.util.Objects;
import java.util.Set;

public record UserProfile(
        UserId id,
        String name,
        Email email,
        Set<UserRole> roles,
        UserStatus status
) {

    public UserProfile {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(email);
        roles = Set.copyOf(Objects.requireNonNull(roles));
        Objects.requireNonNull(status);
    }

    public static UserProfile of(User user) {
        return new UserProfile(
                user.getId(),
                user.getName(),
                user.getEmail().orElseThrow(),
                user.getRoles(),
                user.getStatus()
        );
    }
}
