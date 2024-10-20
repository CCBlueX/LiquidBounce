/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.contains
import net.minecraft.util.math.BlockPos
import kotlin.math.max
import kotlin.math.min

class Region(from: BlockPos, to: BlockPos) : ClosedRange<BlockPos>, Iterable<BlockPos> by BlockPos.iterate(from, to) {

    override val endInclusive: BlockPos
        get() = this.to

    override val start: BlockPos
        get() = this.from

    companion object {
        val EMPTY: Region = Region(BlockPos.ORIGIN, BlockPos.ORIGIN)

        fun quadAround(pos: BlockPos, xz: Int, y: Int): Region {
            assert(xz > 0 && y > 0)

            return Region(pos.add(-xz, -y, -xz), pos.add(xz + 1, y + 1, xz + 1))
        }

        fun fromChunkPosition(x: Int, z: Int): Region {
            val from = BlockPos(x shl 4, 0, z shl 4)

            return Region(from, from.add(16, mc.world!!.height, 16))
        }

        fun fromBlockPos(blockPos: BlockPos): Region {
            return Region(blockPos, blockPos.add(1, 1, 1))
        }
    }

    val from: BlockPos
    val to: BlockPos

    val volume: Int

    init {
        val fixedFrom = BlockPos(
            min(from.x, to.x),
            min(from.y, to.y),
            min(from.z, to.z)
        )
        val fixedTo = BlockPos(
            max(from.x, to.x),
            max(from.y, to.y),
            max(from.z, to.z)
        )

        this.from = fixedFrom
        this.to = fixedTo
        this.volume = (fixedTo.x - fixedFrom.x) * (fixedFrom.y - fixedTo.y) * (fixedFrom.z - fixedFrom.z)
    }

    private inline val xRange: IntRange
        get() = this.from.x..this.to.x

    private inline val yRange: IntRange
        get() = this.from.y..this.to.y

    private inline val zRange: IntRange
        get() = this.from.z..this.to.z

    override fun isEmpty(): Boolean = this.volume == 0

    operator fun contains(pos: Region): Boolean {
        return pos.from.x..pos.to.x in xRange && pos.from.y..pos.to.y in yRange && pos.from.z..pos.to.z in zRange
    }

    override operator fun contains(value: BlockPos): Boolean {
        return value.x in this.from.x..this.to.x && value.y in this.from.y..this.to.y && value.z in this.from.z..this.to.z
    }

    fun intersects(other: Region): Boolean {
        return this.intersects(other.from.x, other.from.y, other.from.z, other.to.x, other.to.y, other.to.z)
    }

    private fun intersects(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int): Boolean {
        return minX..maxX in xRange && maxY..minY in yRange && minZ..maxZ in zRange
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Region

        return from == other.from && to == other.to
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + to.hashCode()
        return result
    }

    /**
     * AND operator.
     *
     * **IMPORTANT:** Assumes that both regions intersect
     */
    fun intersection(currentRegion: Region): Region {
        return Region(
            BlockPos(
                max(this.from.x, currentRegion.from.x),
                max(this.from.y, currentRegion.from.y),
                max(this.from.z, currentRegion.from.z)
            ),
            BlockPos(
                min(this.to.x, currentRegion.to.x),
                min(this.to.y, currentRegion.to.y),
                min(this.to.z, currentRegion.to.z)
            )
        )
    }

    override fun toString(): String {
        return "[${this.from.x},${this.from.y},${this.from.z}] -> [${this.to.x},${this.to.y},${this.to.z}]"
    }

}
