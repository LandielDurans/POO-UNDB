package br.com.fuelfinder.identity.application.port.out;

import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    void save(User user);
}
