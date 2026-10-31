package br.com.fuelfinder.identity.application.port.in;

public interface ProvisionSuperAdminUseCase {

    /**
     * Cria a conta SUPER_ADMIN inicial, se ainda nao
     * existir nenhuma ativa.
     *
     * @return true se a conta foi criada agora
     */
    boolean execute(ProvisionSuperAdminCommand command);
}
