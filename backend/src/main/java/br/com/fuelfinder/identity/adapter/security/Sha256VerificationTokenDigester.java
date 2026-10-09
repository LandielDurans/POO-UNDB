package br.com.fuelfinder.identity.adapter.security;

import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.out.VerificationTokenDigester;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class Sha256VerificationTokenDigester
        implements VerificationTokenDigester {

    @Override
    public VerificationTokenDigest digest(String rawToken) {
        Objects.requireNonNull(
                rawToken,
                "Token nao pode ser nulo."
        );

        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance("SHA-256");

            byte[] tokenBytes =
                    rawToken.getBytes(StandardCharsets.UTF_8);

            byte[] hashBytes =
                    messageDigest.digest(tokenBytes);

            String hexadecimal =
                    HexFormat.of().formatHex(hashBytes);

            return new VerificationTokenDigest(hexadecimal);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Algoritmo SHA-256 indisponivel.",
                    exception
            );
        }
    }
}
