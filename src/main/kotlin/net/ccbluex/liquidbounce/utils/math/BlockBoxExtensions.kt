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
private inline val BoundingBox.centerX: Double get() = minX() + lengthX * 0.5
private inline val BoundingBox.centerY: Double get() = minY() + lengthY * 0.5
private inline val BoundingBox.centerZ: Double get() = minZ() + lengthZ * 0.5

val BoundingBox.size: Int get() = lengthX * lengthY * lengthZ

val BoundingBox.from: BlockPos get() = BlockPos(minX(), minY(), minZ())

val BoundingBox.to: BlockPos get() = BlockPos(maxX(), maxY(), maxZ())

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

fun BoundingBox.centerOnSide(side: Direction): Vec3 =
    when (side) {
        Direction.DOWN  -> Vec3(centerX, minY() - 0.5, centerZ)
        Direction.UP    -> Vec3(centerX, maxY() + 0.5, centerZ)
        Direction.EAST  -> Vec3(maxX() + 0.5, centerY, centerZ)
        Direction.WEST  -> Vec3(minX() - 0.5, centerY, centerZ)
        Direction.SOUTH -> Vec3(centerX, centerY, maxZ() + 0.5)
        Direction.NORTH -> Vec3(centerX, centerY, minZ() - 0.5)
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
fun BlockPos.expandToBoundingBox(
    offsetX: Int = 0,
    offsetY: Int = 0,
    offsetZ: Int = 0,
): BoundingBox = BoundingBox(
    this.x - offsetX, this.y - offsetY, this.z - offsetZ,
    this.x + offsetX, this.y + offsetY, this.z + offsetZ,
)
