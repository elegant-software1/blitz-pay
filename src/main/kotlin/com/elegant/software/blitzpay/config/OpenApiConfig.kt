package com.elegant.software.blitzpay.config

import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(private val apiVersionProperties: ApiVersionProperties) {

    @Bean
    fun paymentsGroup(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("General")
            .pathsToMatch("/{version}/payments/**")
            .addOpenApiCustomizer { openApi ->
                openApi.info = Info().title("BlitzPay — General API")
                openApi.paths = rewriteVersionPaths(openApi.paths, apiVersionProperties.versions.payments)
                openApi.externalDocs = apiSchemaDocs("General")
            }
            .build()

    @Bean
    fun actuatorGroup(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("Actuator")
            .addOpenApiCustomizer { openApi ->
                openApi.externalDocs = apiSchemaDocs("Actuator")
            }
            .pathsToMatch("/actuator/**")
            .build()

    private fun apiSchemaDocs(groupName: String): ExternalDocumentation =
        ExternalDocumentation()
            .description("$groupName OpenAPI schema")
            .url("/api-docs/${encodeGroupName(groupName)}")

    private fun encodeGroupName(groupName: String): String = groupName.replace(" ", "%20")
}
