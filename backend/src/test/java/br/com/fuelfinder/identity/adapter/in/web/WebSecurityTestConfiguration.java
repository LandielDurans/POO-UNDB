package br.com.fuelfinder.identity.adapter.in.web;

import br.com.fuelfinder.identity.adapter.config.IdentitySecurityConfiguration;
import br.com.fuelfinder.identity.application.port.out.SessionRepository;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regras reais de seguranca para testes @WebMvcTest.
 *
 * Exige @ActiveProfiles("test") para a chave JWT.
 * Toda sessao e considerada ativa; os testes de
 * integracao cobrem a checagem real no banco.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import(IdentitySecurityConfiguration.class)
public class WebSecurityTestConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SessionRepository sessionRepository() {
        SessionRepository sessions = mock(SessionRepository.class);
        when(sessions.isActive(any(), any())).thenReturn(true);
        return sessions;
    }
}
