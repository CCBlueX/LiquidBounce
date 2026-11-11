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

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.GameTestScope.Companion.DEFAULT_TIMEOUT
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.gametest.harness.action.query
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

private val HEALTH = Regex("""(-?[\d.]+)f$""")
private val POSITION = Regex("""\[(-?[\d.E-]+)d, (-?[\d.E-]+)d, (-?[\d.E-]+)d]""")

// The session's max_block_modifications
private const val FILL_LIMIT = 32768

/** The player's health as the server has it. */
fun GameTestScope.health() = query("data get entity $playerName Health", HEALTH).groupValues[1].toFloat()

/** Where the server has the player. */
fun GameTestScope.serverPosition(): Vec3 {
    val (x, y, z) = query("data get entity $playerName Pos", POSITION).destructured
    return Vec3(x.toDouble(), y.toDouble(), z.toDouble())
}

/** Waits until the server has the player within [distance] of [position]. */
fun GameTestScope.awaitServerAt(position: Vec3, distance: Double = 0.1, timeout: Int = DEFAULT_TIMEOUT) =
    awaitServer("entity @a[x=${position.x},y=${position.y},z=${position.z},distance=..$distance]", timeout)

/** Keeps the chunks from [from] to [to] loaded, which a fill far from the player needs, and waits for them. */
fun GameTestScope.forceLoadArea(from: BlockPos, to: BlockPos) {
    run("forceload add ${from.x} ${from.z} ${to.x} ${to.z}")
    awaitServer("loaded ${from.x} ${from.y} ${from.z}")
    awaitServer("loaded ${to.x} ${to.y} ${to.z}")
}

/** Fills like `fill`, in slices along x small enough for the session's block limit. */
fun GameTestScope.fillArea(from: BlockPos, to: BlockPos, block: String) {
    val minX = minOf(from.x, to.x)
    val maxX = maxOf(from.x, to.x)
    val slice = maxOf(1, FILL_LIMIT / ((abs(to.y - from.y) + 1) * (abs(to.z - from.z) + 1)))
    for (x in minX..maxX step slice) {
        fill(BlockPos(x, from.y, from.z), BlockPos(minOf(x + slice - 1, maxX), to.y, to.z), block)
    }
}

/** How many blocks between [from] and [to] the server has the same as [reference], a region of the same size. */
fun GameTestScope.matchingBlocks(from: BlockPos, to: BlockPos, reference: BlockPos): Int {
    val command = "execute if blocks ${from.x} ${from.y} ${from.z} ${to.x} ${to.y} ${to.z} " +
        "${reference.x} ${reference.y} ${reference.z} all"
    val id = run(command)
    repeat(DEFAULT_TIMEOUT) {
        server.result(id)?.let { return it }
        ticks(1)
    }
    throw AssertionError("The server never finished '$command', see ${server.log}")
}

/** How many items of [item] the player carries as the server has it. */
fun GameTestScope.carried(item: String): Int {
    val id = run("execute if items entity $playerName container.* $item")
    repeat(DEFAULT_TIMEOUT) {
        server.result(id)?.let { return it }
        ticks(1)
    }
    throw AssertionError("The server never counted $item, see ${server.log}")
}
