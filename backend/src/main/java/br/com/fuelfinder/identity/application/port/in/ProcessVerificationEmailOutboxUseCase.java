package br.com.fuelfinder.identity.application.port.in;

import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxProcessingResult;

public interface ProcessVerificationEmailOutboxUseCase {

    VerificationEmailOutboxProcessingResult executeNext();
}
