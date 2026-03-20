package com.elegant.software.blitzpay.payments.support.config


import io.swagger.v3.oas.models.ExternalDocumentation
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SupportOpenApiConfig {
    @Bean
    fun supportApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("Support")
            .packagesToScan("com.elegant.software.blitzpay.payments.support")
            .pathsToMatch("/support/**")
            .addOpenApiCustomizer { openApi ->
                openApi.externalDocs = ExternalDocumentation()
                    .description("Support OpenAPI schema")
                    .url("/api-docs/Support")
            }
            .build()
}
