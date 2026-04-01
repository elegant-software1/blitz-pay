package com.elegant.software.blitzpay.config

import org.hibernate.exception.JDBCConnectionException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanCreationException
import org.springframework.boot.web.server.PortInUseException
import java.net.ConnectException
import java.sql.SQLException
import kotlin.test.assertContains
import kotlin.test.assertEquals

class StartupFailureDiagnosticsTest {

    @Test
    fun `classifies database connectivity failures`() {
        val failure = IllegalStateException(
            "startup failed",
            JDBCConnectionException("Unable to open JDBC Connection", SQLException("Connection refused")),
        )

        val diagnosis = StartupFailureDiagnostics.diagnose(failure)

        assertEquals("DATABASE_CONNECTIVITY", diagnosis.category)
        assertEquals("java.sql.SQLException", diagnosis.rootCauseType)
        assertContains(diagnosis.actions.joinToString("\n"), "SPRING_DATASOURCE_URL")
    }

    @Test
    fun `classifies db failures even when root cause is connect exception`() {
        val sqlException = SQLException("The connection attempt failed.", ConnectException("Connection refused"))
        val failure = IllegalStateException(
            "startup failed",
            JDBCConnectionException("Unable to open JDBC Connection for DDL execution", sqlException),
        )

        val diagnosis = StartupFailureDiagnostics.diagnose(failure)

        assertEquals("DATABASE_CONNECTIVITY", diagnosis.category)
        assertEquals("java.net.ConnectException", diagnosis.rootCauseType)
    }

    @Test
    fun `classifies port conflicts`() {
        val diagnosis = StartupFailureDiagnostics.diagnose(PortInUseException(8080))

        assertEquals("PORT_CONFLICT", diagnosis.category)
        assertContains(diagnosis.actions.joinToString("\n"), "server.port")
    }

    @Test
    fun `classifies configuration binding failures`() {
        val bindException = IllegalArgumentException("Could not resolve placeholder 'PAYMENT_KEY' in value")

        val diagnosis = StartupFailureDiagnostics.diagnose(bindException)

        assertEquals("CONFIGURATION", diagnosis.category)
        assertContains(diagnosis.actions.joinToString("\n"), "environment variables")
    }

    @Test
    fun `classifies bean wiring failures`() {
        val diagnosis = StartupFailureDiagnostics.diagnose(BeanCreationException("missing dependency"))

        assertEquals("BEAN_WIRING", diagnosis.category)
        assertContains(diagnosis.actions.joinToString("\n"), "missing beans")
    }

    @Test
    fun `falls back to unknown for unmatched failures`() {
        val diagnosis = StartupFailureDiagnostics.diagnose(IllegalStateException("unexpected startup issue"))

        assertEquals("UNKNOWN", diagnosis.category)
        assertContains(diagnosis.actions.joinToString("\n"), "--debug")
    }
}
