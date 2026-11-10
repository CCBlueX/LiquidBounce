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

import net.ccbluex.liquidbounce.gametest.countBlocks
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.groundY
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.player
import net.ccbluex.liquidbounce.gametest.useAngleSmooth
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3

/**
 * A survival player walks through a course with the walking navigation, unhurt: around a wall, into a closed room,
 * across a gap, up a cliff, over soul sand and ice and through a door and a fence gate.
 */
class WalkNavigationGameTest : FabricClientGameTest {

    private lateinit var walker: TestWalker
    private lateinit var builder: TestWalker

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.onClient {
            walker = TestWalker("TestWalker", GOAL_RADIUS)
            builder = TestWalker("TestBuilder", GOAL_RADIUS, build = true)
            useAngleSmooth(walker)
            useAngleSmooth(builder)
        }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.groundY()
                buildCourse(server, y)
                server.runCommand("gamemode survival @a")
                server.runCommand("clear @a")
                server.runCommand("give @a diamond_pickaxe")
                server.runCommand("give @a cobblestone 64")
                context.onClient {
                    walker.active = true
                    builder.active = true
                }

                checkDetour(context, server, y)
                checkTunnel(context, server, y)
                checkBridge(context, server, y)
                checkPillar(context, server, y)
                checkSurfaces(context, server, y)
                checkDoors(context, server, y)
            }
        } finally {
            walker.stop(context)
            builder.stop(context)
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

    /** A roofed corridor in stone: east over soul sand, then north over ice. */
    private fun checkSurfaces(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        server.runCommand("fill 0 ${y - 1} 24 16 ${y + 3} 36 stone")
        server.runCommand("fill 1 $y 25 15 ${y + 2} 27 air")
        server.runCommand("fill 13 $y 25 15 ${y + 2} 35 air")
        server.runCommand("fill 3 ${y - 1} 25 11 ${y - 1} 27 soul_sand")
        server.runCommand("fill 13 ${y - 1} 25 15 ${y - 1} 35 ice")

        moveTo(context, server, Vec3(1.5, y.toDouble(), 26.5))
        walk(context, server, walker, Vec3(14.5, y.toDouble(), 34.5), 600, "over soul sand and ice")
    }

    /** A roofed corridor in stone, closed by walls with an oak door at x 26 and an oak fence gate at x 32. */
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
        walker: TestWalker,
        goal: Vec3,
        ticks: Int,
        what: String,
    ) {
        val health = server.player { it.health }
        walker.walk(context, goal, ticks, "Walking $what", "Did not walk $what", acting = { walker.isActing })

        val (after, source) = server.player { it.health to it.lastDamageSource?.type()?.msgId() }
        check(after >= health) { "Walking $what hurt the player from $health to $after ($source)" }
    }

    private fun ClientGameTestContext.cobblestone(): Int = fromClient { mc ->
        val inventory = checkNotNull(mc.player).inventory
        (0 until inventory.containerSize).sumOf { slot ->
            inventory.getItem(slot).takeIf { it.`is`(Items.COBBLESTONE) }?.count ?: 0
        }
    }

    private companion object {
        const val GOAL_RADIUS = 0.8
    }

}
