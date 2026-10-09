package br.com.fuelfinder.identity.application.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record VerificationTokenDigest(String value) {

    private static final Pattern SHA256_HEX =
            Pattern.compile("[0-9a-f]{64}");

    public VerificationTokenDigest {
        Objects.requireNonNull(
                value,
                "Digest do token nao pode ser nulo."
        );

        if (!SHA256_HEX.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Digest do token deve ser SHA-256 hexadecimal."
            );
        }
    }

    @Override
    public String toString() {
        return "VerificationTokenDigest[REDACTED]";
    }
}
