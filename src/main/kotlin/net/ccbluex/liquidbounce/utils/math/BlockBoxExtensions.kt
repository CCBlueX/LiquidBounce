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

package net.ccbluex.liquidbounce.utils.math

import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.chunk.Chunk

fun BlockBox.iterate(): Iterable<BlockPos> =
    BlockPos.iterate(minX, minY, minZ, maxX, maxY, maxZ)

operator fun BlockBox.iterator(): Iterator<BlockPos> = iterate().iterator()

private inline val BlockBox.lengthX: Int get() = maxX - minX + 1
private inline val BlockBox.lengthY: Int get() = maxY - minY + 1
private inline val BlockBox.lengthZ: Int get() = maxZ - minZ + 1

val BlockBox.size: Int get() = lengthX * lengthY * lengthZ

val BlockBox.from: BlockPos get() = BlockPos(minX, minY, minZ)

val BlockBox.to: BlockPos get() = BlockPos(minX, minY, minZ)

operator fun BlockBox.contains(other: BlockBox): Boolean =
    other.minX >= this.minX &&
        other.maxX <= this.maxX &&
        other.minY >= this.minY &&
        other.maxY <= this.maxY &&
        other.minZ >= this.minZ &&
        other.maxZ <= this.maxZ

fun Chunk.toBlockBox(): BlockBox = this.pos.toBlockBox(minY = this.bottomY, maxY = this.topYInclusive)

@JvmOverloads
fun ChunkPos.toBlockBox(
    minY: Int = world.bottomY,
    maxY: Int = world.topYInclusive,
): BlockBox =
    BlockBox(
        this.startX, minY, this.startZ,
        this.endX, maxY, this.endZ,
    )

val BlockBox.boundingBox: Box get() = Box(
    minX.toDouble(), minY.toDouble(), minZ.toDouble(),
    maxX.toDouble() + 1.0, maxY.toDouble() + 1.0, maxZ.toDouble() + 1.0,
)

val BlockBox.box: Box get() = Box(
    0.0, 0.0, 0.0,
    lengthX.toDouble(), lengthY.toDouble(), lengthZ.toDouble(),
)

fun BlockBox.centerPointOf(side: Direction): Vec3d =
    when (side) {
        Direction.DOWN  -> Vec3d(lengthX * 0.5, minY - 0.5, lengthZ * 0.5)
        Direction.UP    -> Vec3d(lengthX * 0.5, maxY + 0.5, lengthZ * 0.5)
        Direction.EAST  -> Vec3d(maxX + 0.5, lengthY * 0.5, lengthZ * 0.5)
        Direction.WEST  -> Vec3d(minX - 0.5, lengthY * 0.5, lengthZ * 0.5)
        Direction.SOUTH -> Vec3d(lengthX * 0.5, lengthY * 0.5, maxZ + 0.5)
        Direction.NORTH -> Vec3d(lengthX * 0.5, lengthY * 0.5, minZ - 0.5)
    }

@JvmSynthetic
@Suppress("LongParameterList", "NOTHING_TO_INLINE")
inline fun BlockBox.copy(
    minX: Int = this.minX,
    minY: Int = this.minY,
    minZ: Int = this.minZ,
    maxX: Int = this.maxX,
    maxY: Int = this.maxY,
    maxZ: Int = this.maxZ,
): BlockBox = BlockBox(minX, minY, minZ, maxX, maxY, maxZ)

@JvmSynthetic
fun BlockPos.expendToBlockBox(
    offsetX: Int = 0,
    offsetY: Int = 0,
    offsetZ: Int = 0,
): BlockBox = BlockBox(
    this.x - offsetX, this.y - offsetY, this.z - offsetZ,
    this.x + offsetX, this.y + offsetY, this.z + offsetZ,
)
