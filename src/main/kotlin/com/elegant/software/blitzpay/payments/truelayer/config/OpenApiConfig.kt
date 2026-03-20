package com.elegant.software.blitzpay.payments.truelayer.config

import com.elegant.software.blitzpay.config.ApiVersionProperties
import com.elegant.software.blitzpay.config.rewriteVersionPaths
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TruelayerOpenApiConfig(private val apiVersionProperties: ApiVersionProperties) {

    @Bean
    fun truelayerApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("TrueLayer")
            .packagesToScan("com.elegant.software.blitzpay.payments.truelayer")
            .pathsToMatch("/{version}/webhooks/truelayer/**")
            .addOpenApiCustomizer { openApi ->
                openApi.info = Info().title("BlitzPay — TrueLayer API").version("v${apiVersionProperties.versions.truelayer}")
                openApi.paths = rewriteVersionPaths(openApi.paths, apiVersionProperties.versions.truelayer)
                openApi.externalDocs = ExternalDocumentation()
                    .description("TrueLayer OpenAPI schema")
                    .url("/api-docs/TrueLayer")
            }
            .build()
}
