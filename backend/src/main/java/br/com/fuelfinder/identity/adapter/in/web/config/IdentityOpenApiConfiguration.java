package br.com.fuelfinder.identity.adapter.in.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdentityOpenApiConfiguration {

    @Bean
    public OpenAPI identityOpenApi() {
        return new OpenAPI().info(
                new Info()
                        .title("FuelFinder Identity API")
                        .version("0.1.0")
                        .description(
                                "Registro local e verificacao de contas na API"
                        )
        );
    }
}
