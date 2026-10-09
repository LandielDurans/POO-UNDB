package br.com.fuelfinder.identity.application.port.out;

import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;

public interface VerificationTokenDigester {

    VerificationTokenDigest digest(String rawToken);
}
