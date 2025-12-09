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
package net.ccbluex.liquidbounce.utils.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.lastRenderPos
import net.ccbluex.liquidbounce.utils.math.interpolate
import net.ccbluex.liquidbounce.utils.render.WorldToScreen.calculateScreenPos
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f
import java.awt.Color
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


/**
 * A target tracker to choose the best enemy to attack
 */
sealed class TargetRenderer<Ctx: Any>(
    module: ClientModule
) : ToggleableConfigurable(module, "TargetRendering", true) {

    init {
        doNotIncludeAlways()
    }

    abstract val appearance: ChoiceConfigurable<out TargetRenderAppearance<in Ctx>>

    context(env: Ctx)
    fun render(entity: Entity, partialTicks: Float) {
        if (!enabled) {
            return
        }

        appearance.activeChoice.render(entity, partialTicks)
    }

}

private val ghostModeTexture = LiquidBounce.resource("particles/glow.png")
    .toNativeImage().asTexture { "TargetRenderer Ghost" }

class WorldTargetRenderer(module: ClientModule) : TargetRenderer<WorldRenderEnvironment>(module) {

    override val appearance = choices(module, "Mode", 2) {
        arrayOf(Legacy(), Circle(module), GlowingCircle(module), Ghost())
    }

    inner class Ghost : WorldTargetRenderAppearance("Ghost") {

        private var lastTime = System.currentTimeMillis()

        override val parent: ChoiceConfigurable<*>
            get() = appearance

        private val color by color("Color", Color4b(Color.BLUE.rgb, true))
        private val size by float("Size", 0.5f, 0.4f..0.7f)
        private val length by int("Length", 25, 15..40)

        context(env: WorldRenderEnvironment)
        override fun render(entity: Entity, partialTicks: Float) {
            env.matrixStack.push()
            mc.gameRenderer.lightmapTextureManager.disable()

            env.matrixStack.translate(mc.gameRenderer.camera.pos.negate())

            val interpolated = entity.pos.interpolate(entity.lastRenderPos(), partialTicks.toDouble())
                .add(0.2, 1.25, 0.0)

            env.matrixStack.translate(interpolated)

            with(env) {
                RenderSystem.setShaderTexture(0, ghostModeTexture.glTextureView)
                startBatch()
                drawParticle(
                    { sin, cos -> Vec3d(sin, cos, -cos) },
                    { sin, cos -> Vec3d(-sin, -cos, cos) }
                )

                drawParticle(
                    { sin, cos -> Vec3d(-sin, sin, -cos) },
                    { sin, cos -> Vec3d(sin, -sin, cos) }
                )

                drawParticle(
                    { sin, cos -> Vec3d(-sin, -sin, cos) },
                    { sin, cos -> Vec3d(sin, sin, -cos) }
                )
                commitBatch()
            }

            mc.gameRenderer.lightmapTextureManager.enable()
            env.matrixStack.pop()
        }

        private inline fun WorldRenderEnvironment.drawParticle(
            translationsBefore: MatrixStack.(Double, Double) -> Vec3d,
            translateAfter: MatrixStack.(Double, Double) -> Vec3d
        ) {
            val radius = 0.67
            val distance = 10.0 + (length * 0.2)
            val alphaFactor = 15

            for (i in 0..<length) {
                val angle: Double = 0.15f * (System.currentTimeMillis() - lastTime - (i * distance)) / (30)
                val sin = sin(angle) * radius
                val cos = cos(angle) * radius

                with(matrixStack) {
                    translate(translationsBefore(sin, cos))

                    translate(-size / 2.0, -size / 2.0, 0.0)
                    multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.camera.yaw))
                    multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.camera.pitch))
                    translate(size / 2.0, size / 2.0, 0.0)
                }

                val alpha = MathHelper.clamp(color.a - (i * alphaFactor), 0, color.a)
                val renderColor = color.alpha(alpha)

                drawSquareTexture(size, renderColor.toARGB())

                with(matrixStack) {
                    translate(-size / 2.0, -size / 2.0, 0.0)
                    multiply(RotationAxis.POSITIVE_X.rotationDegrees(-mc.gameRenderer.camera.pitch))
                    multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mc.gameRenderer.camera.yaw))
                    translate(size / 2.0, size / 2.0, 0.0)

                    translate(translateAfter(sin, cos))
                }
            }
        }
    }

    inner class Legacy : WorldTargetRenderAppearance("Legacy") {

        override val parent: ChoiceConfigurable<*>
            get() = appearance

        private val size by float("Size", 0.5f, 0.1f..2f)

        private val height by float("Height", 0.1f, 0.02f..2f)

        private val color by color("Color", Color4b(0x64007CFF, true))

        private val extraYOffset by float("ExtraYOffset", 0.1f, 0f..1f)

        context(env: WorldRenderEnvironment)
        override fun render(entity: Entity, partialTicks: Float) {
            val box = Box(
                -size.toDouble(), 0.0, -size.toDouble(),
                size.toDouble(), height.toDouble(), size.toDouble()
            )

            val pos = entity.interpolateCurrentPosition(partialTicks)
                .add(0.0, entity.height.toDouble() + extraYOffset.toDouble(), 0.0)

            with(env) {
                withPositionRelativeToCamera(pos) {
                    drawBox(box, color)
                }
            }
        }
    }

    inner class Circle(module: ClientModule) : WorldTargetRenderAppearance("Circle") {
        override val parent: ChoiceConfigurable<*>
            get() = appearance

        private val radius by float("Radius", 0.85f, 0.1f..2f)
        private val innerRadius by float("InnerRadius", 0f, 0f..2f)
            .onChange { min(radius, it) }

        private val heightMode = choices(module, "HeightMode") {
            arrayOf(
                HeightMode.Feet(it),
                HeightMode.Top(it),
                HeightMode.Relative(it),
                HeightMode.Health(it),
                HeightMode.Animated(it),
            )
        }

        private val outerColor by color("OuterColor", Color4b(0x64007CFF, true))
        private val innerColor by color("InnerColor", Color4b(0x64007CFF, true))

        private val outline = tree(Outline())

        context(env: WorldRenderEnvironment)
        override fun render(entity: Entity, partialTicks: Float) {
            val height = heightMode.activeChoice.getHeight(entity, partialTicks)
            val pos = entity.interpolateCurrentPosition(partialTicks).add(0.0, height, 0.0)

            with(env) {
                startBatch()
                withPositionRelativeToCamera(pos) {
                    drawGradientCircle(radius, innerRadius, outerColor, innerColor)
                    if (outline.enabled) {
                        drawCircleOutline(radius, outline.color)
                    }
                }
                commitBatch()
            }
        }

    }

    inner class GlowingCircle(module: ClientModule) : WorldTargetRenderAppearance("GlowingCircle") {
        override val parent: ChoiceConfigurable<*>
            get() = appearance

        private val radius by float("Radius", 0.85f, 0.1f..2f)

        private val heightMode = choices(module, "HeightMode") {
            arrayOf(
                HeightMode.Feet(it),
                HeightMode.Top(it),
                HeightMode.Relative(it),
                HeightMode.Health(it),
                HeightMode.Animated(it),
            )
        }

        private val color by color("OuterColor", Color4b(0x64007CFF, true))
        private val glowColor by color("GlowColor", Color4b(0x00007CFF, true))

        private val glowHeightSetting by float("GlowHeight", 0.3f, -1f..1f)

        private val outline = tree(Outline())

        context(env: WorldRenderEnvironment)
        override fun render(entity: Entity, partialTicks: Float) {
            val height = heightMode.activeChoice.getHeight(entity, partialTicks)
            val pos = entity.interpolateCurrentPosition(partialTicks).add(0.0, height, 0.0)

            val currentHeightMode = heightMode.activeChoice

            val glowHeight = if (currentHeightMode is HeightMode.WithGlow) {
                currentHeightMode.getGlowHeight(entity, partialTicks) - height
            } else {
                glowHeightSetting.toDouble()
            }

            with(env) {
                startBatch()
                withPositionRelativeToCamera(pos) {
                    drawGradientCircle(
                        radius,
                        radius,
                        color,
                        glowColor,
                        Vector3f(0f, glowHeight.toFloat(), 0f)
                    )

                    drawGradientCircle(
                        radius,
                        0f,
                        color,
                        color
                    )
                    if (outline.enabled) {
                        drawCircleOutline(radius, outline.color)
                    }
                }
                commitBatch()
            }
        }

    }

    inner class Outline : ToggleableConfigurable(parent, "Outline", true) {
        val color by color("Color", Color4b(0x00007CFF, false))
    }

}

class OverlayTargetRenderer(module: ClientModule) : TargetRenderer<DrawContext>(module) {
    override val appearance = choices<TargetRenderAppearance<DrawContext>>(module, "Mode") {
        arrayOf(Arrow())
    }

    private inner class Arrow : OverlayTargetRenderAppearance("Arrow") {

        override val parent: ChoiceConfigurable<TargetRenderAppearance<DrawContext>>
            get() = appearance

        private val color by color("Color", Color4b.RED)
        private val outlineColor by color("OutlineColor", Color4b.TRANSPARENT)
        private val size by float("Size", 1.5f, 0.5f..20f)

        context(ctx: DrawContext)
        override fun render(entity: Entity, partialTicks: Float) {
            val pos = entity.interpolateCurrentPosition(partialTicks)
                .add(0.0, entity.height.toDouble(), 0.0)

            val screenPos = calculateScreenPos(pos) ?: return
            val minX = screenPos.x - 5 * size
            val midX = screenPos.x
            val maxX = screenPos.x + 5 * size
            val minY = screenPos.y - 10 * size
            val maxY = screenPos.y
            ctx.drawTriangle(
                Vec2f(minX, minY),
                Vec2f(midX, maxY),
                Vec2f(maxX, minY),
                color,
                outlineColor,
            )
        }
    }
}

sealed class TargetRenderAppearance<Ctx: Any>(name: String) : Choice(name) {
    context(ctx: Ctx)
    open fun render(entity: Entity, partialTicks: Float) {}
}

sealed class WorldTargetRenderAppearance(name: String) : TargetRenderAppearance<WorldRenderEnvironment>(name)
sealed class OverlayTargetRenderAppearance(name: String) : TargetRenderAppearance<DrawContext>(name)

sealed class HeightMode(name: String) : Choice(name) {
    abstract fun getHeight(entity: Entity, partialTicks: Float): Double

    interface WithGlow {
        fun getGlowHeight(entity: Entity, partialTicks: Float): Double
    }

    class Feet(override val parent: ChoiceConfigurable<*>) : HeightMode("Feet") {
        private val offset by float("Offset", 0f, -1f..1f)
        override fun getHeight(entity: Entity, partialTicks: Float): Double = offset.toDouble()
    }

    class Top(override val parent: ChoiceConfigurable<*>) : HeightMode("Top") {
        private val offset by float("Offset", 0f, -1f..1f)
        override fun getHeight(entity: Entity, partialTicks: Float) = entity.box.maxY - entity.box.minY + offset
    }

    // Lets the user chose the height relative to the entity's height
    // Use 1 for it to always be at the top of the entity
    // Use 0 for it to always be at the feet of the entity

    class Relative(override val parent: ChoiceConfigurable<*>) : HeightMode("Relative") {
        private val height by float("Height", 0.5f, -0.5f..1.5f)

        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            val box = entity.box
            val entityHeight = box.maxY - box.minY
            return height * entityHeight
        }
    }

    class Health(override val parent: ChoiceConfigurable<*>) : HeightMode("Health") {
        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            if (entity !is LivingEntity) return 0.0
            val box = entity.box
            val entityHeight = box.maxY - box.minY
            return entity.health / entity.maxHealth * entityHeight
        }
    }

    class Animated(override val parent: ChoiceConfigurable<*>) : HeightMode("Animated"), WithGlow {
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
