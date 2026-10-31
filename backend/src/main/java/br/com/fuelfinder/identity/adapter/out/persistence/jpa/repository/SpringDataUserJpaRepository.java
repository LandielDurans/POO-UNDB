package br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.UserJpaEntity;

import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserJpaRepository
        extends JpaRepository<UserJpaEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<UserJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = "roles")
    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select count(u) > 0
            from UserJpaEntity u join u.roles r
            where r = :role and u.status = :status
            """)
    boolean existsByRoleAndStatus(
            @Param("role") UserRole role,
            @Param("status") UserStatus status
    );
}
