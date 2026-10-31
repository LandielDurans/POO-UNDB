package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.model.UserProfile;
import br.com.fuelfinder.identity.application.port.in.GetUserProfileUseCase;
import br.com.fuelfinder.identity.application.port.out.UserRepository;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.util.Objects;
import java.util.Optional;

public final class GetUserProfileService implements GetUserProfileUseCase {

    private final UserRepository userRepository;

    public GetUserProfileService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @Override
    public Optional<UserProfile> execute(UserId userId) {
        Objects.requireNonNull(userId, "UserId nao pode ser nulo.");

        return userRepository.findById(userId)
                .filter(user -> user.getStatus() != UserStatus.DELETED)
                .map(UserProfile::of);
    }
}
