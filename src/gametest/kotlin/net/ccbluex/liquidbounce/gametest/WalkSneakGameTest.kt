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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseValueGroup
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A survival player walks sneaking: crouched through a tunnel one and a half blocks high into a closed room, along a
 * bridge one block wide with a turn high over the ground, and along a straight one to a goal it could steer to
 * directly, without taking damage.
 */
class WalkSneakGameTest : FabricClientGameTest {

    private object Owner : EventListener {
        @Volatile
        var active = false

        override val running: Boolean
            get() = active
    }

    private class Walker : NavigationBaseValueGroup<Unit>(
        Owner, "TestSneakWalker", enabled = true, allowBreak = false, allowPlace = false, showRoute = false,
    ) {
        @Volatile
        var goal: Vec3? = null

        override fun createNavigationContext() = Unit

        override fun calculateGoalPosition(context: Unit) = goal

        override fun goalRadius(context: Unit) = GOAL_RADIUS
    }

    private lateinit var walker: Walker

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.onClient { walker = Walker() }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.computeOnServer<Int, RuntimeException> {
                    it.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 8, 8)
                }
                buildCourse(server, y)
                server.runCommand("gamemode survival @a")
                server.runCommand("clear @a")
                server.runCommand("gamerule natural_health_regeneration false")
                context.onClient { Owner.active = true }

                checkLowTunnel(context, server, y)
                checkBridge(context, server, y)
                checkStraightBridge(context, server, y)
            }
        } finally {
            context.onClient {
                Owner.active = false
                walker.goal = null
            }
        }
    }

    /**
     * A stone block at x 10..20 with a closed room at x 14..19, entered only through a tunnel at x 10..13 under top
     * slabs, so it is one and a half blocks high. Twenty blocks over the ground, a bridge one block wide from x 30 to
     * x 42, turning at x 42 towards z 10, and a straight one along z -10 from x 33 to x 45.
     */
    private fun buildCourse(server: TestServerContext, y: Int) {
        server.runCommand("fill 0 $y -20 60 ${y + 30} 20 air")
        server.runCommand("fill 0 ${y - 1} -20 60 ${y - 1} 20 stone")

        server.runCommand("fill 10 $y -3 20 ${y + 3} 3 stone")
        server.runCommand("fill 14 $y -2 19 ${y + 2} 2 air")
        server.runCommand("fill 10 $y 0 13 $y 0 air")
        server.runCommand("fill 10 ${y + 1} 0 13 ${y + 1} 0 stone_slab[type=top]")

        server.runCommand("fill 30 ${y + 20} -1 32 ${y + 20} 1 stone")
        server.runCommand("fill 33 ${y + 20} 0 42 ${y + 20} 0 stone")
        server.runCommand("fill 42 ${y + 20} 1 42 ${y + 20} 8 stone")
        server.runCommand("fill 41 ${y + 20} 9 43 ${y + 20} 11 stone")

        server.runCommand("fill 30 ${y + 20} -11 32 ${y + 20} -9 stone")
        server.runCommand("fill 33 ${y + 20} -10 45 ${y + 20} -10 stone")
    }

    private fun checkLowTunnel(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(6.5, y.toDouble(), 0.5))
        walk(context, server, Vec3(17.5, y.toDouble(), 0.5), 600, "through the low tunnel")
    }

    private fun checkBridge(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(31.5, y + 21.0, 0.5))
        walk(context, server, Vec3(42.5, y + 21.0, 10.5), 1200, "along the bridge")
    }

    private fun checkStraightBridge(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(31.5, y + 21.0, -9.5))
        walk(context, server, Vec3(45.5, y + 21.0, -9.5), 1200, "along the straight bridge")
    }

    private fun walk(context: ClientGameTestContext, server: TestServerContext, goal: Vec3, ticks: Int, what: String) {
        val health = server.computeOnServer<Float, RuntimeException> { it.playerList.players.first().health }
        context.onClient { walker.goal = goal }
        val sneaked = try {
            awaitArrival(context, goal, ticks, what)
        } finally {
            context.onClient { walker.goal = null }
        }

        val after = server.computeOnServer<Float, RuntimeException> { it.playerList.players.first().health }
        check(after >= health) { "Walking $what hurt the player from $health to $after" }
        check(sneaked) { "Walked $what without sneaking" }
    }

    /**
     * Waits until the player is at [goal]; true if it sneaked on the way.
     */
    private fun awaitArrival(context: ClientGameTestContext, goal: Vec3, ticks: Int, what: String): Boolean {
        var sneaked = false
        repeat(ticks) {
            val (position, sneaking) = context.fromClient {
                val player = checkNotNull(it.player)
                player.position() to player.isShiftKeyDown
            }
            sneaked = sneaked || sneaking
            if (position.horizontalDistance(goal) <= GOAL_RADIUS + 0.1 && abs(position.y - goal.y) < 1.0) {
                return sneaked
            }
            context.waitTick()
        }

        val position = context.fromClient { checkNotNull(it.player).position() }
        error("Did not walk $what to $goal within $ticks ticks, stopped at $position")
    }

    private fun moveTo(context: ClientGameTestContext, server: TestServerContext, position: Vec3) {
        server.runCommand("tp @a ${position.x} ${position.y} ${position.z}")
        context.waitFor({ client ->
            client.player?.position()?.distanceToSqr(position)?.let { it < EPSILON } == true
        }, WAIT)
        context.waitTicks(5)
    }

    private fun Vec3.horizontalDistance(other: Vec3): Double {
        val dx = x - other.x
        val dz = z - other.z
        return sqrt(dx * dx + dz * dz)
    }

    private companion object {
        const val GOAL_RADIUS = 0.8
        const val WAIT = 200
        const val EPSILON = 1.0E-4
    }

}
