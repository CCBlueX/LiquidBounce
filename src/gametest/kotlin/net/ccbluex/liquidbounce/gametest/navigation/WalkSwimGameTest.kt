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

package net.ccbluex.liquidbounce.gametest.navigation

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.useAngleSmooth
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseValueGroup
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.min

/**
 * A survival player walks from a closed room to another one joined only under water: it dives into a pool, swims
 * through a flooded tunnel one block high into a second pool and climbs out to the goal, without taking damage or
 * running out of air.
 */
class WalkSwimGameTest : FabricClientGameTest {

    private object Owner : EventListener {
        @Volatile
        var active = false

        override val running: Boolean
            get() = active
    }

    private class Walker : NavigationBaseValueGroup<Unit>(
        Owner, "TestSwimWalker", enabled = true, allowBreak = false, allowPlace = false, showRoute = false,
    ) {
        @Volatile
        var goal: Vec3? = null

        override fun createNavigationContext() = Unit

        override fun calculateGoalPosition(context: Unit) = goal

        override fun goalRadius(context: Unit) = GOAL_RADIUS
    }

    private class Swim(val swam: Boolean, val minAir: Int)

    private lateinit var walker: Walker

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.onClient {
            walker = Walker()
            useAngleSmooth(walker)
        }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val ground = server.computeOnServer<Int, RuntimeException> {
                    it.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 8, 8)
                }
                // Above the thin superflat ground, so the pools fit into the stone below the rooms.
                val y = ground + COURSE_HEIGHT
                buildCourse(server, y)
                server.runCommand("gamemode survival @a")
                server.runCommand("clear @a")
                server.runCommand("gamerule natural_health_regeneration false")
                context.onClient { Owner.active = true }

                checkTunnel(context, server, y)
            }
        } finally {
            context.onClient {
                Owner.active = false
                walker.goal = null
            }
        }
    }

    /**
     * A stone block with two closed rooms at x 0..10 and x 16..26, each with a pool four blocks deep, at x 5..9 and
     * x 17..21. A flooded tunnel one block high and seven blocks long joins the bottoms of the pools.
     */
    private fun buildCourse(server: TestServerContext, y: Int) {
        server.runCommand("fill -2 ${y - 7} -6 30 ${y + 4} 6 stone")
        server.runCommand("fill 0 $y -3 10 ${y + 2} 3 air")
        server.runCommand("fill 16 $y -3 26 ${y + 2} 3 air")
        server.runCommand("fill 5 ${y - 4} -2 9 ${y - 1} 2 water")
        server.runCommand("fill 17 ${y - 4} -2 21 ${y - 1} 2 water")
        server.runCommand("fill 10 ${y - 4} 0 16 ${y - 4} 0 water")
    }

    private fun checkTunnel(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(2.5, y.toDouble(), 0.5))
        val goal = Vec3(24.5, y.toDouble(), 0.5)
        val health = server.computeOnServer<Float, RuntimeException> { it.playerList.players.first().health }

        context.onClient { walker.goal = goal }
        val swim = try {
            TurnWatch.watch(context, "Swimming through the tunnel") { awaitArrival(context, goal, ARRIVAL_TICKS) }
        } finally {
            context.onClient { walker.goal = null }
        }

        val after = server.computeOnServer<Float, RuntimeException> { it.playerList.players.first().health }
        check(after >= health) { "Swimming through the tunnel hurt the player from $health to $after" }
        check(swim.minAir > 0) { "The player ran out of air in the tunnel" }
        check(swim.swam) { "Went through the tunnel without the swimming pose" }
    }

    private fun awaitArrival(context: ClientGameTestContext, goal: Vec3, ticks: Int): Swim {
        var swam = false
        var minAir = Int.MAX_VALUE
        repeat(ticks) {
            val (position, swimming, air) = context.fromClient {
                val player = checkNotNull(it.player)
                Triple(player.position(), player.isSwimming, player.airSupply)
            }
            swam = swam || swimming
            minAir = min(minAir, air)
            if (position.horizontalDistanceTo(goal) <= GOAL_RADIUS + 0.1 && abs(position.y - goal.y) < 1.0) {
                return Swim(swam, minAir)
            }
            context.waitTick()
        }

        val position = context.fromClient { checkNotNull(it.player).position() }
        error("Did not swim through the tunnel to $goal within $ticks ticks, stopped at $position")
    }

    private fun moveTo(context: ClientGameTestContext, server: TestServerContext, position: Vec3) {
        server.runCommand("tp @a ${position.x} ${position.y} ${position.z}")
        context.waitFor({ client ->
            client.player?.position()?.distanceToSqr(position)?.let { it < EPSILON } == true
        }, WAIT)
        context.waitTicks(5)
    }

    private companion object {
        const val COURSE_HEIGHT = 10
        const val ARRIVAL_TICKS = 800
        const val GOAL_RADIUS = 0.8
        const val WAIT = 200
        const val EPSILON = 1.0E-4
    }

}
