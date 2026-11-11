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

import net.ccbluex.liquidbounce.features.client.Client
import net.ccbluex.liquidbounce.features.client.ClientContext
import net.ccbluex.liquidbounce.features.client.ClientManager
import net.ccbluex.liquidbounce.features.client.ClientOverview
import net.ccbluex.liquidbounce.features.client.ClientSnapshot
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext
import net.minecraft.server.MinecraftServer
import net.minecraft.world.phys.Vec3

private const val JOIN_TIMEOUT_TICKS = 30 * 20
private const val WALK_TICKS = 60
private const val SWITCHES = 20
private const val SOAK_TICKS = 200

private const val WALKED_DISTANCE = 2.0
private const val STOOD_DISTANCE = 0.1

/**
 * Three accounts play on one server from one client. Each is a client with its own connection and its own
 * modules, the window goes from one to the other, and none of them loses its connection over it. A client in the
 * background shows what it sees, and the ClickGUI edits its modules over the interop server.
 */
class MultiClientGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()

        context.createMultiClientServer().use { server ->
            server.connect().use { connection ->
                connection.waitForChunksDownload()
                val address = server.address

                val main = ClientManager.main
                val second = context.openClient("Second", address)
                val third = context.openClient("Third", address)
                server.waitFor({ it.playerList.playerCount == 3 }, JOIN_TIMEOUT_TICKS)
                context.waitFor({ second.inGame && third.inGame }, JOIN_TIMEOUT_TICKS)

                // Players standing in each other would be pushed around
                server.runCommand("execute as Second at @s run tp @s ~6 ~ ~")
                server.runCommand("execute as Third at @s run tp @s ~-6 ~ ~")
                context.waitTicks(20)

                checkModulesBelongToTheirClient(context, main, second, third)
                checkOnlyTheWalkingClientMoves(context, server, second)
                checkSwitching(context, listOf(main, second, third))
                checkClientView(context, server, third)
                checkClientInterop(context, main, third)
                checkStillPlaying(context, server, listOf(main, second, third))
                checkClosing(context, server, second)
            }
        }
    }

    private fun checkModulesBelongToTheirClient(
        context: ClientGameTestContext,
        main: Client,
        second: Client,
        third: Client,
    ) {
        fun sprint(client: Client) = context.fromClient {
            ClientContext.within(client) { ModuleSprint.enabled to ModuleSprint.mode().getValue() }
        }

        val before = sprint(main)
        context.onClient {
            ClientContext.within(second) {
                ModuleSprint.mode().setByString("Omnidirectional")
                ModuleSprint.enabled = true
            }
        }

        check(sprint(second) == (true to "Omnidirectional")) { "Sprint of the second client: ${sprint(second)}" }
        check(sprint(main) == before) { "Sprint of the main client changed from $before to ${sprint(main)}" }
        check(!sprint(third).first) { "Sprint of the third client was turned on with the second" }
    }

    private fun checkOnlyTheWalkingClientMoves(
        context: ClientGameTestContext,
        server: TestDedicatedServerContext,
        walking: Client,
    ) {
        val start = server.positions()
        context.onClient { ClientContext.within(walking) { ModuleAutoWalk.enabled = true } }
        context.waitTicks(WALK_TICKS)

        val snapshot = context.fromClient { ClientOverview.snapshot(walking) }
        check("AutoWalk" in snapshot.modules && snapshot.activity == ClientSnapshot.Activity.MOVING) {
            "The walking client is described as ${snapshot.activity} with ${snapshot.modules}"
        }
        context.onClient { ClientContext.within(walking) { ModuleAutoWalk.enabled = false } }

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

    private fun checkSwitching(context: ClientGameTestContext, clients: List<Client>) {
        // Ends on the main client, which is the one the test framework disconnects
        for (switch in SWITCHES downTo 0) {
            val client = clients[switch % clients.size]
            context.onClient { ClientManager.activate(client) }
            context.waitFor { it.player != null && it.player === client.player }

            val name = context.name(client)
            val shown = context.fromClient { it.player!!.gameProfile.name to it.user.name }
            check(client.isActive && shown == (name to name)) { "The window shows $shown after switching to $name" }
        }
    }

    private fun checkStillPlaying(
        context: ClientGameTestContext,
        server: TestDedicatedServerContext,
        clients: List<Client>,
    ) {
        context.waitTicks(SOAK_TICKS)
        check(server.positions().size == clients.size && context.fromClient { clients.all { it.inGame } }) {
            "Not every client is still playing: ${server.positions().keys}"
        }
    }

    private fun checkClosing(
        context: ClientGameTestContext,
        server: TestDedicatedServerContext,
        client: Client,
    ) {
        context.onClient { ClientManager.close(client) }
        val name = context.name(client)
        server.waitFor({ it.playerList.getPlayerByName(name) == null }, JOIN_TIMEOUT_TICKS)

        check(context.fromClient { client !in ClientManager.clients && !client.inGame }) {
            "The closed client is still around"
        }
    }

}

private fun ModuleSprint.mode() = containedValues.first { it.name == "Mode" }

private fun ClientGameTestContext.name(client: Client): String = fromClient { client.user.name }


private fun <T> TestDedicatedServerContext.fromServer(block: (MinecraftServer) -> T): T =
    computeOnServer<T, RuntimeException> { block(it) }

private fun TestDedicatedServerContext.positions(): Map<String, Vec3> =
    fromServer { server -> server.playerList.players.associate { it.gameProfile.name to it.position() } }
