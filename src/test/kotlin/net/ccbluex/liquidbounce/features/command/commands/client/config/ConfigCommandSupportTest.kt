/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.command.commands.client.config

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import net.ccbluex.liquidbounce.api.core.HttpException
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.models.auth.ExpiryValue
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.minecraft.network.chat.Component
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class ConfigCommandSupportTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }
    }

    private val command = object : CmdI18n {
        override val path = "config"
    }

    @Test
    fun `optional authentication keeps a usable session`() = runTest {
        val session = OAuthSession(ExpiryValue("access", 4102444800000), "refresh")
        assertSame(session, optionalSession { session })
        assertNull(optionalSession { null })
    }

    @Test
    fun `optional authentication falls back to public access on token and session failures`() = runTest {
        val failures = listOf(
            HttpException(HttpMethod.POST, "https://example.com/token", 400, """{"error":"invalid_grant"}"""),
            IOException("Token endpoint unavailable"),
            IllegalStateException("No session")
        )
        for (failure in failures) {
            assertNull(optionalSession { throw failure })
        }
    }

    @Test
    fun `optional authentication propagates cancellation`() = runTest {
        val cancellation = CancellationException("cancelled")
        assertSame(cancellation, assertFailsWith<CancellationException> {
            optionalSession { throw cancellation }
        })
    }

    @Test
    fun `optional authentication does not catch fatal errors`() = runTest {
        val failure = AssertionError("failure")
        assertSame(failure, assertFailsWith<AssertionError> { optionalSession { throw failure } })
    }

    @Test
    fun `required authentication failures become command errors`() = runTest {
        val failure = IOException("Token endpoint unavailable")
        val error = assertFailsWith<CommandException> { command.request { throw failure } }
        assertSame(failure, error.cause)
    }

    @Test
    fun `required authentication propagates cancellation`() = runTest {
        val cancellation = CancellationException("cancelled")
        assertSame(cancellation, assertFailsWith<CancellationException> {
            command.request { throw cancellation }
        })
    }

    @Test
    fun `required authentication preserves existing command errors`() = runTest {
        val error = CommandException(Component.literal("Sign in first"))
        assertSame(error, assertFailsWith<CommandException> { command.request { throw error } })
    }
}
