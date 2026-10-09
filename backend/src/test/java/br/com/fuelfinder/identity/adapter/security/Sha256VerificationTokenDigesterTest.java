
package br.com.fuelfinder.identity.adapter.security;

import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Sha256VerificationTokenDigesterTest {

    private final Sha256VerificationTokenDigester digester =
            new Sha256VerificationTokenDigester();

    @Test
    void shouldCalculateKnownSha256Digest() {
        VerificationTokenDigest digest =
                digester.digest("abc");

        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223"
                        + "b00361a396177a9cb410ff61f20015ad",
                digest.value()
        );
    }

    @Test
    void shouldSupportUtf8Characters() {
        VerificationTokenDigest digest =
                digester.digest("á");

        assertEquals(
                "fb9778719d93551b1c88df5f1ab229f8"
                        + "3ea30af112fc51017c9f0a383dcf6cb3",
                digest.value()
        );
    }

    @Test
    void shouldPreserveExactTokenContent() {
        VerificationTokenDigest first =
                digester.digest("abc");

        VerificationTokenDigest second =
                digester.digest("abc ");

        assertNotEquals(first, second);
    }

    @Test
    void shouldRejectNullToken() {
        assertThrows(
                NullPointerException.class,
                () -> digester.digest(null)
        );
    }
}
