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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.entity.EntityPose
import net.minecraft.item.ItemStack
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

sealed class ScaffoldTechnique(name: String) : Choice(name) {
    final override val parent: ChoiceConfigurable<ScaffoldTechnique>
        get() = ModuleScaffold.technique

    abstract fun findPlacementTarget(
        predictedPos: Vec3d,
        predictedPose: EntityPose,
        optimalLine: Line?,
        bestStack: ItemStack
    ): BlockPlacementTarget?

    open fun getRotations(target: BlockPlacementTarget?) = target?.rotation

    open fun getCrosshairTarget(target: BlockPlacementTarget?, rotation: Rotation): BlockHitResult? =
        raycast(rotation)

    companion object {
        @JvmField
        internal val INVESTIGATE_DOWN_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(0, -1, 1, -2, 2)

        @JvmField
        internal val NORMAL_INVESTIGATION_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(0, -1, 1)

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
