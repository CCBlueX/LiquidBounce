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

import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.gametest.harness.LocalGameTest
import net.ccbluex.liquidbounce.utils.client.logger
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items

private const val BRIDGE_Y = 120
private const val BLOCKS = 40
private const val STACK = 64
private const val ATTEMPTS = 3
private const val TIMEOUT_TICKS = 600
private const val SETTLE_TICKS = 10

/** Half the width of the area cleared before every bridge, more than one bridge reaches. */
private const val CLEARANCE = 48

/**
 * Scaffold with its default settings bridges over the void, straight and diagonally, with either aim. It must never
 * let the player fall. Blocks per second, and the ticks it needed a block but placed none, are logged for comparison.
 */
class ScaffoldBridgeGameTest : LocalGameTest() {

    private enum class Course(val yaw: Float) {
        STRAIGHT(-90f),
        DIAGONAL(-45f),
    }

    private class Run(val aim: String, val course: Course) {
        var placed = 0
        var firstTick = -1
        var lastTick = -1
        var waits = 0
        var sneaks = 0
        var fellAt: String? = null

        val blocksPerSecond
            get() = if (lastTick > firstTick) (placed - 1) * TICKS_PER_SECOND / (lastTick - firstTick) else 0.0

        override fun toString() =
            "${aim.padEnd(8)} ${course.name.padEnd(8)} placed $placed, %.2f blocks/s, waits $waits, sneaks $sneaks"
                .format(blocksPerSecond) + (fellAt?.let { ", fell at $it" } ?: "")
    }

    override fun test(context: ClientGameTestContext) {
        context.worldBuilder().create().use { world ->
            world.server.runCommand("gamemode survival @a")
            world.server.runCommand("gamerule minecraft:fall_damage false")
            context.waitFor({ client -> client.player?.hasInfiniteMaterials() == false }, SETTLE_TIMEOUT_TICKS)

            val runs = mutableListOf<Run>()
            context.restoring(ModuleScaffold) {
                for (aim in listOf("Constant", "Human")) {
                    for (course in Course.entries) {
                        repeat(ATTEMPTS) { runs += bridge(context, world, Run(aim, course)) }
                    }
                }
            }

            runs.forEach { logger.info("[ScaffoldBridge] $it") }
            for ((key, attempts) in runs.groupBy { it.aim to it.course }) {
                val blocks = attempts.sumOf { it.placed - 1 }
                val ticks = attempts.sumOf { it.lastTick - it.firstTick }
                logger.info(
                    "[ScaffoldBridge] total ${key.first} ${key.second}: %.2f blocks/s, waits %d, sneaks %d".format(
                        blocks * TICKS_PER_SECOND / ticks, attempts.sumOf { it.waits }, attempts.sumOf { it.sneaks },
                    )
                )
            }

            val failed = runs.filter { it.fellAt != null || it.placed < BLOCKS }
            check(failed.isEmpty()) { "Scaffold did not bridge:\n" + failed.joinToString("\n") }
        }
    }

    private fun bridge(context: ClientGameTestContext, world: TestSingleplayerContext, run: Run): Run {
        world.server.runCommand("clear @p")
        world.server.runCommand("give @p minecraft:stone $STACK")
        val floor = BRIDGE_Y - 1
        world.server.runCommand("fill -$CLEARANCE $floor -$CLEARANCE $CLEARANCE $floor $CLEARANCE air")
        world.server.runCommand("setblock 0 ${BRIDGE_Y - 1} 0 minecraft:stone")
        world.server.runCommand("tp @p 0.5 $BRIDGE_Y 0.5 ${run.course.yaw} 0")
        context.waitFor({ client ->
            val player = checkNotNull(client.player)
            player.onGround() && player.blockPosition() == BlockPos(0, BRIDGE_Y, 0) && blocks(client) == STACK
        }, SETTLE_TIMEOUT_TICKS)
        context.waitTicks(SETTLE_TICKS)

        context.onClient { client ->
            ModuleScaffold.set("Rotations/Aim", run.aim)
            ModuleScaffold.enabled = true
            client.options.keyUp.isDown = true
            client.options.keySprint.isDown = true
        }

        try {
            for (tick in 0 until TIMEOUT_TICKS) {
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

        if (player.y < BRIDGE_Y - FALL_MARGIN) {
            run.fellAt = player.position().toString()
            return true
        }

        return run.placed >= BLOCKS
    }

    private fun blocks(client: Minecraft) = checkNotNull(client.player).inventory.countItem(Items.STONE)

    private companion object {
        const val SETTLE_TIMEOUT_TICKS = 200
        const val TICKS_PER_SECOND = 20.0
        const val FALL_MARGIN = 0.01
    }

}
