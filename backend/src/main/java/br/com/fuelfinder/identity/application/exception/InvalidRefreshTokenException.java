package br.com.fuelfinder.identity.application.exception;

public final class InvalidRefreshTokenException
        extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Sessao invalida ou expirada.");
    }
}
