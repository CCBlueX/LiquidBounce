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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.core.Vec3i
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

sealed class ScaffoldTechnique(name: String) : Mode(name) {
    final override val parent: ModeValueGroup<ScaffoldTechnique>
        get() = ModuleScaffold.technique

    abstract fun findPlacementTarget(
        predictedPos: Vec3,
        predictedPose: Pose,
        optimalLine: Line?,
        bestStack: ItemStack
    ): BlockPlacementTarget?

    open fun getRotations(target: BlockPlacementTarget?) = target?.rotation

    open fun getCrosshairTarget(target: BlockPlacementTarget?, rotation: Rotation): BlockHitResult? =
        traceFromPlayer(rotation)

    companion object {
        @JvmField
        internal val INVESTIGATE_DOWN_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(0, -1, 1, -2, 2)

        @JvmField
        internal val NORMAL_INVESTIGATION_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(0, -1, 1)

        @JvmField
        internal val FULL_INVESTIGATION_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(0, -1, 1, -2, 2, -3, 3, -4, 4)

        private fun commonOffsetToInvestigate(vararg xzOffsets: Int): List<Vec3i> = buildList(xzOffsets.size.sq() * 2) {
            for (x in xzOffsets) {
                for (z in xzOffsets) {
                    add(Vec3i(x, 0, z))
                    add(Vec3i(x, -1, z))
                }
            }
        }
    }
}
