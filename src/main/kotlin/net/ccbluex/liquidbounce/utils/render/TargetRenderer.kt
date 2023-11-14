/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.entity.Entity
import net.minecraft.util.math.Box
import kotlin.math.min

/**
 * A target tracker to choose the best enemy to attack
 */
class TargetRenderer(module: Module) : ToggleableConfigurable(module, "TargetRendering", true) {

    val appearance = choices(module, "Mode", Legacy(), arrayOf(Legacy(), Circle(module)))

    fun render(env: RenderEnvironment, entity: Entity, partialTicks: Float) {
        ((appearance.activeChoice) as TargetRenderAppearance).render(env, entity, partialTicks)
    }

    inner class Legacy() : TargetRenderAppearance("Legacy") {

        override val parent: ChoiceConfigurable
            get() = appearance

        private val size by float("Size", 0.5f, 0.1f..2f)

        private val height by float("Height", 0.1f, 0.02f..2f)

        private val color by color("Color", Color4b(0x64007CFF, true))

        private val extraYOffset by float("ExtraYOffset", 0.1f, 0f..1f)
        override fun render(env: RenderEnvironment, entity: Entity, partialTicks: Float) {
            val box = Box(
                -size.toDouble(), 0.0, -size.toDouble(),
                size.toDouble(), height.toDouble(), size.toDouble()
            )

            val pos =
                entity.interpolateCurrentPosition(partialTicks).toVec3() +
                    Vec3(0.0, entity.height.toDouble() + extraYOffset.toDouble(), 0.0)


            with(env) {
                withColor(color) {
                    withPosition(pos) {
                        drawSolidBox(box)
                    }
                }
            }
        }
    }

    inner class Circle(module: Module) : TargetRenderAppearance("Circle") {
        override val parent: ChoiceConfigurable
            get() = appearance

        private val radius by float("Radius", 0.85f, 0.1f..2f)
        private val innerRadius by float("InnerRadius", 0f, 0f..2f)
            .listen { min(radius, it) }

        private val heightMode = choices(module, "HeightMode", FeetHeight(), arrayOf(FeetHeight(), TopHeight(), RelativeHeight()))

        private val outerColor by color("OuterColor", Color4b(0x64007CFF, true))
        private val innerColor by color("InnerColor", Color4b(0x64007CFF, true))
        override fun render(env: RenderEnvironment, entity: Entity, partialTicks: Float) {
            val height = (heightMode.activeChoice as HeightMode).getHeight(entity)
            val pos =
                entity.interpolateCurrentPosition(partialTicks).toVec3() +
                    Vec3(0.0, height, 0.0)

            with(env) {
                withPosition(pos) {
                    withDisabledCull {
                        drawGradientCircle(radius, innerRadius, outerColor, innerColor)
                    }
                }
            }
        }
        inner class FeetHeight : HeightMode("Feet") {
            override val parent: ChoiceConfigurable
                get() = heightMode
        }

        inner class TopHeight : HeightMode("Top") {
            override val parent: ChoiceConfigurable
                get() = heightMode
            override fun getHeight(entity: Entity) = entity.box.maxY - entity.box.minY
        }

        inner class RelativeHeight : HeightMode("Relative") {
            override val parent: ChoiceConfigurable
                get() = heightMode

            private val height by float("Height", 0.5f, 0f..1f)

            override fun getHeight(entity: Entity): Double {
                val box = entity.box
                val entityHeight = box.maxY - box.minY
                return height * entityHeight
            }
        }
    }




}

abstract class TargetRenderAppearance(name: String) : Choice(name) {
    open fun render(env: RenderEnvironment, entity: Entity, partialTicks: Float) {}
}

abstract class HeightMode(name: String) : Choice(name) {
    open fun getHeight(entity: Entity): Double = 0.0
}


