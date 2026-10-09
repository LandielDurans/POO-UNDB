package br.com.fuelfinder.identity.application.port.out;

import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;

import java.time.Instant;

public interface EmailVerificationPersistence {

    /**
     * Valida e consome o token.
     *
     * Deve verificar:
     * - O digest corresponde a um token existente.
     * - O token ainda nao foi consumido.
     * - O token nao expirou.
     * - O usuario esta pendente de verificacao.
     *
     * Em caso de sucesso:
     * - Ativa o usuario.
     * - Consome o token definitivamente.
     *
     * Todas as alteracoes devem ocorrer na mesma transacao.
     *
     * @return true se a verificacao foi concluida;
     *         false se o token nao pode ser utilizado.
     */
    boolean consumeAndActivate(
            VerificationTokenDigest digest,
            Instant verifiedAt
    );
}
