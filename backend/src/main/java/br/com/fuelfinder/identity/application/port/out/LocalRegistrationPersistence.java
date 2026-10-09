package br.com.fuelfinder.identity.application.port.out;

import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.domain.model.User;

public interface LocalRegistrationPersistence {

    /**
     * 1. Persistir o usuario pendente
     * 2. Persistir o hash da credencial local
     * 3. Registrar um evento duravel de verificacao de email
     *
     * Deve garantir a unicidade do email no armazenamento
     * Em caso de falha ou erro, nenhuma gravacao parcial deve permanecer
     */

    void register(User user, PasswordHash passwordHash);
}
