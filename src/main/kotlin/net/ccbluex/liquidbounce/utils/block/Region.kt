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
import net.minecraft.util.math.*
import net.minecraft.world.chunk.Chunk
import kotlin.math.max
import kotlin.math.min

@Suppress("detekt:TooManyFunctions")
class Region private constructor(
    fromX: Int,
    fromY: Int,
    fromZ: Int,
    toX: Int,
    toY: Int,
    toZ: Int,
) : ClosedRange<BlockPos>,
    Iterable<BlockPos> by BlockPos.iterate(fromX, fromY, fromZ, toX, toY, toZ) {

    constructor(from: BlockPos, to: BlockPos) : this(
        min(from.x, to.x),
        min(from.y, to.y),
        min(from.z, to.z),
        max(from.x, to.x),
        max(from.y, to.y),
        max(from.z, to.z),
    )

    override val start: BlockPos
        get() = this.from

    override val endInclusive: BlockPos
        get() = this.to

    val from: BlockPos = BlockPos(fromX, fromY, fromZ)
    val to: BlockPos = BlockPos(toX, toY, toZ)

    private val lengthX: Long = toX.toLong() - fromX.toLong() + 1L
    private val lengthY: Long = toY.toLong() - fromY.toLong() + 1L
    private val lengthZ: Long = toZ.toLong() - fromZ.toLong() + 1L

    /**
     * Always false.
     */
    override fun isEmpty(): Boolean = lengthX == 0L && lengthY == 0L && lengthZ == 0L

    operator fun contains(pos: Region): Boolean =
        pos.from.x >= this.from.x && pos.from.y >= this.from.y && pos.from.z >= this.from.z &&
            pos.to.x <= this.to.x && pos.to.y <= this.to.y && pos.to.z <= this.to.z

    override operator fun contains(value: BlockPos): Boolean {
        return value.x in from.x..to.x && value.y in from.y..to.y && value.z in from.z..to.z
    }

    fun getBottomFaceCenter() = Vec3d(
        (from.x + to.x + 1).toDouble() * 0.5,
        from.y.toDouble(),
        (from.z + to.z + 1).toDouble() * 0.5,
    )

    /**
     * [Box] with offset
     */
    val boundingBox: Box
        get() = Box(
            from.x.toDouble(),
            from.y.toDouble(),
            from.z.toDouble(),
            to.x + 1.0,
            to.y + 1.0,
            to.z + 1.0
        )

    /**
     * [Box] with no offset (starts at [Vec3d.ZERO])
     */
    val box: Box
        get() = Box(
            0.0, 0.0, 0.0,
            lengthX.toDouble(),
            lengthY.toDouble(),
            lengthZ.toDouble(),
        )

    // operators

    infix fun intersects(other: Region): Boolean {
        return this.intersects(min = other.from, max = other.to)
    }

    private fun intersects(min: Vec3i, max: Vec3i): Boolean {
        return !(this.to.x < min.x || this.from.x > max.x ||
            this.to.y < min.y || this.from.y > max.y ||
            this.to.z < min.z || this.from.z > max.z)
    }

    /**
     * AND operator.
     *
     * **IMPORTANT**: Assumes that both regions intersect
     */
    infix fun intersection(currentRegion: Region): Region {
        return Region(
            BlockPos(
                max(this.from.x, currentRegion.from.x),
                max(this.from.y, currentRegion.from.y),
                max(this.from.z, currentRegion.from.z),
            ),
            BlockPos(
                min(this.to.x, currentRegion.to.x),
                min(this.to.y, currentRegion.to.y),
                min(this.to.z, currentRegion.to.z),
            )
        )
    }

    /**
     * OR operator.
     */
    infix fun union(currentRegion: Region): Region {
        return Region(
            fromX = min(this.from.x, currentRegion.from.x),
            fromY = min(this.from.y, currentRegion.from.y),
            fromZ = min(this.from.z, currentRegion.from.z),
            toX = max(this.to.x, currentRegion.to.x),
            toY = max(this.to.y, currentRegion.to.y),
            toZ = max(this.to.z, currentRegion.to.z),
        )
    }

    // from Object

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Region

        return from == other.from && to == other.to
    }

    override fun hashCode(): Int = from.hashCode() xor to.hashCode()

    override fun toString(): String {
        return "[${this.from.x},${this.from.y},${this.from.z}] -> [${this.to.x},${this.to.y},${this.to.z}]"
    }

    companion object {
        /**
         * A [Region] only containing [BlockPos.ORIGIN]
         */
        @JvmField
        val ORIGIN: Region = of(BlockPos.ORIGIN)

        @JvmStatic
        fun quadAround(pos: BlockPos, xz: Int, y: Int): Region =
            centered(pos, dx = xz, dy = y, dz = xz)

        @JvmStatic
        fun of(blockPos: BlockPos): Region =
            Region(blockPos, blockPos)

        @JvmStatic
        fun centered(pos: BlockPos, dx: Int, dy: Int, dz: Int): Region =
            Region(pos.add(-dx, -dy, -dz), pos.add(dx, dy, dz))

        @JvmStatic
        fun of(chunk: Chunk): Region {
            val pos = chunk.pos
            return Region(
                BlockPos(pos.x shl 4, chunk.bottomY, pos.z shl 4),
                BlockPos(pos.x shl 4 or 15, chunk.topYInclusive, pos.z shl 4 or 15)
            )
        }

        @JvmStatic
        fun of(pos: ChunkPos): Region {
            return Region(
                BlockPos(pos.x shl 4, mc.world!!.bottomY, pos.z shl 4),
                BlockPos(pos.x shl 4 or 15, mc.world!!.topYInclusive, pos.z shl 4 or 15)
            )
        }

    }

}
