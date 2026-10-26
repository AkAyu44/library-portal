package kz.iitu.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lab 7: the header of the OpenAPI document — what the code itself cannot say.
 * Paths, methods and schemas are generated from the controllers and the DTOs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryPortalOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Library Portal API")
                .version("1.0.0")
                .description("REST API of the book catalogue — RWPSF 3305, IITU, laboratory works 7-8 (PostgreSQL, paging). "
                        + "Errors are RFC 9457 problem details (application/problem+json)."));
    }
}
