package br.com.fuelfinder.identity.adapter.config;

import br.com.fuelfinder.identity.adapter.in.startup.SuperAdminProvisioningRunner;
import br.com.fuelfinder.identity.adapter.out.email.SmtpVerificationEmailSender;
import br.com.fuelfinder.identity.adapter.security.SecureRandomVerificationTokenGenerator;
import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.in.DispatchVerificationEmailUseCase;
import br.com.fuelfinder.identity.application.port.in.GetUserProfileUseCase;
import br.com.fuelfinder.identity.application.port.in.LoginUseCase;
import br.com.fuelfinder.identity.application.port.in.PrepareEmailVerificationUseCase;
import br.com.fuelfinder.identity.application.port.in.ProcessVerificationEmailOutboxUseCase;
import br.com.fuelfinder.identity.application.port.in.ProvisionSuperAdminUseCase;
import br.com.fuelfinder.identity.application.port.in.RefreshSessionUseCase;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserUseCase;
import br.com.fuelfinder.identity.application.port.out.*;
import br.com.fuelfinder.identity.application.service.DispatchVerificationEmailService;
import br.com.fuelfinder.identity.application.service.GetUserProfileService;
import br.com.fuelfinder.identity.application.service.LoginService;
import br.com.fuelfinder.identity.application.service.LogoutService;
import br.com.fuelfinder.identity.application.service.PasswordPolicy;
import br.com.fuelfinder.identity.application.service.PrepareEmailVerificationService;
import br.com.fuelfinder.identity.application.service.ProcessVerificationEmailOutboxService;
import br.com.fuelfinder.identity.application.service.ProvisionSuperAdminService;
import br.com.fuelfinder.identity.application.service.RefreshSessionService;
import br.com.fuelfinder.identity.application.service.SessionTokenFactory;
import br.com.fuelfinder.identity.application.service.RegisterLocalUserService;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailUseCase;
import br.com.fuelfinder.identity.application.service.VerifyEmailService;
import br.com.fuelfinder.identity.adapter.security.Sha256VerificationTokenDigester;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Clock;
import java.time.Duration;

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

    @Bean
    public VerificationEmailSender verificationEmailSender(
            JavaMailSender mailSender,
            @Value("${app.identity.verification-email.from}")
            String from,
            @Value("${app.identity.verification-email.link-base-url}")
            String linkBaseUrl
    ) {
        return new SmtpVerificationEmailSender(
                mailSender,
                from,
                linkBaseUrl
        );
    }

    @Bean
    public DispatchVerificationEmailUseCase
    dispatchVerificationEmailUseCase(
            PrepareEmailVerificationUseCase preparation,
            VerificationEmailSender emailSender
    ) {
        return new DispatchVerificationEmailService(
                preparation,
                emailSender
        );
    }

    @Bean
    public ProcessVerificationEmailOutboxUseCase
    processVerificationEmailOutboxUseCase(
            VerificationEmailOutbox outbox,
            DispatchVerificationEmailUseCase dispatcher,
            Clock clock
    ) {
        return new ProcessVerificationEmailOutboxService(
                outbox,
                dispatcher,
                clock
        );
    }

    @Bean
    public SessionTokenFactory sessionTokenFactory(
            VerificationTokenGenerator tokenGenerator,
            VerificationTokenDigester tokenDigester,
            @Value("${app.security.refresh-token-ttl}")
            Duration refreshTokenTtl
    ) {
        return new SessionTokenFactory(
                tokenGenerator,
                tokenDigester,
                refreshTokenTtl
        );
    }

    @Bean
    public LoginUseCase loginUseCase(
            UserRepository userRepository,
            LocalCredentialRepository credentialRepository,
            PasswordHasher passwordHasher,
            SessionRepository sessionRepository,
            SessionTokenFactory tokenFactory,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock
    ) {
        return new LoginService(
                userRepository,
                credentialRepository,
                passwordHasher,
                sessionRepository,
                tokenFactory,
                accessTokenIssuer,
                clock
        );
    }

    @Bean
    public RefreshSessionUseCase refreshSessionUseCase(
            SessionRepository sessionRepository,
            UserRepository userRepository,
            SessionTokenFactory tokenFactory,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock
    ) {
        return new RefreshSessionService(
                sessionRepository,
                userRepository,
                tokenFactory,
                accessTokenIssuer,
                clock
        );
    }

    // Implementa LogoutUseCase e LogoutAllSessionsUseCase.
    @Bean
    public LogoutService logoutService(
            SessionRepository sessionRepository,
            SessionTokenFactory tokenFactory,
            Clock clock
    ) {
        return new LogoutService(sessionRepository, tokenFactory, clock);
    }

    @Bean
    public GetUserProfileUseCase getUserProfileUseCase(
            UserRepository userRepository
    ) {
        return new GetUserProfileService(userRepository);
    }

    @Bean
    public ProvisionSuperAdminUseCase provisionSuperAdminUseCase(
            PrivilegedAccountPersistence persistence,
            UserRepository userRepository,
            PasswordPolicy passwordPolicy,
            PasswordHasher passwordHasher,
            Clock clock
    ) {
        return new ProvisionSuperAdminService(
                persistence,
                userRepository,
                passwordPolicy,
                passwordHasher,
                clock
        );
    }

    @Bean
    public SuperAdminProvisioningRunner superAdminProvisioningRunner(
            ProvisionSuperAdminUseCase useCase,
            @Value("${app.identity.super-admin.name}") String name,
            @Value("${app.identity.super-admin.email}") String email,
            @Value("${app.identity.super-admin.password}") String password
    ) {
        return new SuperAdminProvisioningRunner(useCase, name, email, password);
    }

}
