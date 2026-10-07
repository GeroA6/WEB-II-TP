package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata general que aparece en Swagger UI (título, descripción, versión).
 * La documentación de cada endpoint puntual va con @Operation en el
 * controller correspondiente.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI demoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TP2 · Persistencia, listas y arquitectura hexagonal")
                        .description("Catálogo de productos consumido desde DummyJSON, favoritos persistidos "
                                + "en PostgreSQL y listas para organizarlos.")
                        .version("v2"));
    }
}
