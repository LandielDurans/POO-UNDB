package br.com.fuelfinder.identity.adapter.in.scheduling;

import br.com.fuelfinder.identity.application.port.in.ProcessVerificationEmailOutboxUseCase;

import org.junit.jupiter.api.Test;

import static br.com.fuelfinder.identity.application.model.VerificationEmailOutboxProcessingResult.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerificationEmailOutboxWorkerTest {

    private final ProcessVerificationEmailOutboxUseCase useCase =
            mock(ProcessVerificationEmailOutboxUseCase.class);

    @Test
    void shouldProcessUntilNoEventIsLeft() {
        when(useCase.executeNext()).thenReturn(
                DISPATCH_REQUESTED,
                RETRY_SCHEDULED,
                NO_ACTION_REQUIRED,
                EXHAUSTED,
                NO_EVENT
        );

        assertEquals(4, worker(20).processPendingEvents());
        verify(useCase, times(5)).executeNext();
    }

    @Test
    void shouldDoNothingWhenOutboxIsEmpty() {
        when(useCase.executeNext()).thenReturn(NO_EVENT);

        assertEquals(0, worker(20).processPendingEvents());
        verify(useCase, times(1)).executeNext();
    }

    @Test
    void shouldStopAtMaxEventsPerRun() {
        when(useCase.executeNext()).thenReturn(DISPATCH_REQUESTED);

        assertEquals(3, worker(3).processPendingEvents());
        verify(useCase, times(3)).executeNext();
    }

    @Test
    void shouldStopRunOnInfrastructureFailure() {
        when(useCase.executeNext())
                .thenReturn(DISPATCH_REQUESTED)
                .thenThrow(new IllegalStateException("banco indisponivel"));

        // A falha nao escapa: o agendador continua
        // ativo e o lease expirado recupera o evento.
        assertEquals(1, worker(20).processPendingEvents());
        verify(useCase, times(2)).executeNext();
    }

    @Test
    void shouldRejectInvalidConfiguration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> worker(0)
        );

        assertThrows(
                NullPointerException.class,
                () -> new VerificationEmailOutboxWorker(null, 1)
        );
    }

    private VerificationEmailOutboxWorker worker(int maxEventsPerRun) {
        return new VerificationEmailOutboxWorker(
                useCase,
                maxEventsPerRun
        );
    }
}
