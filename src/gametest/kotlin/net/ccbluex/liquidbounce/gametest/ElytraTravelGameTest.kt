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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.phys.Vec3

/**
 * `.goto` with an elytra over long distances: cruising with rockets from one floating platform to another far over
 * the void of the End, climbing over a pillar on the way, and arriving unhurt.
 */
class ElytraTravelGameTest : FabricClientGameTest {

    private val modules = listOf(ModuleAutoWalk)

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { modules.map { ConfigSystem.serializeValueGroup(it) } }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                server.runCommand("gamemode survival @a")
                server.runCommand("clear @a")
                server.runCommand("item replace entity @a armor.chest with elytra")

                checkEndCruise(context, server)
            }
        } finally {
            context.onClient {
                for ((module, config) in modules.zip(saved)) {
                    module.enabled = false
                    ConfigSystem.deserializeValueGroup(module, config)
                }
            }
        }
    }

    /**
     * Platforms at x 300 and x 650 over the void, the second ten blocks higher, with a pillar reaching above both
     * halfway.
     */
    private fun checkEndCruise(context: ClientGameTestContext, server: TestServerContext) {
        server.runCommand("item replace entity @a hotbar.0 with firework_rocket 64")
        server.runCommand("gamemode creative @a")
        for ((x, y) in listOf(PLATFORM_A to LOW, PLATFORM_B to LOW + RISE, PILLAR to null)) {
            server.runCommand("execute in minecraft:the_end run tp @a $x ${LOW + 40} 300")
            context.waitTicks(LOAD_TICKS)
            if (y == null) {
                server.runCommand(
                    "execute in minecraft:the_end run fill $x ${LOW - 30} 298 ${x + 2} ${LOW + 40} 302 obsidian"
                )
            } else {
                server.runCommand("execute in minecraft:the_end run fill ${x - 4} $y 296 ${x + 4} $y 304 end_stone")
            }
        }
        server.runCommand("kill @e[type=ender_dragon]")

        val start = Vec3(PLATFORM_A + 0.5, LOW + 1.0, 300.5)
        server.runCommand("execute in minecraft:the_end run tp @a ${start.x} ${start.y} ${start.z} -90 0")
        context.waitFor({ client -> client.player?.position()?.distanceToSqr(start)?.let { it < 0.01 } == true }, 200)
        context.waitTicks(LOAD_TICKS)
        // Falling in creative still counts towards fall damage, so survival only starts once standing.
        server.runCommand("gamemode survival @a")
        context.waitTicks(SETTLE_TICKS)

        val goal = Vec3(PLATFORM_B + 0.5, LOW + RISE + 1.0, 300.5)
        val glided = goto(context, server, goal, "across the void", CRUISE_TICKS) { it.isFallFlying }
        check(glided) { "Crossed the void without flying" }
    }

    /**
     * Goes to [goal] with `.goto` within [ticks]; true if [flying] held on the way.
     */
    private fun goto(
        context: ClientGameTestContext,
        server: TestServerContext,
        goal: Vec3,
        what: String,
        ticks: Int,
        flying: (net.minecraft.client.player.LocalPlayer) -> Boolean,
    ): Boolean {
        val health = server.computeOnServer<Float, RuntimeException> { it.playerList.players.first().health }
        context.command("goto ${goal.x} ${goal.y} ${goal.z}")
        check(context.fromClient { ModuleAutoWalk.enabled }) { ".goto did not turn AutoWalk on" }

        var flew = false
        repeat(ticks) {
            val (arrived, now) = context.fromClient { mc -> !ModuleAutoWalk.enabled to flying(checkNotNull(mc.player)) }
            flew = flew || now
            if (arrived) {
                val position = context.fromClient { checkNotNull(it.player).position() }
                check(position.distanceTo(goal) <= ARRIVAL) { "AutoWalk stopped at $position going $what" }
                val after = server.computeOnServer<Float, RuntimeException> { it.playerList.players.first().health }
                check(after >= health) { "Going $what hurt the player from $health to $after" }
                return flew
            }
            context.waitTick()
        }

        val position = context.fromClient { checkNotNull(it.player).position() }
        context.onClient { ModuleAutoWalk.enabled = false }
        error("Did not get $what to $goal within $ticks ticks, stopped at $position")
    }

    private companion object {
        const val PLATFORM_A = 300
        const val PLATFORM_B = 650
        const val PILLAR = 475
        const val LOW = 100
        const val RISE = 10
        const val LOAD_TICKS = 60
        const val SETTLE_TICKS = 10
        const val CRUISE_TICKS = 1600
        const val ARRIVAL = 1.5
    }

}
