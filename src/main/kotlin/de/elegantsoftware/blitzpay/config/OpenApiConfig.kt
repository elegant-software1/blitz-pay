package de.elegantsoftware.blitzpay.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("BlitzPay API")
                    .version("1.0")
                    .description("BlitzPay Payment Service")
            )
            .servers(
                listOf(
                    Server().url("http://localhost:8080").description("Local Server"),
                    Server().url("http://10.0.2.2:8080").description("Android Emulator")
                )
            )
    }
}