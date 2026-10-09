package br.com.fuelfinder.identity.application.port.out;

public interface VerificationTokenGenerator {
    /**
     * Gera um token criptograficamente
     *
     * O valor original deve ser armazenado com seguranca
     */

    String generate();
}
