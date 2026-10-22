package br.com.fuelfinder.shared.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Objects;

@Configuration(proxyBeanMethods = false)
public class WebCorsConfiguration implements WebMvcConfigurer {

    private static final long PREFLIGHT_CACHE_SECONDS = 3600;

    private final String[] allowedOrigins;

    public WebCorsConfiguration(
            @Value("${app.cors.allowed-origins:http://localhost:3000}")
            String allowedOrigins
    ) {
        Objects.requireNonNull(
                allowedOrigins,
                "Origens permitidas nao podem ser nulas."
        );

        // Lista separada por virgula, ex.:
        // http://localhost:3000,https://app.fuelfinder.com.br
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
                .allowedHeaders("*")
                .maxAge(PREFLIGHT_CACHE_SECONDS);
    }
}
