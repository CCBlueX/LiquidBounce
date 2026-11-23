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
package net.ccbluex.liquidbounce.gametest.server

import net.ccbluex.gametest.harness.Anticheat
import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.TestWorld
import net.ccbluex.gametest.harness.action.give
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.gametest.set
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.Options
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Scaffold bridges over the void on courses a player walks while the anticheats watch: it must never let the player
 * fall, and nothing that watches may object. Human has to pass every anticheat with every bridging type but Telly,
 * Constant only those without aim checks. Degrees turned per block are logged for comparison.
 */
class ScaffoldGameTest : ServerGameTest(TestWorld.VOID, {
    val saved = client { ConfigSystem.serializeValueGroup(ModuleScaffold) }
    val runs = buildList {
        // Telly needs a quick aim: a Human one cannot turn onto the next face while the jump carries the player past it
        for (bridging in listOf("Normal", "Ninja", "GodBridge", "Breezily")) {
            COURSES.forEach { course -> add(Triple(bridging, "Human", course)) }
        }
        if (anticheats.none { it == Anticheat.INTAVE || it == Anticheat.MX }) {
            for (bridging in listOf("Normal", "Ninja", "GodBridge", "Breezily", "Telly")) {
                COURSES.take(2).forEach { course -> add(Triple(bridging, "Constant", course)) }
            }
        }
    }

    val failed = mutableListOf<String>()
    try {
        for ((bridging, aim, course) in runs) {
            var run = bridge(bridging, aim, course)
            logger.info("[Scaffold] $run")
            if (!run.done) {
                // A start on the server falls now and then, mostly on a diagonal; falling twice in a row fails
                run = bridge(bridging, aim, course)
                logger.info("[Scaffold] $run, second walk")
            }
            if (!run.done) {
                failed += run.toString()
            }
        }
    } finally {
        client {
            ModuleScaffold.enabled = false
            ConfigSystem.deserializeValueGroup(ModuleScaffold, saved)
        }
    }
    check(failed.isEmpty()) { "Scaffold did not bridge:\n" + failed.joinToString("\n") }
}) {
    // Each bridge gets a second walk of its own; playing every bridge again would take minutes
    override val retries get() = 0
}

/**
 * Where a bridge starts and how the player walks it: [keys] held from the start, the camera turned to [turnTo] once
 * [turnAt] blocks are down, the keys let go for two seconds once [stopAt] blocks are.
 */
private class Course(
    val name: String,
    val start: BlockPos,
    val yaw: Float,
    val blocks: Int,
    val keys: (Options) -> List<KeyMapping>,
    val turnAt: Int = -1,
    val turnTo: Float = 0f,
    val stopAt: Int = -1,
)

private val COURSES = listOf(
    Course("Straight", BlockPos(4, 64, 0), -90f, 14, { listOf(it.keyUp, it.keySprint) }),
    Course("Diagonal", BlockPos(4, 64, 4), -45f, 14, { listOf(it.keyUp, it.keySprint) }),
    Course("Backwards", BlockPos(-4, 64, 0), -90f, 12, { listOf(it.keyDown) }),
    Course("Sideways", BlockPos(0, 64, 4), -90f, 12, { listOf(it.keyRight) }),
    Course("Corner", BlockPos(0, 64, -4), 180f, 16, { listOf(it.keyUp, it.keySprint) }, turnAt = 8, turnTo = -90f),
    Course("StopAndGo", BlockPos(-4, 64, 4), 0f, 12, { listOf(it.keyUp, it.keySprint) }, stopAt = 6),
)

/** One bridge, from [stack] planks in the hand. */
private class Run(val bridging: String, val aim: String, val course: Course, private val stack: Int) {
    var placed = 0
    var fell = false
    var turned = 0.0

    /** Whether every block of the course went down without a fall. */
    val done get() = !fell && placed >= course.blocks
    private var last = RotationManager.serverRotation
    private var stoppedAt = -1

    /** Walks the [tick]th tick of the course; whether the bridge is over. */
    fun tick(mc: Minecraft, tick: Int): Boolean {
        val player = checkNotNull(mc.player)
        val now = stack - player.inventory.countItem(Items.OAK_PLANKS)
        if (now > placed) {
            placed = now
            if (placed == course.turnAt) {
                player.yRot = course.turnTo
            }
            if (placed == course.stopAt && stoppedAt < 0) {
                course.keys(mc.options).forEach { key -> key.isDown = false }
                stoppedAt = tick
            }
        }
        if (stoppedAt >= 0 && tick == stoppedAt + 40) {
            course.keys(mc.options).forEach { key -> key.isDown = true }
        }

        val sent = RotationManager.serverRotation
        val yaw = angleDifference(sent.yaw, last.yaw) * cos(Math.toRadians(sent.pitch.toDouble()))
        turned += hypot(yaw, (sent.pitch - last.pitch).toDouble())
        last = sent

        fell = player.y < course.start.y - 0.5
        return fell || placed >= course.blocks
    }

    override fun toString() = "${bridging.padEnd(9)} ${aim.padEnd(8)} ${course.name.padEnd(9)} " +
        "placed %2d/%2d, %.1f degrees a block".format(placed, course.blocks, turned / maxOf(placed, 1)) +
        if (fell) ", fell" else ""
}

private fun GameTestScope.bridge(bridging: String, aim: String, course: Course): Run {
    give("minecraft:oak_planks", 64)
    awaitClient("the blocks") { checkNotNull(it.player).inventory.countItem(Items.OAK_PLANKS) >= 64 }
    travel(course.start, yaw = course.yaw)
    ticks(10)
    val run = client {
        ModuleScaffold.set("Bridging", bridging)
        ModuleScaffold.set("Rotations/Aim", aim)
        ModuleScaffold.enabled = true
        course.keys(it.options).forEach { key -> key.isDown = true }
        Run(bridging, aim, course, checkNotNull(it.player).inventory.countItem(Items.OAK_PLANKS))
    }

    try {
        for (tick in 0 until 500) {
            ticks(1)
            if (client { run.tick(it, tick) }) {
                break
            }
        }
    } finally {
        client {
            COURSES.flatMap { course -> course.keys(it.options) }.forEach { key -> key.isDown = false }
            ModuleScaffold.enabled = false
        }
    }
    if (run.fell) {
        // Back up before the void takes the player, so the next bridge starts alive
        travel(course.start, yaw = course.yaw)
    }
    ticks(20)
    return run
}
