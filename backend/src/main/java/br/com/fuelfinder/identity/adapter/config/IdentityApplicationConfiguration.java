package br.com.fuelfinder.identity.adapter.config;

import br.com.fuelfinder.identity.adapter.security.SecureRandomVerificationTokenGenerator;
import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.in.PrepareEmailVerificationUseCase;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserUseCase;
import br.com.fuelfinder.identity.application.port.out.*;
import br.com.fuelfinder.identity.application.service.PasswordPolicy;
import br.com.fuelfinder.identity.application.service.PrepareEmailVerificationService;
import br.com.fuelfinder.identity.application.service.RegisterLocalUserService;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailUseCase;
import br.com.fuelfinder.identity.application.service.VerifyEmailService;
import br.com.fuelfinder.identity.adapter.security.Sha256VerificationTokenDigester;

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

    @Bean
    public VerificationTokenDigester verificationTokenDigester() {
        return new Sha256VerificationTokenDigester();
    }

    @Bean
    public VerifyEmailUseCase verifyEmailUseCase(
            VerificationTokenDigester digester,
            EmailVerificationPersistence persistence,
            Clock clock
    ) {
        return new VerifyEmailService(
                digester,
                persistence,
                clock
        );
    }

    @Bean
    public VerificationTokenGenerator verificationTokenGenerator() {
        return new SecureRandomVerificationTokenGenerator();
    }

    @Bean
    public PrepareEmailVerificationUseCase
    prepareEmailVerificationUseCase(
            UserRepository userRepository,
            VerificationTokenGenerator tokenGenerator,
            VerificationTokenDigester tokenDigester,
            EmailVerificationTokenIssuer tokenIssuer,
            Clock clock
    ) {
        return new PrepareEmailVerificationService(
                userRepository,
                tokenGenerator,
                tokenDigester,
                tokenIssuer,
                clock
        );
    }

}
