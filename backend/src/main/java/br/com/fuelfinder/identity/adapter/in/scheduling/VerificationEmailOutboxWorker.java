package br.com.fuelfinder.identity.adapter.in.scheduling;

import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxProcessingResult;
import br.com.fuelfinder.identity.application.port.in.ProcessVerificationEmailOutboxUseCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;

public class VerificationEmailOutboxWorker {

    private static final Logger log =
            LoggerFactory.getLogger(VerificationEmailOutboxWorker.class);

    private final ProcessVerificationEmailOutboxUseCase useCase;
    private final int maxEventsPerRun;

    public VerificationEmailOutboxWorker(
            ProcessVerificationEmailOutboxUseCase useCase,
            int maxEventsPerRun
    ) {
        this.useCase = Objects.requireNonNull(useCase);

        if (maxEventsPerRun < 1) {
            throw new IllegalArgumentException(
                    "Limite de eventos por execucao deve ser positivo."
            );
        }

        this.maxEventsPerRun = maxEventsPerRun;
    }

    /**
     * Processa ate maxEventsPerRun eventos por execucao.
     *
     * Cada evento e reservado, enviado e concluido em
     * transacoes separadas: o e-mail nunca e enviado
     * com uma transacao do banco aberta.
     *
     * Falhas de envio ja sao tratadas pelo caso de uso
     * (retry ou esgotamento). Uma excecao aqui indica
     * falha de infraestrutura; a execucao para e o
     * lease expirado permite recuperar o evento depois.
     *
     * @return quantidade de eventos processados
     */
    @Scheduled(
            initialDelayString =
                    "${app.identity.outbox.initial-delay-ms:10000}",
            fixedDelayString =
                    "${app.identity.outbox.poll-interval-ms:5000}"
    )
    public int processPendingEvents() {
        int processed = 0;

        while (processed < maxEventsPerRun) {
            final VerificationEmailOutboxProcessingResult result;

            try {
                result = useCase.executeNext();
            } catch (RuntimeException failure) {
                // Sem a mensagem: pode conter o endereco
                // de e-mail do destinatario.
                log.warn(
                        "Falha ao processar evento da outbox de "
                                + "verificacao ({}).",
                        failure.getClass().getSimpleName()
                );
                log.debug("Detalhes da falha da outbox.", failure);
                break;
            }

            if (result
                    == VerificationEmailOutboxProcessingResult.NO_EVENT) {
                break;
            }

            processed++;
        }

        return processed;
    }
}
