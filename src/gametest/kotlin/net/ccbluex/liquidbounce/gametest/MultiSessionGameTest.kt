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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.features.account.AccountManager
import net.ccbluex.liquidbounce.features.account.CrackedAccount
import net.ccbluex.liquidbounce.features.session.ClientSession
import net.ccbluex.liquidbounce.features.session.SessionManager
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext
import net.minecraft.server.MinecraftServer
import java.util.Properties

private const val JOIN_TIMEOUT_TICKS = 30 * 20
private const val SOAK_TICKS = 600

/**
 * Several accounts play on one server from one client, each as a session of its own.
 */
class MultiSessionGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()

        val properties = Properties().apply {
            setProperty("online-mode", "false")
            setProperty("white-list", "false")
            setProperty("max-players", "8")
        }
        context.worldBuilder().createServer(properties).use { server ->
            server.connect().use { connection ->
                connection.waitForChunksDownload()
                val address = "localhost:${server.fromServer { it.port }}"

                val second = context.openSession("Second", address)
                server.waitFor({ it.playerList.getPlayerByName("Second") != null }, JOIN_TIMEOUT_TICKS)
                context.waitFor({ second.inGame }, JOIN_TIMEOUT_TICKS)

                context.waitTicks(SOAK_TICKS)
                val online = server.fromServer { it.playerList.players.map { player -> player.gameProfile.name } }
                check(online.size == 2 && "Second" in online) { "Players after $SOAK_TICKS ticks: $online" }
                check(context.fromClient { second.inGame }) { "The second session left the game" }
            }
        }
    }

}

private fun ClientGameTestContext.openSession(name: String, address: String): ClientSession = fromClient {
    val account = CrackedAccount(name, false).also { it.refresh() }
    SessionManager.open(AccountManager.authenticate(account)).also { SessionManager.connect(it, address) }
}

private fun <T> TestDedicatedServerContext.fromServer(block: (MinecraftServer) -> T): T =
    computeOnServer<T, RuntimeException> { block(it) }
