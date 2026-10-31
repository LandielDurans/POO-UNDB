package br.com.fuelfinder.identity.adapter.config;

import br.com.fuelfinder.identity.adapter.in.scheduling.VerificationEmailOutboxWorker;
import br.com.fuelfinder.identity.application.port.in.ProcessVerificationEmailOutboxUseCase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Processamento automatico da outbox de verificacao.
 *
 * Desligado com app.identity.outbox.enabled=false
 * (ex.: nos testes, para que um worker em segundo
 * plano nao dispute eventos com o teste).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        name = "app.identity.outbox.enabled",
        havingValue = "true"
)
public class IdentitySchedulingConfiguration {

    @Bean
    public VerificationEmailOutboxWorker verificationEmailOutboxWorker(
            ProcessVerificationEmailOutboxUseCase useCase,
            @Value("${app.identity.outbox.max-events-per-run:20}")
            int maxEventsPerRun
    ) {
        return new VerificationEmailOutboxWorker(
                useCase,
                maxEventsPerRun
        );
    }
}
