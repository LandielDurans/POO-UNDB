package br.com.fuelfinder.identity.application.port.out;

import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.time.Instant;

public interface EmailVerificationTokenIssuer {

    /**
     * Registra um token de verificacao.
     *
     * A implementacao deve:
     * - Verificar atomicamente se o usuario
     *   continua PENDING_VERIFICATION.
     * - Persistir somente o digest.
     * - Registrar emissao e expiracao.
     * - Garantir expiresAt posterior a issuedAt.
     * - Garantir unicidade do digest.
     *
     * Retorna false caso a conta nao exista
     * ou nao esteja mais pendente.
     *
     * Tokens anteriormente emitidos nao devem
     * ser revogados apenas por uma repeticao
     * do processamento da outbox.
     *
     * Na ativacao, todos os tokens restantes
     * dessa conta deverao ser invalidados.
     */
    boolean issueIfPending(
            UserId userId,
            VerificationTokenDigest digest,
            Instant issuedAt,
            Instant expiresAt
    );
}
