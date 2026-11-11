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

package net.ccbluex.liquidbounce.utils.navigation

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.route.RouteSearch
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.ccbluex.liquidbounce.utils.navigation.walk.Abilities
import net.ccbluex.liquidbounce.utils.navigation.walk.Path
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.Vec3
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** An in-memory world for routing tests; every block is air unless filled. */
class TestNavigationWorld(
    private val known: (x: Int, z: Int) -> Boolean = { _, _ -> true },
) : NavigationWorld {

    private val blocks = HashMap<BlockPos, BlockState>()

    fun fill(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int, state: BlockState = STONE) = apply {
        for (x in minOf(x1, x2)..maxOf(x1, x2)) {
            for (y in minOf(y1, y2)..maxOf(y1, y2)) {
                for (z in minOf(z1, z2)..maxOf(z1, z2)) {
                    blocks[BlockPos(x, y, z)] = state
                }
            }
        }
    }

    fun set(x: Int, y: Int, z: Int, state: BlockState) = fill(x, y, z, x, y, z, state)

    fun terrain(opensDoors: Boolean = false) = Terrain(this, opensDoors = opensDoors)

    override fun isKnown(x: Int, y: Int, z: Int) = y >= minY && y <= maxY && known(x, z)

    override fun isInsideBorder(x: Double, z: Double) = true

    override fun getBlockEntity(pos: BlockPos): BlockEntity? = null

    override fun getBlockState(pos: BlockPos): BlockState = blocks[pos] ?: AIR

    override fun getFluidState(pos: BlockPos): FluidState = getBlockState(pos).fluidState

    override fun getHeight() = 384

    override fun getMinY() = -64

    companion object {
        val STONE: BlockState get() = Blocks.STONE.defaultBlockState()
        val AIR: BlockState get() = Blocks.AIR.defaultBlockState()

        /** A stone floor at y = 0 from -40 to 40, so feet stand at y = 1. */
        fun flat(known: (x: Int, z: Int) -> Boolean = { _, _ -> true }) =
            TestNavigationWorld(known).fill(-40, 0, -40, 40, 0, 40)

        fun feet(x: Int, y: Int, z: Int): Vec3 = Vec3.atBottomCenterOf(BlockPos(x, y, z))
    }

}

/** Advances the search until it is done, after the first slice at most [maxSlices] more times. */
fun <T> RouteSearch<T>.advanceToEnd(maxSlices: Int = Int.MAX_VALUE): RouteResult<T> {
    var result = advance()
    var slices = 0
    while (result == RouteResult.Searching && slices++ < maxSlices) {
        result = advance()
    }
    return result
}

inline fun <reified T> found(result: RouteResult<T>): T = assertIs<RouteResult.Found<T>>(result).value

fun completePath(result: RouteResult<Path>, message: String? = "path should reach the goal"): Path =
    found(result).also { assertTrue(it.complete, message) }

/** A sprinting walker that takes falls of [safeFall] blocks and mines every block in [breakTicks]. */
fun walkAbilities(
    canBreak: Boolean = false,
    canPlace: Boolean = false,
    canParkour: Boolean = false,
    riskyJumps: Boolean = false,
    safeFall: Int = 3,
    breakTicks: Double = Double.POSITIVE_INFINITY,
    avoid: LongSet = LongOpenHashSet(),
) = Abilities(
    canBreak = canBreak,
    canPlace = canPlace,
    placeableBlocks = if (canPlace) 64 else 0,
    canParkour = canParkour,
    riskyJumps = riskyJumps,
    canSprint = true,
    safeFall = safeFall,
    waterFall = 20,
    breakTicks = { breakTicks },
    avoid = avoid,
)
