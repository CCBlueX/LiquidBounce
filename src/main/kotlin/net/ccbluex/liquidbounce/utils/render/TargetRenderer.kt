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
import net.ccbluex.liquidbounce.config.util.Exclude
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Box
import kotlin.math.min
import kotlin.math.sin

/**
 * A target tracker to choose the best enemy to attack
 */
abstract class TargetRenderer(module: Module) : ToggleableConfigurable(module, "TargetRendering", true) {

    abstract val appearance: ChoiceConfigurable

    open fun render(env: RenderEnvironment, entity: Entity, partialTicks: Float) {
        (appearance.activeChoice as TargetRenderAppearance).render(env, entity, partialTicks)
    }
}


class WorldTargetRenderer(module: Module) : TargetRenderer(module) {

    override val appearance = choices(module, "Mode", Legacy(), arrayOf(Legacy(), Circle(module), GlowingCircle(module)))

    inner class Legacy : WorldTargetRenderAppearance("Legacy") {

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

    inner class Circle(module: Module) : WorldTargetRenderAppearance("Circle") {
        override val parent: ChoiceConfigurable
            get() = appearance

        private val radius by float("Radius", 0.85f, 0.1f..2f)
        private val innerRadius by float("InnerRadius", 0f, 0f..2f)
            .listen { min(radius, it) }

        private val heightMode = choices(
            module,
            "HeightMode",
            { FeetHeight(it) },
            { arrayOf(FeetHeight(it), TopHeight(it), RelativeHeight(it), HealthHeight(it)) }
        )

        private val outerColor by color("OuterColor", Color4b(0x64007CFF, true))
        private val innerColor by color("InnerColor", Color4b(0x64007CFF, true))

        private val outline = tree(Outline())


        override fun render(env: RenderEnvironment, entity: Entity, partialTicks: Float) {
            val height = (heightMode.activeChoice as HeightMode).getHeight(entity, partialTicks)
            val pos =
                entity.interpolateCurrentPosition(partialTicks).toVec3() +
                    Vec3(0.0, height, 0.0)

            with(env) {
                withPosition(pos) {
                    withDisabledCull {
                        drawGradientCircle(radius, innerRadius, outerColor, innerColor)
                    }
                    if(outline.enabled) {
                        drawCircleOutline(radius, outline.color)
                    }
                }
            }
        }

    }

    inner class GlowingCircle(module: Module) : WorldTargetRenderAppearance("GlowingCircle") {
        override val parent: ChoiceConfigurable
            get() = appearance

        private val radius by float("Radius", 0.85f, 0.1f..2f)

        private val heightMode = choices(
            module,
            "HeightMode",
            { FeetHeight(it) },
            { arrayOf(FeetHeight(it), TopHeight(it), RelativeHeight(it), HealthHeight(it), AnimatedHeight(it)) }
        )

        private val color by color("OuterColor", Color4b(0x64007CFF, true))
        private val glowColor by color("GlowColor", Color4b(0x00007CFF, true))

        private val glowHeightSetting by float("GlowHeight", 0.3f, -1f..1f)

        private val outline = tree(Outline())


        override fun render(env: RenderEnvironment, entity: Entity, partialTicks: Float) {
            val height = (heightMode.activeChoice as HeightMode).getHeight(entity, partialTicks)
            val pos =
                entity.interpolateCurrentPosition(partialTicks).toVec3() +
                    Vec3(0.0, height, 0.0)


            val currentHeightMode = heightMode.activeChoice

            val glowHeight = if(currentHeightMode is HeightWithGlow)
                currentHeightMode.getGlowHeight(entity, partialTicks) - height
            else
                glowHeightSetting.toDouble()

            with(env) {
                withPosition(pos) {
                    withDisabledCull {
                        drawGradientCircle(
                            radius,
                            radius,
                            color,
                            glowColor,
                            Vec3(0.0, glowHeight, 0.0))

                        drawGradientCircle(
                            radius,
                            0f,
                            color,
                            color)
                    }
                    if(outline.enabled) {
                        drawCircleOutline(radius, outline.color)
                    }
                }
            }
        }

    }

    inner class Outline : ToggleableConfigurable(module,"Outline", true) {
        val color by color("Color", Color4b(0x00007CFF, false))
    }

    inner class FeetHeight(private val choiceConfigurable: ChoiceConfigurable) : HeightMode("Feet") {
        override val parent: ChoiceConfigurable
            get() = choiceConfigurable

        val offset: Float by float("Offset", 0f, -1f..1f)

        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            return offset.toDouble()
        }

    }

    inner class TopHeight(private val choiceConfigurable: ChoiceConfigurable) : HeightMode("Top") {
        override val parent: ChoiceConfigurable
            get() = choiceConfigurable

        val offset by float("Offset", 0f, -1f..1f)
        override fun getHeight(entity: Entity, partialTicks: Float) = entity.box.maxY - entity.box.minY + offset
    }

    // Lets the user chose the height relative to the entity's height
    // Use 1 for it to always be at the top of the entity
    // Use 0 for it to always be at the feet of the entity

    inner class RelativeHeight(private val choiceConfigurable: ChoiceConfigurable) : HeightMode("Relative") {
        override val parent: ChoiceConfigurable
            get() = choiceConfigurable

        private val height by float("Height", 0.5f, -0.5f..1.5f)

        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            val box = entity.box
            val entityHeight = box.maxY - box.minY
            return height * entityHeight
        }
    }

    inner class HealthHeight(private val choiceConfigurable: ChoiceConfigurable) : HeightMode("Health") {
        override val parent: ChoiceConfigurable
            get() = choiceConfigurable



        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            if(entity !is LivingEntity) return 0.0
            val box = entity.box
            val entityHeight = box.maxY - box.minY
            return entity.health / entity.maxHealth * entityHeight
        }
    }

    inner class AnimatedHeight(private val choiceConfigurable: ChoiceConfigurable) : HeightWithGlow("Animated") {
        override val parent: ChoiceConfigurable
            get() = choiceConfigurable

        private val speed by float("Speed", 0.18f, 0.01f..1f)
        private val heightMultiplier by float("HeightMultiplier", 0.4f, 0.1f..1f)
        private val heightOffset by float("HeightOffset", 1.3f, 0f..2f)
        private val glowOffset by float("GlowOffset", -1f, -3.1f..3.1f)

        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            return calculateHeight((entity.age + partialTicks) * speed)
        }

        override fun getGlowHeight(entity: Entity, partialTicks: Float): Double {
            return calculateHeight((entity.age + partialTicks) * speed + glowOffset)
        }

        private fun calculateHeight(time: Float) =
            (sin(time) * heightMultiplier + heightOffset).toDouble()
    }
}

class OverlayTargetRenderer(module: Module) : TargetRenderer(module) {
    override val appearance = choices(module, "Mode", Legacy())

    inner class Legacy : WorldTargetRenderAppearance("Legacy") {

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
}

abstract class TargetRenderAppearance(name: String) : Choice(name) {
    open fun render(env: RenderEnvironment, entity: Entity, partialTicks: Float) {}
}

abstract class WorldTargetRenderAppearance(name: String) : TargetRenderAppearance(name)
abstract class OverlayTargetRenderAppearance(name: String) : TargetRenderAppearance(name)

abstract class HeightMode(name: String) : Choice(name) {
    open fun getHeight(entity: Entity, partialTicks: Float): Double = 0.0
}

abstract class HeightWithGlow(name: String) : HeightMode(name) {
    open fun getGlowHeight(entity: Entity, partialTicks: Float): Double = 0.0

}

