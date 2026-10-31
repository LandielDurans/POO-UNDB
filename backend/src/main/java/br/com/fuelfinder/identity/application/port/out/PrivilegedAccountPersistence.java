package br.com.fuelfinder.identity.application.port.out;

import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.domain.model.User;

public interface PrivilegedAccountPersistence {

    boolean existsActiveSuperAdmin();

    /**
     * Grava usuario ativo e credencial local na mesma
     * transacao, sem evento de verificacao de e-mail.
     */
    void createActiveAccount(User user, PasswordHash passwordHash);
}
