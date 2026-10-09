package br.com.fuelfinder.identity.application.port.out;

import br.com.fuelfinder.identity.application.model.PreparedVerificationEmail;

public interface VerificationEmailSender {

    void send(PreparedVerificationEmail email);
}
