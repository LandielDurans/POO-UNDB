package br.com.fuelfinder.identity.application.exception;

public final class EmailAlreadyRegisteredException
        extends RuntimeException {
    public EmailAlreadyRegisteredException(){
        super("Nao foi possivel cadastrar esse email.");
    }

}
