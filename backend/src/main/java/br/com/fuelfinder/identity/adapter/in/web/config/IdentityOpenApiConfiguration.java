package br.com.fuelfinder.identity.adapter.in.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdentityOpenApiConfiguration {

    @Bean
    public OpenAPI identityOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("FuelFinder Identity API")
                                .version("0.2.0")
                                .description(
                                        "Cadastro, verificacao de e-mail e sessoes"
                                )
                )
                .components(
                        new Components().addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }
}
