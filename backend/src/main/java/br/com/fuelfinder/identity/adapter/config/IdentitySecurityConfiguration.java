package br.com.fuelfinder.identity.adapter.config;

import br.com.fuelfinder.identity.adapter.security.ActiveSessionTokenValidator;
import br.com.fuelfinder.identity.adapter.security.JwtAccessTokenIssuer;
import br.com.fuelfinder.identity.application.port.out.AccessTokenIssuer;
import br.com.fuelfinder.identity.application.port.out.SessionRepository;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class IdentitySecurityConfiguration {

    // HS256 exige chave de ao menos 256 bits.
    private static final int MIN_SECRET_BYTES = 32;

    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    };

    private static final String[] API_DOCS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecretKey jwtSigningKey(
            @Value("${app.security.jwt.secret}") String secret
    ) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET deve ter ao menos "
                            + MIN_SECRET_BYTES + " bytes."
            );
        }

        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSigningKey,
            @Value("${app.security.jwt.issuer}") String issuer,
            SessionRepository sessionRepository,
            Clock clock
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Assinatura, emissor, validade e sessao ativa.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new ActiveSessionTokenValidator(sessionRepository, clock)
        ));

        return decoder;
    }

    @Bean
    public AccessTokenIssuer accessTokenIssuer(
            JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.access-token-ttl}") Duration ttl
    ) {
        return new JwtAccessTokenIssuer(jwtEncoder, issuer, ttl);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder
    ) throws Exception {
        JwtGrantedAuthoritiesConverter authorities =
                new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(JwtAccessTokenIssuer.ROLES_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authentication =
                new JwtAuthenticationConverter();
        authentication.setJwtGrantedAuthoritiesConverter(authorities);

        http
                // API sem cookies: o token vai no header,
                // entao CSRF nao se aplica.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_ENDPOINTS)
                        .permitAll()
                        .requestMatchers(API_DOCS).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(authentication))
                        .authenticationEntryPoint((request, response, failure) ->
                                writeProblem(
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Nao autenticado",
                                        "Envie um access token valido.",
                                        "UNAUTHENTICATED"
                                )))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, failure) ->
                                writeProblem(
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Nao autenticado",
                                        "Envie um access token valido.",
                                        "UNAUTHENTICATED"
                                ))
                        .accessDeniedHandler((request, response, denied) ->
                                writeProblem(
                                        response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "Acesso negado",
                                        "Seu perfil nao permite esta operacao.",
                                        "ACCESS_DENIED"
                                )));

        return http.build();
    }

    // Corpo fixo, sem dados da requisicao: nada a escapar.
    private static void writeProblem(
            HttpServletResponse response,
            int status,
            String title,
            String detail,
            String code
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if (status == HttpServletResponse.SC_UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }

        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,\
                "detail":"%s","code":"%s"}"""
                .formatted(title, status, detail, code));
    }
}
