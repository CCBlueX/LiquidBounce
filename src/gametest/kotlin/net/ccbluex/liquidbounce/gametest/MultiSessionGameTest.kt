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
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.features.session.ClientSession
import net.ccbluex.liquidbounce.features.session.SessionContext
import net.ccbluex.liquidbounce.features.session.SessionManager
import net.ccbluex.liquidbounce.features.session.SessionOverview
import net.ccbluex.liquidbounce.features.session.SessionSnapshot
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext
import net.minecraft.server.MinecraftServer
import net.minecraft.world.phys.Vec3
import java.util.Properties

private const val JOIN_TIMEOUT_TICKS = 30 * 20
private const val WALK_TICKS = 60
private const val SWITCHES = 20
private const val SOAK_TICKS = 600

private const val WALKED_DISTANCE = 2.0
private const val STOOD_DISTANCE = 0.1

/**
 * Three accounts play on one server from one client. Each is a session with its own connection and its own
 * modules, the window goes from one to the other, and none of them loses its connection over it.
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

                val main = SessionManager.main
                val second = context.openSession("Second", address)
                val third = context.openSession("Third", address)
                server.waitFor({ it.playerList.playerCount == 3 }, JOIN_TIMEOUT_TICKS)
                context.waitFor({ second.inGame && third.inGame }, JOIN_TIMEOUT_TICKS)

                // Players standing in each other would be pushed around
                server.runCommand("execute as Second at @s run tp @s ~6 ~ ~")
                server.runCommand("execute as Third at @s run tp @s ~-6 ~ ~")
                context.waitTicks(20)

                checkModulesBelongToTheirSession(context, main, second, third)
                checkOnlyTheWalkingSessionMoves(context, server, second)
                checkSwitching(context, server, listOf(main, second, third))
                checkClosing(context, server, second)
            }
        }
    }

    private fun checkModulesBelongToTheirSession(
        context: ClientGameTestContext,
        main: ClientSession,
        second: ClientSession,
        third: ClientSession,
    ) {
        fun sprint(session: ClientSession) = context.fromClient {
            SessionContext.within(session) { ModuleSprint.enabled to ModuleSprint.mode().getValue() }
        }

        val before = sprint(main)
        context.onClient {
            SessionContext.within(second) {
                ModuleSprint.mode().setByString("Omnidirectional")
                ModuleSprint.enabled = true
            }
        }

        check(sprint(second) == (true to "Omnidirectional")) { "Sprint of the second session: ${sprint(second)}" }
        check(sprint(main) == before) { "Sprint of the main session changed from $before to ${sprint(main)}" }
        check(!sprint(third).first) { "Sprint of the third session was turned on with the second" }
    }

    private fun checkOnlyTheWalkingSessionMoves(
        context: ClientGameTestContext,
        server: TestDedicatedServerContext,
        walking: ClientSession,
    ) {
        val start = server.positions()
        context.onClient { SessionContext.within(walking) { ModuleAutoWalk.enabled = true } }
        context.waitTicks(WALK_TICKS)

        val snapshot = context.fromClient { SessionOverview.snapshot(walking) }
        check("AutoWalk" in snapshot.modules && snapshot.activity == SessionSnapshot.Activity.MOVING) {
            "The walking session is described as ${snapshot.activity} with ${snapshot.modules}"
        }
        context.onClient { SessionContext.within(walking) { ModuleAutoWalk.enabled = false } }

        val walker = context.name(walking)
        for ((name, position) in server.positions()) {
            val moved = position.distanceTo(start.getValue(name))
            if (name == walker) {
                check(moved > WALKED_DISTANCE) { "$name only walked $moved blocks" }
            } else {
                check(moved < STOOD_DISTANCE) { "$name moved $moved blocks although only $walker walks" }
            }
        }
    }

    private fun checkSwitching(
        context: ClientGameTestContext,
        server: TestDedicatedServerContext,
        sessions: List<ClientSession>,
    ) {
        // Ends on the main session, which is the one the test framework disconnects
        for (switch in SWITCHES downTo 0) {
            val session = sessions[switch % sessions.size]
            context.onClient { SessionManager.activate(session) }
            context.waitFor { it.player != null && it.player === session.player }

            val name = context.name(session)
            val shown = context.fromClient { it.player!!.gameProfile.name to it.user.name }
            check(session.isActive && shown == (name to name)) { "The window shows $shown after switching to $name" }
        }

        context.waitTicks(SOAK_TICKS)
        check(server.positions().size == sessions.size && context.fromClient { sessions.all { it.inGame } }) {
            "Still playing after $SWITCHES switches: ${server.positions().keys}"
        }
    }

    private fun checkClosing(
        context: ClientGameTestContext,
        server: TestDedicatedServerContext,
        session: ClientSession,
    ) {
        context.onClient { SessionManager.close(session) }
        val name = context.name(session)
        server.waitFor({ it.playerList.getPlayerByName(name) == null }, JOIN_TIMEOUT_TICKS)

        check(context.fromClient { session !in SessionManager.sessions && !session.inGame }) {
            "The closed session is still around"
        }
    }

}

private fun ModuleSprint.mode() = containedValues.first { it.name == "Mode" }

private fun ClientGameTestContext.name(session: ClientSession): String = fromClient { session.user.name }

private fun ClientGameTestContext.openSession(name: String, address: String): ClientSession = fromClient {
    val account = CrackedAccount(name, false).also { it.refresh() }
    SessionManager.open(AccountManager.authenticate(account)).also { SessionManager.connect(it, address) }
}

private fun <T> TestDedicatedServerContext.fromServer(block: (MinecraftServer) -> T): T =
    computeOnServer<T, RuntimeException> { block(it) }

private fun TestDedicatedServerContext.positions(): Map<String, Vec3> =
    fromServer { server -> server.playerList.players.associate { it.gameProfile.name to it.position() } }
