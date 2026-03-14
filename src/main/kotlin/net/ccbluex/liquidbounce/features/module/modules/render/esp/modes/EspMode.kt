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

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.fastutil.Pool
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP.modes
import net.ccbluex.liquidbounce.render.EMPTY_BOX
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

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            pool.recycleAll(prepared)
            prepared.clear()
            for (entity in RenderedEntities) {
                if (!shouldRender(entity)) continue

                val dimensions = entity.getDimensions(entity.pose)
                val halfWidth = dimensions.width.toDouble() / 2.0
                val localBox = AABB(
                    -halfWidth, 0.0, -halfWidth,
                    halfWidth, dimensions.height.toDouble(), halfWidth
                ).inflate(expand.toDouble())

                val state = pool.borrow()
                state.entity = entity
                state.localBox = localBox
                prepared.add(state)
            }
        }

        override fun disable() {
            super.disable()
            pool.recycleAll(prepared)
            prepared.clear()
        }

        protected class BoxBasedEspRenderState {
            @JvmField var entity: LivingEntity? = null
            @JvmField var localBox: AABB = EMPTY_BOX
            @JvmField var position: Vec3 = Vec3.ZERO
            @JvmField var worldBox: AABB = EMPTY_BOX

            fun update(tickDelta: Float) {
                position = entity?.interpolateCurrentPosition(tickDelta) ?: Vec3.ZERO
                worldBox = localBox.move(position)
            }

            fun reset() {
                entity = null
                localBox = EMPTY_BOX
                position = Vec3.ZERO
                worldBox = EMPTY_BOX
            }

            operator fun component1() = entity!!
            operator fun component2() = localBox
            operator fun component3() = position
            operator fun component4() = worldBox
        }

        protected fun collectPreparedBoxes(tickDelta: Float): List<BoxBasedEspRenderState> {
            for (i in prepared.indices) {
                prepared[i].update(tickDelta)
            }
            return prepared
        }

        private companion object {
            private val pool = Pool(::BoxBasedEspRenderState, BoxBasedEspRenderState::reset)

            private val prepared = ObjectArrayList<BoxBasedEspRenderState>()
        }
    }
}
