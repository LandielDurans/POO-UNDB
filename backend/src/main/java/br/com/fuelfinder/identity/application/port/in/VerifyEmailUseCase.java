package br.com.fuelfinder.identity.application.port.in;

public interface VerifyEmailUseCase {

    void execute(VerifyEmailCommand command);
}
