package br.com.fuelfinder.identity.adapter.config;

import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserUseCase;
import br.com.fuelfinder.identity.application.port.out.LocalRegistrationPersistence;
import br.com.fuelfinder.identity.application.port.out.PasswordHasher;
import br.com.fuelfinder.identity.application.port.out.UserRepository;
import br.com.fuelfinder.identity.application.service.PasswordPolicy;
import br.com.fuelfinder.identity.application.service.RegisterLocalUserService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class IdentityApplicationConfiguration {

    @Bean
    public PasswordPolicy passwordPolicy() {
        return new PasswordPolicy();
    }

    @Bean
    public RegisterLocalUserUseCase registerLocalUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            PasswordPolicy passwordPolicy,
            LocalRegistrationPersistence registrationPersistence,
            Clock clock
    ) {
        return new RegisterLocalUserService(
                userRepository,
                passwordHasher,
                passwordPolicy,
                registrationPersistence,
                clock
        );
    }
}
