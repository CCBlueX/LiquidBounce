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

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.utils.math

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.optionals.toList

@OptIn(ExperimentalContracts::class)
inline fun VoxelShape.ifEmpty(defaultValue: () -> VoxelShape): VoxelShape {
    contract {
        callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE)
    }
    return if (isEmpty) defaultValue() else this
}

/**
 * @return null if shape is empty
 */
fun VoxelShape.boundsOrNull(): AABB? = if (isEmpty) null else bounds()

fun VoxelShape.distanceToSqr(position: Vec3): Double =
    this.closestPointTo(position).orElse(null)?.distanceToSqr(position) ?: Double.POSITIVE_INFINITY

fun VoxelShape.clipAllBoxes(
    base: BlockPos,
    from: Vec3,
    to: Vec3,
): List<Vec3> {
    return when {
        this.isEmpty -> emptyList()

        this == Shapes.block() ->
            AABB.clip(
                base.x.toDouble(),
                base.y.toDouble(),
                base.z.toDouble(),
                1.0 + base.x,
                1.0 + base.y,
                1.0 + base.z,
                from,
                to,
            ).toList()

        else -> {
            val list = mutableListOf<Vec3>()
            this.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                AABB.clip(
                    minX + base.x,
                    minY + base.y,
                    minZ + base.z,
                    maxX + base.x,
                    maxY + base.y,
                    maxZ + base.z,
                    from,
                    to,
                ).orElse(null)?.let {
                    list.add(it)
                }
            }
            list
        }
    }
}

/**
 * Shrinks a VoxelShape by the specified amounts on selected axes.
 */
@Suppress("CognitiveComplexMethod")
fun VoxelShape.shrink(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): VoxelShape {
    return when {
        this.isEmpty -> Shapes.empty()
        this == Shapes.block() -> Shapes.box(
            x, y, z,
            1.0 - x, 1.0 - y, 1.0 - z
        )

        else -> {
            var shape = Shapes.empty()

            this.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                val width = maxX - minX
                val height = maxY - minY
                val depth = maxZ - minZ

                val canShrinkX = x == 0.0 || width > x * 2
                val canShrinkY = y == 0.0 || height > y * 2
                val canShrinkZ = z == 0.0 || depth > z * 2

                if (canShrinkX && canShrinkY && canShrinkZ) {
                    val shrunkBox = Shapes.box(
                        minX + (if (x > 0) x else 0.0),
                        minY + (if (y > 0) y else 0.0),
                        minZ + (if (z > 0) z else 0.0),
                        maxX - (if (x > 0) x else 0.0),
                        maxY - (if (y > 0) y else 0.0),
                        maxZ - (if (z > 0) z else 0.0)
                    )

                    shape = Shapes.joinUnoptimized(shape, shrunkBox, BooleanOp.OR)
                }
            }

            shape
        }
    }
}
