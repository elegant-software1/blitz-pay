package com.elegant.software.blitzpay.config

import org.hibernate.exception.JDBCConnectionException
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.UnsatisfiedDependencyException
import org.springframework.boot.context.event.ApplicationFailedEvent
import org.springframework.boot.context.properties.bind.BindException as ConfigurationBindException
import org.springframework.boot.web.server.PortInUseException
import org.springframework.context.ApplicationListener
import org.springframework.core.env.Environment
import org.slf4j.LoggerFactory
import java.net.BindException
import java.sql.SQLException

class StartupFailureDiagnosticsListener : ApplicationListener<ApplicationFailedEvent> {

    private val logger = LoggerFactory.getLogger(StartupFailureDiagnosticsListener::class.java)

    override fun onApplicationEvent(event: ApplicationFailedEvent) {
        val exception = event.exception
        val diagnosis = StartupFailureDiagnostics.diagnose(exception)
        val environment = event.applicationContext?.environment

        logger.error(
            """
            Startup diagnostics:
            - Category: ${diagnosis.category}
            - Root cause: ${diagnosis.rootCauseType}${diagnosis.rootCauseMessage?.let { ": $it" } ?: ""}
            - Recommended actions:
            ${diagnosis.actions.joinToString(separator = "\n") { "  - $it" }}
            - Runtime context:
              - activeProfiles: ${activeProfiles(environment)}
              - server.port: ${environment?.getProperty("server.port") ?: "8080 (default)"}
              - spring.datasource.url: ${environment?.getProperty("spring.datasource.url") ?: "not configured"}
            """.trimIndent(),
            exception,
        )
    }

    private fun activeProfiles(environment: Environment?): String {
        if (environment == null) return "unknown"
        val active = environment.activeProfiles.filter { it.isNotBlank() }
        return if (active.isEmpty()) "default" else active.joinToString(",")
    }
}

internal object StartupFailureDiagnostics {

    fun diagnose(failure: Throwable): StartupFailureDiagnosis {
        val causeChain = causeChainOf(failure)
        val rootCause = causeChain.last()
        val category = classify(causeChain)
        return StartupFailureDiagnosis(
            category = category.name,
            rootCauseType = rootCause.javaClass.name,
            rootCauseMessage = rootCause.message?.takeIf { it.isNotBlank() },
            actions = actionsFor(category),
        )
    }

    private fun causeChainOf(failure: Throwable): List<Throwable> {
        val chain = mutableListOf<Throwable>()
        val seen = mutableSetOf<Throwable>()
        var current: Throwable? = failure

        while (current != null && seen.add(current)) {
            chain += current
            current = current.cause
        }
        return chain
    }

    private fun classify(causeChain: List<Throwable>): StartupFailureCategory {
        if (causeChain.any { it is PortInUseException }) return StartupFailureCategory.PORT_CONFLICT
        if (causeChain.any { it is BindException && it.message.orEmpty().contains("Address already in use", ignoreCase = true) }) {
            return StartupFailureCategory.PORT_CONFLICT
        }

        if (causeChain.any {
                it is ConfigurationBindException || it::class.qualifiedName == "org.springframework.util.PlaceholderResolutionException"
            }) {
            return StartupFailureCategory.CONFIGURATION
        }

        return when {
            causeChain.any {
                it is IllegalArgumentException && (
                    it.message.orEmpty().contains("Could not resolve placeholder", ignoreCase = true) ||
                        it.message.orEmpty().contains("Failed to bind properties", ignoreCase = true)
                    )
            } -> StartupFailureCategory.CONFIGURATION

            causeChain.any { it is JDBCConnectionException || it is SQLException } ->
                StartupFailureCategory.DATABASE_CONNECTIVITY

            causeChain.any { it is BeanCreationException || it is UnsatisfiedDependencyException } ->
                StartupFailureCategory.BEAN_WIRING

            causeChain.any {
                it.javaClass.name.contains("liquibase", ignoreCase = true) ||
                    it.javaClass.name.contains("flyway", ignoreCase = true)
            } -> StartupFailureCategory.MIGRATION

            else -> StartupFailureCategory.UNKNOWN
        }
    }

    private fun actionsFor(category: StartupFailureCategory): List<String> = when (category) {
        StartupFailureCategory.CONFIGURATION -> listOf(
            "Validate required properties and environment variables for the active profile.",
            "Check property names/types in application configuration and profile-specific files.",
            "Run with `--debug` to inspect Spring condition evaluation if the source is unclear.",
        )

        StartupFailureCategory.PORT_CONFLICT -> listOf(
            "Free the occupied port or set a different `server.port`.",
            "Verify no previous application instance is still running.",
        )

        StartupFailureCategory.DATABASE_CONNECTIVITY -> listOf(
            "Verify database host/port reachability and credentials.",
            "Check `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`.",
            "If using Docker Compose locally, run `docker compose up -d postgres` and wait for health checks.",
        )

        StartupFailureCategory.BEAN_WIRING -> listOf(
            "Check missing beans, ambiguous candidates, and constructor dependency chains.",
            "Confirm component scan boundaries include the required packages.",
        )

        StartupFailureCategory.MIGRATION -> listOf(
            "Validate migration scripts order and checksum consistency.",
            "Ensure migration user permissions match schema-change requirements.",
        )

        StartupFailureCategory.UNKNOWN -> listOf(
            "Inspect the root cause and the first failing stack frames above.",
            "Run with `--debug` to get condition evaluation and auto-configuration details.",
            "Add a dedicated `FailureAnalyzer` for recurrent startup failure patterns.",
        )
    }
}

internal data class StartupFailureDiagnosis(
    val category: String,
    val rootCauseType: String,
    val rootCauseMessage: String?,
    val actions: List<String>,
)

private enum class StartupFailureCategory {
    CONFIGURATION,
    PORT_CONFLICT,
    DATABASE_CONNECTIVITY,
    BEAN_WIRING,
    MIGRATION,
    UNKNOWN,
}
