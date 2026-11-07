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
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseValueGroup
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A survival player walks through a course with the walking navigation: around a wall, through a closed room,
 * across a gap in the air, up a cliff, over soul sand and ice and through a door and a fence gate, without taking
 * damage.
 */
@Suppress("TooManyFunctions")
class WalkNavigationGameTest : FabricClientGameTest {

    private object Owner : EventListener {
        @Volatile
        var active = false

        override val running: Boolean
            get() = active
    }

    private class Walker(name: String, build: Boolean) : NavigationBaseValueGroup<Unit>(
        Owner, name, enabled = true, allowBreak = build, allowPlace = build, showRoute = false,
    ) {
        @Volatile
        var goal: Vec3? = null

        override fun createNavigationContext() = Unit

        override fun calculateGoalPosition(context: Unit) = goal

        override fun goalRadius(context: Unit) = GOAL_RADIUS
    }

    private lateinit var walker: Walker
    private lateinit var builder: Walker

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.onClient {
            walker = Walker("TestWalker", build = false)
            builder = Walker("TestBuilder", build = true)
        }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.computeOnServer<Int, RuntimeException> {
                    it.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 8, 8)
                }
                buildCourse(server, y)
                server.runCommand("gamemode survival @a")
                server.runCommand("clear @a")
                server.runCommand("give @a diamond_pickaxe")
                server.runCommand("give @a cobblestone 64")
                context.onClient { Owner.active = true }

                checkDetour(context, server, y)
                checkTunnel(context, server, y)
                checkBridge(context, server, y)
                checkPillar(context, server, y)
                checkSurfaces(context, server, y)
                checkDoors(context, server, y)
            }
        } finally {
            context.onClient {
                Owner.active = false
                walker.goal = null
                builder.goal = null
            }
        }
    }

    /**
     * A wall at x 10, a closed stone room at x 22..28, two floating islands at x 32..34 and 40..42, and a bedrock
     * cliff at x 46..50. The ground floor is stone at y - 1.
     */
    private fun buildCourse(server: TestServerContext, y: Int) {
        server.runCommand("fill 0 $y -20 60 ${y + 14} 20 air")
        server.runCommand("fill 0 ${y - 1} -20 60 ${y - 1} 20 stone")
        server.runCommand("fill 10 $y -6 10 ${y + 3} 6 stone")
        server.runCommand("fill 22 $y -3 28 ${y + 3} 3 stone hollow")
        server.runCommand("fill 32 ${y + 8} -1 34 ${y + 8} 1 stone")
        server.runCommand("fill 40 ${y + 8} -1 42 ${y + 8} 1 stone")
        server.runCommand("fill 46 $y 0 50 ${y + 2} 4 bedrock")
    }

    private fun checkDetour(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(4.5, y.toDouble(), 0.5))
        walk(context, server, walker, Vec3(16.5, y.toDouble(), 0.5), 400, "around the wall")
        check(server.countBlocks(BlockPos(10, y, -6), BlockPos(10, y + 3, 6)) == 13 * 4) { "The wall was changed" }
    }

    private fun checkTunnel(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(18.5, y.toDouble(), 0.5))
        walk(context, server, builder, Vec3(25.5, y + 1.0, 0.5), 600, "into the closed room")
    }

    private fun checkBridge(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(33.5, y + 9.0, 0.5))
        val before = context.cobblestone()
        walk(context, server, builder, Vec3(41.5, y + 9.0, 0.5), 800, "across the gap")
        check(context.cobblestone() < before) { "Crossed the gap without placing a block" }
    }

    private fun checkPillar(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(44.5, y.toDouble(), 2.5))
        val before = context.cobblestone()
        walk(context, server, builder, Vec3(48.5, y + 3.0, 2.5), 800, "up the cliff")
        check(context.cobblestone() < before) { "Climbed the cliff without pillaring" }
    }

    /**
     * A roofed corridor in a stone block at x 0..16, z 24..36: east over soul sand, then north over ice.
     */
    private fun checkSurfaces(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        server.runCommand("fill 0 ${y - 1} 24 16 ${y + 3} 36 stone")
        server.runCommand("fill 1 $y 25 15 ${y + 2} 27 air")
        server.runCommand("fill 13 $y 25 15 ${y + 2} 35 air")
        server.runCommand("fill 3 ${y - 1} 25 11 ${y - 1} 27 soul_sand")
        server.runCommand("fill 13 ${y - 1} 25 15 ${y - 1} 35 ice")

        moveTo(context, server, Vec3(1.5, y.toDouble(), 26.5))
        walk(context, server, walker, Vec3(14.5, y.toDouble(), 34.5), 600, "over soul sand and ice")
    }

    /**
     * A roofed corridor in a stone block at x 20..40, z 28..32, closed by walls with a closed oak door at x 26 and a
     * closed oak fence gate at x 32.
     */
    private fun checkDoors(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        server.runCommand("fill 20 ${y - 1} 28 40 ${y + 3} 32 stone")
        server.runCommand("fill 21 $y 29 39 ${y + 2} 31 air")
        server.runCommand("fill 26 $y 29 26 ${y + 2} 31 stone")
        server.runCommand("fill 32 $y 29 32 ${y + 2} 31 stone")
        server.runCommand("setblock 26 $y 30 oak_door[facing=east,half=lower]")
        server.runCommand("setblock 26 ${y + 1} 30 oak_door[facing=east,half=upper]")
        server.runCommand("setblock 32 $y 30 oak_fence_gate[facing=east]")
        server.runCommand("setblock 32 ${y + 1} 30 air")

        moveTo(context, server, Vec3(22.5, y.toDouble(), 30.5))
        walk(context, server, walker, Vec3(37.5, y.toDouble(), 30.5), 600, "through a door and a fence gate")
        for (pos in listOf(BlockPos(26, y, 30), BlockPos(32, y, 30))) {
            val open = server.computeOnServer<Boolean, RuntimeException> {
                it.overworld().getBlockState(pos).getValue(BlockStateProperties.OPEN)
            }
            check(open) { "The way through $pos was not opened" }
        }
    }

    private fun walk(
        context: ClientGameTestContext,
        server: TestServerContext,
        walker: Walker,
        goal: Vec3,
        ticks: Int,
        what: String,
    ) {
        val health = server.computeOnServer<Float, RuntimeException> { it.player().health }
        context.onClient { walker.goal = goal }
        try {
            awaitArrival(context, goal, ticks, what)
        } finally {
            context.onClient { walker.goal = null }
        }

        val (after, source) = server.computeOnServer<Pair<Float, String?>, RuntimeException> {
            val player = it.player()
            player.health to player.lastDamageSource?.type()?.msgId()
        }
        check(after >= health) { "Walking $what hurt the player from $health to $after ($source)" }
    }

    private fun awaitArrival(context: ClientGameTestContext, goal: Vec3, ticks: Int, what: String) {
        repeat(ticks) {
            val position = context.fromClient { checkNotNull(it.player).position() }
            if (position.horizontalDistance(goal) <= GOAL_RADIUS + 0.1 && abs(position.y - goal.y) < 1.0) {
                return
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

    private fun ClientGameTestContext.cobblestone(): Int = fromClient { mc ->
        val inventory = checkNotNull(mc.player).inventory
        (0 until inventory.containerSize).sumOf { slot ->
            inventory.getItem(slot).takeIf { it.`is`(Items.COBBLESTONE) }?.count ?: 0
        }
    }

    private fun TestServerContext.countBlocks(from: BlockPos, to: BlockPos): Int =
        computeOnServer<Int, RuntimeException> { server ->
            BlockPos.betweenClosed(from, to).count { !server.overworld().getBlockState(it).isAir }
        }

    private fun MinecraftServer.player() = playerList.players.first()

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
