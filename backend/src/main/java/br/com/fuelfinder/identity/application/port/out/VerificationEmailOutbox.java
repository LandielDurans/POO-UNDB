package br.com.fuelfinder.identity.application.port.out;

import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxClaim;
import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationEmailOutbox {

    /**
     * Reserva atomicamente um evento disponivel.
     *
     * A implementacao deve:
     * - Selecionar somente eventos aptos ao processamento.
     * - Impedir reservas simultaneas do mesmo evento.
     * - Gerar um novo leaseId para cada reserva.
     * - Incrementar attemptNumber.
     * - Registrar a expiracao da reserva.
     *
     * Eventos com reservas expiradas podem ser
     * reservados novamente.
     *
     * Retorna vazio quando nao houver eventos elegiveis.
     */
    Optional<VerificationEmailOutboxClaim> claimNext(
            Instant now,
            Duration leaseDuration
    );

    /**
     * Finaliza um evento processado.
     *
     * Aceita a operacao somente se:
     * - O evento estiver reservado.
     * - O leaseId corresponder a reserva atual.
     * - A reserva ainda estiver valida.
     *
     * O resultado diferencia envio solicitado
     * de processamento desnecessario.
     *
     * Retorna false para reservas invalidas
     * ou expiradas.
     */
    boolean complete(
            UUID eventId,
            UUID leaseId,
            VerificationEmailOutboxResult result,
            Instant completedAt
    );

    /**
     * Agenda uma nova tentativa apos falha.
     *
     * Deve liberar a reserva atual e impedir
     * nova execucao antes de nextAttemptAt.
     *
     * nextAttemptAt deve ser posterior a failedAt.
     *
     * A alteracao exige reserva valida.
     */
    boolean scheduleRetry(
            UUID eventId,
            UUID leaseId,
            Instant failedAt,
            Instant nextAttemptAt
    );

    /**
     * Encerra um evento que atingiu o limite
     * maximo de tentativas.
     *
     * O evento permanece armazenado para
     * diagnostico e auditoria operacional.
     *
     * A alteracao exige reserva valida.
     */
    boolean markExhausted(
            UUID eventId,
            UUID leaseId,
            Instant failedAt
    );
}
