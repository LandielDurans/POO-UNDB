package br.com.fuelfinder.identity.application.exception;

public final class AccountNotVerifiedException
        extends RuntimeException {

    public AccountNotVerifiedException() {
        super("Conta pendente de verificacao de e-mail.");
    }
}
