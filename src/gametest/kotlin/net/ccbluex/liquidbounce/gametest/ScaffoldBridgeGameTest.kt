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

import net.ccbluex.gametest.harness.ClientGameTest
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.logger
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items

private const val BRIDGE_Y = 120
private const val BLOCKS = 20
private const val STACK = 64
private const val SETTLE_TICKS = 10
private const val SETTLE_TIMEOUT_TICKS = 200

/**
 * Scaffold bridges over the void with every bridging type and either aim and nothing else changed: straight,
 * diagonally, from where a player usually stands, and with the jump key held. It must never let the player fall.
 * Blocks per second, and the ticks it needed a block but placed none, are logged for comparison.
 */
class ScaffoldBridgeGameTest : ClientGameTest(ScaffoldBridge::test)

private object ScaffoldBridge {

    /** A bridge from the block at the origin: where on it the player stands, looking where, [jumps] with jump held. */
    private enum class Course(val x: Double, val z: Double, val yaw: Float, val jumps: Boolean = false) {
        STRAIGHT(0.5, 0.5, -90f),
        DIAGONAL(0.5, 0.5, -45f),

        // Off the middle of the block and off the diagonal
        OFFSET(0.3, 0.7, -50f),
        JUMPING(0.5, 0.5, -90f, jumps = true),
    }

    private val COMBINATIONS = listOf("Normal", "Ninja", "GodBridge", "Breezily", "Telly").flatMap { bridging ->
        listOf("Constant", "Human").map { aim -> bridging to aim }
    }

    private class Run(val bridging: String, val aim: String, val course: Course) {
        var placed = 0
        var firstTick = -1
        var lastTick = -1
        var waits = 0
        var sneaks = 0
        var fellAt: String? = null

        val blocksPerSecond
            get() = if (lastTick > firstTick) (placed - 1) * 20.0 / (lastTick - firstTick) else 0.0

        override fun toString() = "${bridging.padEnd(9)} ${aim.padEnd(8)} ${course.name.padEnd(8)} " +
            "placed $placed, %.2f blocks/s, waits $waits, sneaks $sneaks".format(blocksPerSecond) +
            (fellAt?.let { ", fell at $it" } ?: "")
    }

    fun test(context: ClientGameTestContext) {
        context.worldBuilder().create().use { world ->
            world.server.runCommand("gamemode survival @a")
            world.server.runCommand("gamerule minecraft:fall_damage false")
            context.waitFor({ client -> client.player?.hasInfiniteMaterials() == false }, SETTLE_TIMEOUT_TICKS)

            val runs = mutableListOf<Run>()
            context.restoring(ModuleScaffold) {
                for ((bridging, aim) in COMBINATIONS) {
                    for (course in Course.entries) {
                        runs += bridge(context, world, Run(bridging, aim, course))
                    }
                }
            }

            runs.forEach { logger.info("[ScaffoldBridge] $it") }
            for ((key, attempts) in runs.groupBy { it.bridging to it.aim }) {
                val blocks = attempts.sumOf { it.placed - 1 }
                val ticks = attempts.sumOf { it.lastTick - it.firstTick }
                logger.info(
                    "[ScaffoldBridge] total ${key.first} ${key.second}: %.2f blocks/s, waits %d, sneaks %d".format(
                        blocks * 20.0 / ticks, attempts.sumOf { it.waits }, attempts.sumOf { it.sneaks },
                    )
                )
            }

            val failed = runs.filter { it.fellAt != null || it.placed < BLOCKS }
            check(failed.isEmpty()) { "Scaffold did not bridge:\n" + failed.joinToString("\n") }
        }
    }

    private fun bridge(context: ClientGameTestContext, world: TestSingleplayerContext, run: Run): Run {
        val course = run.course
        world.server.runCommand("clear @p")
        world.server.runCommand("give @p minecraft:stone $STACK")
        // Every bridge starts from a single block. They all lead to +x and +z, and a jumping one climbs
        world.server.runCommand("fill -2 ${BRIDGE_Y - 1} -2 40 ${BRIDGE_Y + 15} 40 air")
        world.server.runCommand("setblock 0 ${BRIDGE_Y - 1} 0 minecraft:stone")
        world.server.runCommand("tp @p ${course.x} $BRIDGE_Y ${course.z} ${course.yaw} 0")
        context.waitFor({ client ->
            val player = checkNotNull(client.player)
            player.onGround() && player.blockPosition() == BlockPos(0, BRIDGE_Y, 0) && blocks(client) == STACK
        }, SETTLE_TIMEOUT_TICKS)
        context.waitTicks(SETTLE_TICKS)

        context.onClient { client ->
            ModuleScaffold.set("Bridging", run.bridging)
            ModuleScaffold.set("Rotations/Aim", run.aim)
            ModuleScaffold.enabled = true
            client.options.keyUp.isDown = true
            client.options.keySprint.isDown = true
            client.options.keyJump.isDown = course.jumps
        }

        try {
            for (tick in 0 until 600) {
                context.waitTick()
                val done = context.fromClient { client -> sample(client, run, tick) }
                if (done) {
                    break
                }
            }
        } finally {
            context.onClient { client ->
                client.options.keyUp.isDown = false
                client.options.keySprint.isDown = false
                client.options.keyJump.isDown = false
                ModuleScaffold.enabled = false
            }
            context.waitTicks(SETTLE_TICKS)
        }

        return run
    }

    /** Records this tick of [run] and whether it is over. */
    private fun sample(client: Minecraft, run: Run, tick: Int): Boolean {
        val player = checkNotNull(client.player)
        val placed = STACK - blocks(client)

        if (placed > run.placed) {
            if (run.firstTick < 0) {
                run.firstTick = tick
            }
            run.lastTick = tick
            run.placed = placed
        } else if (run.firstTick >= 0 && ModuleScaffold.currentTarget != null &&
            player.level().getBlockState(player.blockPosition().below()).isAir
        ) {
            // Over the gap with a block owed and none placed
            run.waits++
        }

        if (run.firstTick >= 0 && player.isShiftKeyDown) {
            run.sneaks++
        }

        if (player.y < BRIDGE_Y - 0.01) {
            run.fellAt = player.position().toString()
            return true
        }

        return run.placed >= BLOCKS
    }

    private fun blocks(client: Minecraft) = checkNotNull(client.player).inventory.countItem(Items.STONE)

}
