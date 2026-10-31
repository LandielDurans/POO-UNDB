package br.com.fuelfinder.identity.adapter.in.startup;

import br.com.fuelfinder.identity.application.port.in.ProvisionSuperAdminCommand;
import br.com.fuelfinder.identity.application.port.in.ProvisionSuperAdminUseCase;
import br.com.fuelfinder.identity.domain.valueobject.Email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Objects;

/**
 * Cria a conta SUPER_ADMIN inicial na subida da
 * aplicacao, a partir das variaveis de ambiente.
 *
 * Sem SUPER_ADMIN_EMAIL, nada e feito. Depois que a
 * conta existe, novas subidas nao alteram nada.
 */
public class SuperAdminProvisioningRunner implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(SuperAdminProvisioningRunner.class);

    private final ProvisionSuperAdminUseCase useCase;
    private final String name;
    private final String email;
    private final String password;

    public SuperAdminProvisioningRunner(
            ProvisionSuperAdminUseCase useCase,
            String name,
            String email,
            String password
    ) {
        this.useCase = Objects.requireNonNull(useCase);
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()) {
            log.info("SUPER_ADMIN_EMAIL nao definido; provisionamento ignorado.");
            return;
        }

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "SUPER_ADMIN_PASSWORD e obrigatorio quando "
                            + "SUPER_ADMIN_EMAIL esta definido."
            );
        }

        boolean created = useCase.execute(
                new ProvisionSuperAdminCommand(name, Email.of(email), password)
        );

        log.info(created
                ? "Conta SUPER_ADMIN criada."
                : "Conta SUPER_ADMIN ja existe; nada a fazer.");
    }
}
