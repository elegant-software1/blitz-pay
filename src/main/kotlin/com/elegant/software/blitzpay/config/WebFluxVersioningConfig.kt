package com.elegant.software.blitzpay.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.server.PathContainer
import org.springframework.web.reactive.accept.ApiVersionResolver
import org.springframework.web.reactive.config.ApiVersionConfigurer
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
class WebFluxVersioningConfig(
    private val apiVersionProperties: ApiVersionProperties,
    private val corsProperties: CorsProperties
) : WebFluxConfigurer {

    override fun configureApiVersioning(configurer: ApiVersionConfigurer) {
        configurer
            .useVersionResolver(PathOnlyApiVersionResolver())
            .setVersionRequired(false)
            .setDefaultVersion(apiVersionProperties.defaultVersion)
            .detectSupportedVersions(true)
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(*corsProperties.allowedOrigins.toTypedArray())
            .allowedMethods(*corsProperties.allowedMethods.toTypedArray())
            .allowedHeaders(*corsProperties.allowedHeaders.toTypedArray())
    }
}

class PathOnlyApiVersionResolver : ApiVersionResolver {
    private val versionPattern = Regex("^v(\\d+(?:\\.\\d+)*)$")

    override fun resolveVersion(exchange: org.springframework.web.server.ServerWebExchange): String? {
        val path = exchange.request.path.value()

        // ✅ EXCLUDE MCP endpoint completely
        if (path.startsWith("/mcp")) {
            return null
        }

        val firstSegment = exchange.request.path.pathWithinApplication().elements()
            .filterIsInstance<PathContainer.PathSegment>()
            .firstOrNull()
            ?.valueToMatch()
            ?: return null

        val match = versionPattern.matchEntire(firstSegment) ?: return null
        return match.groupValues[1]
    }
}
