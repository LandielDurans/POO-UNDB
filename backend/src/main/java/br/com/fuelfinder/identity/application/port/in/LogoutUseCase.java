package br.com.fuelfinder.identity.application.port.in;

public interface LogoutUseCase {

    /**
     * Encerra a sessao do refresh token informado.
     * Idempotente: token desconhecido nao gera erro.
     */
    void execute(String refreshToken);
}
