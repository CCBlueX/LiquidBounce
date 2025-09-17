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

package net.ccbluex.liquidbounce.features.module.modules.world.nuker.area

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.areaMode
import net.ccbluex.liquidbounce.utils.block.isNotBreakable
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

sealed class NukerArea(name: String) : Choice(name) {

    override val parent: ChoiceConfigurable<*>
        get() = areaMode

    abstract fun lookupTargets(radius: Float, count: Int? = null): List<Pair<BlockPos, BlockState>>

    protected fun isPositionAvailable(
        eyesPos: Vec3d,
        rangeSquared: Double,
        pos: BlockPos,
        state: BlockState,
    ): Boolean {
        if (state.isNotBreakable(pos) || !ModuleNuker.isValid(state)) {
            return false
        }

        val shape = state.getCollisionShape(world, pos, ShapeContext.of(player))

        if (shape.isEmpty) {
            return false
        }

        val vec3d = shape.offset(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
            .getClosestPointTo(eyesPos)
            .orElse(null) ?: return false

        return vec3d.squaredDistanceTo(eyesPos) <= rangeSquared
    }
}
