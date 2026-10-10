package br.com.fuelfinder.identity.adapter.out.persistence.jpa.config;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.mapper.UserJpaMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class IdentityJpaConfiguration {

    @Bean
    public UserJpaMapper userJpaMapper() {
        return new UserJpaMapper();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
