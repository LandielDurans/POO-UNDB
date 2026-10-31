package br.com.fuelfinder.identity.adapter.out.persistence.jpa;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.LocalCredentialJpaEntity;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.mapper.UserJpaMapper;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository.SpringDataLocalCredentialJpaRepository;
import br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository.SpringDataUserJpaRepository;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.port.out.PrivilegedAccountPersistence;
import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.model.User;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Repository
public class PrivilegedAccountPersistenceAdapter
        implements PrivilegedAccountPersistence {

    private final SpringDataUserJpaRepository userRepository;
    private final SpringDataLocalCredentialJpaRepository credentialRepository;
    private final UserJpaMapper userMapper;

    public PrivilegedAccountPersistenceAdapter(
            SpringDataUserJpaRepository userRepository,
            SpringDataLocalCredentialJpaRepository credentialRepository,
            UserJpaMapper userMapper
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.credentialRepository = Objects.requireNonNull(credentialRepository);
        this.userMapper = Objects.requireNonNull(userMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveSuperAdmin() {
        return userRepository.existsByRoleAndStatus(
                UserRole.SUPER_ADMIN,
                UserStatus.ACTIVE
        );
    }

    @Override
    @Transactional
    public void createActiveAccount(User user, PasswordHash passwordHash) {
        Objects.requireNonNull(user, "Usuario nao pode ser nulo.");
        Objects.requireNonNull(passwordHash, "PasswordHash nao pode ser nulo.");

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Conta privilegiada deve nascer ativa."
            );
        }

        userRepository.saveAndFlush(userMapper.toEntity(user));

        credentialRepository.saveAndFlush(
                new LocalCredentialJpaEntity(
                        user.getId().value(),
                        passwordHash.value(),
                        user.getCreatedAt()
                )
        );
    }
}
