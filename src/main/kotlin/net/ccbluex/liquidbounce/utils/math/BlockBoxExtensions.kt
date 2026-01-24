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

package net.ccbluex.liquidbounce.utils.math

import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

fun BoundingBox.iterate(): Iterable<BlockPos> =
    BlockPos.betweenClosed(minX(), minY(), minZ(), maxX(), maxY(), maxZ())

operator fun BoundingBox.iterator(): Iterator<BlockPos> = iterate().iterator()

private inline val BoundingBox.lengthX: Int get() = maxX() - minX() + 1
private inline val BoundingBox.lengthY: Int get() = maxY() - minY() + 1
private inline val BoundingBox.lengthZ: Int get() = maxZ() - minZ() + 1

val BoundingBox.size: Int get() = lengthX * lengthY * lengthZ

val BoundingBox.from: BlockPos get() = BlockPos(minX(), minY(), minZ())

val BoundingBox.to: BlockPos get() = BlockPos(minX(), minY(), minZ())

operator fun BoundingBox.contains(other: BoundingBox): Boolean =
    other.minX() >= this.minX() &&
        other.maxX() <= this.maxX() &&
        other.minY() >= this.minY() &&
        other.maxY() <= this.maxY() &&
        other.minZ() >= this.minZ() &&
        other.maxZ() <= this.maxZ()

fun ChunkAccess.toBlockBox(): BoundingBox = this.pos.toBlockBox(minY = this.minY, maxY = this.maxY)

@JvmOverloads
fun ChunkPos.toBlockBox(
    minY: Int = world.minY,
    maxY: Int = world.maxY,
): BoundingBox =
    BoundingBox(
        this.minBlockX, minY, this.minBlockZ,
        this.maxBlockX, maxY, this.maxBlockZ,
    )

val BoundingBox.boundingBox: AABB
    get() = AABB(
    minX().toDouble(), minY().toDouble(), minZ().toDouble(),
    maxX().toDouble() + 1.0, maxY().toDouble() + 1.0, maxZ().toDouble() + 1.0,
)

val BoundingBox.box: AABB
    get() = AABB(
    0.0, 0.0, 0.0,
    lengthX.toDouble(), lengthY.toDouble(), lengthZ.toDouble(),
)

fun BoundingBox.centerPointOf(side: Direction): Vec3 =
    when (side) {
        Direction.DOWN  -> Vec3(lengthX * 0.5, minY() - 0.5, lengthZ * 0.5)
        Direction.UP    -> Vec3(lengthX * 0.5, maxY() + 0.5, lengthZ * 0.5)
        Direction.EAST  -> Vec3(maxX() + 0.5, lengthY * 0.5, lengthZ * 0.5)
        Direction.WEST  -> Vec3(minX() - 0.5, lengthY * 0.5, lengthZ * 0.5)
        Direction.SOUTH -> Vec3(lengthX * 0.5, lengthY * 0.5, maxZ() + 0.5)
        Direction.NORTH -> Vec3(lengthX * 0.5, lengthY * 0.5, minZ() - 0.5)
    }

@JvmSynthetic
@Suppress("LongParameterList", "NOTHING_TO_INLINE")
inline fun BoundingBox.copy(
    minX: Int = this.minX(),
    minY: Int = this.minY(),
    minZ: Int = this.minZ(),
    maxX: Int = this.maxX(),
    maxY: Int = this.maxY(),
    maxZ: Int = this.maxZ(),
): BoundingBox = BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)

@JvmSynthetic
fun BlockPos.expendToBlockBox(
    offsetX: Int = 0,
    offsetY: Int = 0,
    offsetZ: Int = 0,
): BoundingBox = BoundingBox(
    this.x - offsetX, this.y - offsetY, this.z - offsetZ,
    this.x + offsetX, this.y + offsetY, this.z + offsetZ,
)
