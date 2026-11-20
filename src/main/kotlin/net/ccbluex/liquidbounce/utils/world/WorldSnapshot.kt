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

package net.ccbluex.liquidbounce.utils.world

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.level.material.FluidState

/** Block states copied from the client world, safe to read from any thread. */
class WorldSnapshot internal constructor(
    private val minY: Int,
    private val height: Int,
    private val sections: Long2ObjectOpenHashMap<PalettedContainer<BlockState>?>,
    private val borderMinX: Double,
    private val borderMaxX: Double,
    private val borderMinZ: Double,
    private val borderMaxZ: Double,
) : WorldBlocks {

    override fun isKnown(x: Int, y: Int, z: Int) =
        y >= minY && y < minY + height && sections.containsKey(sectionKey(x, y, z))

    override fun isInsideBorder(x: Double, z: Double) =
        x >= borderMinX && x < borderMaxX && z >= borderMinZ && z < borderMaxZ

    override fun getBlockState(pos: BlockPos): BlockState {
        if (pos.y < minY || pos.y >= minY + height) {
            return VOID_AIR
        }

        val key = sectionKey(pos.x, pos.y, pos.z)
        val states = sections.get(key) ?: return if (sections.containsKey(key)) AIR else VOID_AIR
        return states.get(pos.x and 15, pos.y and 15, pos.z and 15)
    }

    override fun getFluidState(pos: BlockPos): FluidState = getBlockState(pos).fluidState

    override fun getBlockEntity(pos: BlockPos): BlockEntity? = null

    override fun getHeight() = height

    override fun getMinY() = minY

    private companion object {
        val AIR: BlockState = Blocks.AIR.defaultBlockState()
        val VOID_AIR: BlockState = Blocks.VOID_AIR.defaultBlockState()

        fun sectionKey(x: Int, y: Int, z: Int) = SectionPos.asLong(x shr 4, y shr 4, z shr 4)
    }

}
