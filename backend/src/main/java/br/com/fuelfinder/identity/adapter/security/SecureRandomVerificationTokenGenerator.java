package br.com.fuelfinder.identity.adapter.security;

import br.com.fuelfinder.identity.application.port.out.VerificationTokenGenerator;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecureRandomVerificationTokenGenerator
        implements VerificationTokenGenerator {

    private static final int TOKEN_SIZE_BYTES = 32;

    private final SecureRandom secureRandom =
            new SecureRandom();

    @Override
    public String generate() {
        byte[] randomBytes =
                new byte[TOKEN_SIZE_BYTES];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}
