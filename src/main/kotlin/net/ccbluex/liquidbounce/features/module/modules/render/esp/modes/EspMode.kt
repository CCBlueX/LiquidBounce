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
package net.ccbluex.liquidbounce.features.module.modules.render.esp.modes

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP.modes
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.cameraDistanceSq
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

sealed class EspMode(
    name: String,
    val requiresTrueSight: Boolean = false
) : Mode(name) {
    final override val parent
        get() = modes

    fun shouldRender(entity: Entity?): Boolean {
        return entity != null && entity.position().cameraDistanceSq() < ModuleESP.maximumDistance.sq()
    }

    sealed class BoxBased(name: String) : EspMode(name) {
        protected val expand by float("Expand", 0.05f, 0f..0.5f)

        protected data class PreparedBox(
            @JvmField val entity: LivingEntity,
            @JvmField val localBox: AABB,
            @JvmField val position: Vec3,
            @JvmField val worldBox: AABB
        )

        protected fun collectPreparedBoxes(tickDelta: Float): List<PreparedBox> {
            val prepared = ArrayList<PreparedBox>(RenderedEntities.size)

            for (entity in RenderedEntities) {
                if (!shouldRender(entity)) continue

                val dimensions = entity.getDimensions(entity.pose)
                val halfWidth = dimensions.width.toDouble() / 2.0
                val localBox = AABB(
                    -halfWidth, 0.0, -halfWidth,
                    halfWidth, dimensions.height.toDouble(), halfWidth
                ).inflate(expand.toDouble())

                val position = entity.interpolateCurrentPosition(tickDelta)
                prepared += PreparedBox(entity, localBox, position, localBox.move(position))
            }

            return prepared
        }
    }
}
