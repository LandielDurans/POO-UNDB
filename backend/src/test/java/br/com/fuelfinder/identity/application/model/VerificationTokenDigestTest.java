
package br.com.fuelfinder.identity.application.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerificationTokenDigestTest {

    @Test
    void shouldAcceptValidDigest() {
        String value = "a".repeat(64);

        VerificationTokenDigest digest =
                new VerificationTokenDigest(value);

        assertEquals(value, digest.value());
    }

    @Test
    void shouldRejectNullDigest() {
        assertThrows(
                NullPointerException.class,
                () -> new VerificationTokenDigest(null)
        );
    }

    @Test
    void shouldRejectBlankDigest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerificationTokenDigest(" ".repeat(64))
        );
    }

    @Test
    void shouldRejectWrongLength() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerificationTokenDigest("a".repeat(63))
        );
    }

    @Test
    void shouldRejectNonHexCharacters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerificationTokenDigest("g".repeat(64))
        );
    }

    @Test
    void shouldNotExposeDigestInToString() {
        String value = "a".repeat(64);

        VerificationTokenDigest digest =
                new VerificationTokenDigest(value);

        assertFalse(digest.toString().contains(value));

        assertEquals(
                "VerificationTokenDigest[REDACTED]",
                digest.toString()
        );
    }
}
