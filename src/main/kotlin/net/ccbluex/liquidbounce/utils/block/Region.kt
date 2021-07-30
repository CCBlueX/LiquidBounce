/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import java.lang.Integer.max
import java.lang.Integer.min

class Region(from: BlockPos, to: BlockPos) {

    companion object {
        val EMPTY: Region = Region(BlockPos.ORIGIN, BlockPos.ORIGIN)

        fun quadAround(pos: BlockPos, xz: Int, y: Int): Region {
            assert(xz > 0 && y > 0)

            return Region(pos.subtract(Vec3i(xz, y, xz)), pos.add(Vec3i(xz + 1, y + 1, xz + 1)))
        }

        fun fromChunkPosition(x: Int, z: Int): Region {
            val from = BlockPos(x * 16, 0, z * 16)

            return Region(from, from.add(BlockPos(16, mc.world!!.height, 16)))
        }

        fun fromBlockPos(blockPos: BlockPos): Region {
            return Region(blockPos, blockPos.add(BlockPos(1, 1, 1)))
        }
    }

    val from: BlockPos
    val to: BlockPos

    val volume: Int
        get() {
            val delta = this.to.subtract(this.from)

            return delta.x * delta.y * delta.z
        }

    init {
        val fixedFrom = BlockPos(
            min(from.x, to.x),
            min(from.y, to.y),
            min(from.z, to.z),
        )
        val fixedTo = BlockPos(
            max(from.x, to.x),
            max(from.y, to.y),
            max(from.z, to.z),
        )

        this.from = fixedFrom
        this.to = fixedTo
    }

    fun isEmpty(): Boolean {
        return this.from.x == this.to.x || this.from.y == this.to.y || this.from.z == this.to.z
    }

    operator fun contains(pos: Region): Boolean {
        return pos.from.x >= this.from.x && pos.to.x <= this.to.x && pos.from.y >= this.from.y && pos.to.y <= this.to.y && pos.from.z >= this.from.z && pos.to.z <= this.to.z
    }

    operator fun contains(pos: BlockPos): Boolean {
        return pos.x >= this.from.x && pos.x < this.to.x && pos.y >= this.from.y && pos.y < this.to.y && pos.z >= this.from.z && pos.z < this.to.z
    }

    fun intersects(other: Region): Boolean {
        return this.intersects(other.from.x, other.from.y, other.from.z, other.to.x, other.to.y, other.to.z)
    }

    private fun intersects(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int): Boolean {
        return this.from.x <= maxX && this.to.x > minX && this.from.y <= maxY && this.to.y > minY && this.from.z <= maxZ && this.to.z > minZ
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Region

        if (from != other.from) return false
        if (to != other.to) return false

        return true
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + to.hashCode()
        return result
    }

    inline fun forEachCoordinate(function: (Int, Int, Int) -> Unit) {
        for (x in from.x until to.x) {
            for (y in from.y until to.y) {
                for (z in from.z until to.z) {
                    function(x, y, z)
                }
            }
        }
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
                max(this.from.z, currentRegion.from.z),
            ),
            BlockPos(
                min(this.to.x, currentRegion.to.x),
                min(this.to.y, currentRegion.to.y),
                min(this.to.z, currentRegion.to.z),
            )
        )
    }

    override fun toString(): String {
        return "[${this.from.x},${this.from.y},${this.from.z}] -> [${this.to.x},${this.to.y},${this.to.z}]"
    }

}
