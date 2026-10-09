package br.com.fuelfinder.identity.application.exception;

public final class InvalidEmailVerificationTokenException
        extends RuntimeException {

    public InvalidEmailVerificationTokenException(){
        super("Token de verificacao invalido ou expirado.");
    }
}
