package br.com.fuelfinder.identity.adapter.out.persistence.jpa.repository;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity.LocalCredentialJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataLocalCredentialJpaRepository
        extends JpaRepository<LocalCredentialJpaEntity, UUID> {
}
