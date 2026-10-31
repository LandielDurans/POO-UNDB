package br.com.fuelfinder.identity.adapter.config;

import br.com.fuelfinder.identity.adapter.in.scheduling.VerificationEmailOutboxWorker;
import br.com.fuelfinder.identity.application.port.in.ProcessVerificationEmailOutboxUseCase;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdentitySchedulingConfigurationTest {

    private final ApplicationContextRunner context =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            IdentitySchedulingConfiguration.class
                    )
                    .withBean(
                            ProcessVerificationEmailOutboxUseCase.class,
                            () -> mock(
                                    ProcessVerificationEmailOutboxUseCase.class
                            )
                    );

    @Test
    void shouldStartWorkerWhenEnabled() {
        context.withPropertyValues("app.identity.outbox.enabled=true")
                .run(ctx -> assertThat(ctx)
                        .hasSingleBean(VerificationEmailOutboxWorker.class));
    }

    @Test
    void shouldNotStartWorkerWhenDisabled() {
        context.withPropertyValues("app.identity.outbox.enabled=false")
                .run(ctx -> assertThat(ctx)
                        .doesNotHaveBean(VerificationEmailOutboxWorker.class));
    }

    @Test
    void shouldNotStartWorkerWhenPropertyIsMissing() {
        context.run(ctx -> assertThat(ctx)
                .doesNotHaveBean(VerificationEmailOutboxWorker.class));
    }
}
