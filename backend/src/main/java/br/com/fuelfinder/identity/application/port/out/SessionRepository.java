package br.com.fuelfinder.identity.application.port.out;

import br.com.fuelfinder.identity.application.model.ActiveSession;
import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Sessoes de autenticacao.
 *
 * Refresh tokens sao identificados apenas pelo
 * SHA-256 (mesmo digest usado nos tokens de
 * verificacao: ambos sao tokens aleatorios de
 * alta entropia).
 */
public interface SessionRepository {

    void create(
            UUID sessionId,
            UserId userId,
            VerificationTokenDigest refreshTokenDigest,
            Instant createdAt,
            Instant expiresAt
    );

    /**
     * Substitui atomicamente o digest de uma sessao
     * ativa e nao expirada, renovando sua validade.
     *
     * Retorna vazio se o token nao corresponder a
     * uma sessao ativa (desconhecido, ja trocado,
     * revogado ou expirado).
     */
    Optional<ActiveSession> rotate(
            VerificationTokenDigest currentDigest,
            VerificationTokenDigest nextDigest,
            Instant now,
            Instant expiresAt
    );

    boolean revoke(VerificationTokenDigest refreshTokenDigest, Instant now);

    boolean revokeById(UUID sessionId, Instant now);

    int revokeAllForUser(UserId userId, Instant now);

    boolean isActive(UUID sessionId, Instant now);
}
