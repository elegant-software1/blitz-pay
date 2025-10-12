package com.elegant.software.quickpay.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Value("\${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/quickpay}")
    private lateinit var keycloakIssuerUri: String

    @Bean
    fun customOpenAPI(): OpenAPI {
        val authUrl = "$keycloakIssuerUri/protocol/openid-connect/auth"
        val tokenUrl = "$keycloakIssuerUri/protocol/openid-connect/token"

        return OpenAPI()
            .components(
                Components()
                    .addSecuritySchemes(
                        "keycloak_oauth2",
                        SecurityScheme()
                            .type(SecurityScheme.Type.OAUTH2)
                            .flows(
                                OAuthFlows()
                                    .authorizationCode(
                                        OAuthFlow()
                                            .authorizationUrl(authUrl)
                                            .tokenUrl(tokenUrl)
                                    )
                            )
                    )
                    .addSecuritySchemes(
                        "bearer_jwt",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
            .addSecurityItem(SecurityRequirement().addList("bearer_jwt"))
    }

    @Bean
    fun paymentsGroup(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("payments")
            // adjust to your controllers' paths
            .pathsToMatch("/payments/**")
            .build()

    @Bean
    fun actuatorGroup(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("actuator")
            .pathsToMatch("/actuator/**")
            .build()
}
