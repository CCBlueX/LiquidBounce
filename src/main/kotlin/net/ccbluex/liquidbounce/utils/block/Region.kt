/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.chunk.Chunk
import kotlin.math.max
import kotlin.math.min

@Suppress("detekt:TooManyFunctions")
class Region(from: BlockPos, to: BlockPos) : ClosedRange<BlockPos>, Iterable<BlockPos> by BlockPos.iterate(from, to) {

    override val endInclusive: BlockPos
        get() = this.to

    override val start: BlockPos
        get() = this.from

    companion object {
        // the Region is a closed range so this is not empty actually
        val EMPTY: Region = Region(BlockPos.ORIGIN, BlockPos.ORIGIN)

        fun quadAround(pos: BlockPos, xz: Int, y: Int): Region {
            return Region(pos.add(-xz, -y, -xz), pos.add(xz, y, xz))
        }

        fun from(blockPos: BlockPos): Region {
            return Region(blockPos, blockPos)
        }

        fun from(chunk: Chunk): Region {
            val pos = chunk.pos
            return Region(
                BlockPos(pos.x shl 4, chunk.bottomY, pos.z shl 4),
                BlockPos(pos.x shl 4 or 15, chunk.topYInclusive, pos.z shl 4 or 15)
            )
        }

        fun fromChunkPos(x: Int, z: Int): Region {
            return Region(
                BlockPos(x shl 4, mc.world!!.bottomY, z shl 4),
                BlockPos(x shl 4 or 15, mc.world!!.topYInclusive, z shl 4 or 15)
            )
        }

        fun Region.getBox(): Box {
            return Box(
                0.0, 0.0, 0.0,
                to.x - from.x + 1.0,
                to.y - from.y + 1.0,
                to.z - from.z + 1.0,
            )
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
        this.volume = (fixedTo.x - fixedFrom.x) * (fixedTo.y - fixedFrom.y) * (fixedTo.z - fixedFrom.z)
    }

    private inline val xRange: IntRange
        get() = this.from.x..this.to.x

    private inline val yRange: IntRange
        get() = this.from.y..this.to.y

    private inline val zRange: IntRange
        get() = this.from.z..this.to.z

    override fun isEmpty(): Boolean = this.volume == 0

    operator fun contains(pos: Region): Boolean {
        return pos.xRange in xRange && pos.yRange in yRange && pos.zRange in zRange
    }

    override operator fun contains(value: BlockPos): Boolean {
        return value.x in xRange && value.y in yRange && value.z in zRange
    }

    fun intersects(other: Region): Boolean {
        return this.intersects(
            min = Vec3i(other.from.x, other.from.y, other.from.z),
            max = Vec3i(other.to.x, other.to.y, other.to.z)
        )
    }

    private fun intersects(min: Vec3i, max: Vec3i): Boolean {
        return !(this.to.x <= min.x || this.from.x >= max.x ||
            this.to.y <= min.y || this.from.y >= max.y ||
            this.to.z <= min.z || this.from.z >= max.z)
    }

    fun getBottomFaceCenter() = Vec3d(
        (from.x + to.x + 1).toDouble() / 2.0,
        from.y.toDouble(),
        (from.z + to.z + 1).toDouble() / 2.0
    )

    fun getBoundingBox() = Box(
        from.x.toDouble(),
        from.y.toDouble(),
        from.z.toDouble(),
        to.x + 1.0,
        to.y + 1.0,
        to.z + 1.0
    )

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

    fun union(currentRegion: Region): Region {
        return Region(
            BlockPos(
                min(this.from.x, currentRegion.from.x),
                min(this.from.y, currentRegion.from.y),
                min(this.from.z, currentRegion.from.z)
            ),
            BlockPos(
                max(this.to.x, currentRegion.to.x),
                max(this.to.y, currentRegion.to.y),
                max(this.to.z, currentRegion.to.z)
            )
        )
    }

    override fun toString(): String {
        return "[${this.from.x},${this.from.y},${this.from.z}] -> [${this.to.x},${this.to.y},${this.to.z}]"
    }

}
