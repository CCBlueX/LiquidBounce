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

package net.ccbluex.liquidbounce.features.module.modules.world.nuker.area

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.areaMode
import net.ccbluex.liquidbounce.utils.block.isNotBreakable
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext

sealed class NukerArea(name: String) : Mode(name) {

    override val parent: ModeValueGroup<*>
        get() = areaMode

    abstract fun lookupTargets(radius: Float, count: Int? = null): List<Pair<BlockPos, BlockState>>

    protected fun isPositionAvailable(
        eyesPos: Vec3,
        rangeSquared: Double,
        pos: BlockPos,
        state: BlockState,
    ): Boolean {
        if (state.isNotBreakable(pos) || !ModuleNuker.isValid(state)) {
            return false
        }

        val shape = state.getShape(world, pos, CollisionContext.of(player))

        if (shape.isEmpty) {
            return false
        }

        val vec3d = shape.move(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
            .closestPointTo(eyesPos)
            .orElse(null) ?: return false

        return vec3d.distanceToSqr(eyesPos) <= rangeSquared
    }
}
