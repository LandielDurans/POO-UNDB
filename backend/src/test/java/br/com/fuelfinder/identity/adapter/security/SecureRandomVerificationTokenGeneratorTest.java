package br.com.fuelfinder.identity.adapter.security;

import br.com.fuelfinder.identity.application.port.in.VerifyEmailCommand;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SecureRandomVerificationTokenGeneratorTest {

    private final SecureRandomVerificationTokenGenerator generator =
            new SecureRandomVerificationTokenGenerator();

    @Test
    void shouldGenerateUrlSafeTokenWith32Bytes() {
        String token = generator.generate();

        assertEquals(43, token.length());
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));

        byte[] decoded =
                Base64.getUrlDecoder().decode(token);

        assertEquals(32, decoded.length);
    }

    @Test
    void shouldGenerateDifferentTokens() {
        String first = generator.generate();
        String second = generator.generate();

        assertNotEquals(first, second);
    }

    @Test
    void shouldGenerateTokenAcceptedByVerificationCommand() {
        String token = generator.generate();

        assertDoesNotThrow(
                () -> new VerifyEmailCommand(token)
        );
    }
}
