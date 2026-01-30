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
package net.ccbluex.liquidbounce.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.toTextureProperty
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawCircleOutline
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.drawSquareTexture
import net.ccbluex.liquidbounce.render.drawTexQuad
import net.ccbluex.liquidbounce.render.drawTriangle
import net.ccbluex.liquidbounce.render.engine.font.HorizontalAnchor
import net.ccbluex.liquidbounce.render.engine.font.VerticalAnchor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.plus
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.lastRenderPos
import net.ccbluex.liquidbounce.utils.render.WorldToScreen.calculateScreenPos
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Style
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A target tracker to choose the best enemy to attack
 */
class TargetRenderer(
    owner: ToggleableValueGroup,
    val target: () -> Entity?,
) : ToggleableValueGroup(owner, "TargetRendering", true) {

    constructor(module: ToggleableValueGroup, targetTracker: TargetTracker) : this(module, targetTracker::target)

    init {
        doNotIncludeAlways()
    }

    private val appearance = modes(owner, "Mode", 2) {
        arrayOf(
            TargetRenderAppearance.World.Legacy(it),
            TargetRenderAppearance.World.Circle(owner, it),
            TargetRenderAppearance.World.GlowingCircle(owner, it),
            TargetRenderAppearance.World.Ghost(it),
            TargetRenderAppearance.Gui.Text(owner, it),
            TargetRenderAppearance.Gui.Image(owner, it),
            TargetRenderAppearance.Gui.Arrow(it),
        )
    }

    @Suppress("unused")
    private val worldRenderHandler = handler<WorldRenderEvent> { event ->
        val mode = appearance.activeMode as? TargetRenderAppearance.World ?: return@handler

        val target = target() ?: return@handler

        with(mode) {
            renderEnvironmentForWorld(event.matrixStack) {
                render(target, event.partialTicks)
            }
        }
    }

    @Suppress("unused")
    private val guiRenderHandler = handler<OverlayRenderEvent> { event ->
        val mode = appearance.activeMode as? TargetRenderAppearance.Gui ?: return@handler

        val target = target() ?: return@handler

        with(mode) {
            event.context.render(target, event.tickDelta)
        }
    }

    private sealed class TargetRenderAppearance<Ctx : Any>(name: String) : Mode(name) {
        abstract fun Ctx.render(entity: Entity, partialTicks: Float)

        sealed class World(name: String) : TargetRenderAppearance<WorldRenderEnvironment>(name) {

            class Ghost(override val parent: ModeValueGroup<*>) : World("Ghost") {

                private var lastTime = System.currentTimeMillis()

                private val color by color("Color", Color4b.BLUE)
                private val size by float("Size", 0.5f, 0.4f..0.7f)
                private val length by int("Length", 25, 15..40)

                override fun WorldRenderEnvironment.render(entity: Entity, partialTicks: Float) {
                    matrixStack.pushPose()

                    matrixStack.translate(mc.gameRenderer.mainCamera.position().reverse())

                    val interpolated = entity.lastRenderPos().lerp(entity.position(), partialTicks.toDouble())
                        .add(0.2, 1.25, 0.0)

                    matrixStack.translate(interpolated)

                    with(this) {
                        startBatch()
                        drawParticle(
                            { sin, cos -> Vec3(sin, cos, -cos) },
                            { sin, cos -> Vec3(-sin, -cos, cos) }
                        )

                        drawParticle(
                            { sin, cos -> Vec3(-sin, sin, -cos) },
                            { sin, cos -> Vec3(sin, -sin, cos) }
                        )

                        drawParticle(
                            { sin, cos -> Vec3(-sin, -sin, cos) },
                            { sin, cos -> Vec3(sin, sin, -cos) }
                        )
                        commitBatch()
                    }

                    matrixStack.popPose()
                }

                private inline fun WorldRenderEnvironment.drawParticle(
                    translationsBefore: PoseStack.(Double, Double) -> Vec3,
                    translateAfter: PoseStack.(Double, Double) -> Vec3
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
                            mulPose(Axis.YP.rotationDegrees(-camera.yRot()))
                            mulPose(Axis.XP.rotationDegrees(camera.xRot()))
                            translate(size / 2.0, size / 2.0, 0.0)
                        }

                        val alpha = Mth.clamp(color.a - (i * alphaFactor), 0, color.a)
                        val renderColor = color.alpha(alpha)

                        drawSquareTexture(ghostModeTexture, size, renderColor.argb)

                        with(matrixStack) {
                            translate(-size / 2.0, -size / 2.0, 0.0)
                            mulPose(Axis.XP.rotationDegrees(-camera.xRot()))
                            mulPose(Axis.YP.rotationDegrees(camera.yRot()))
                            translate(size / 2.0, size / 2.0, 0.0)

                            translate(translateAfter(sin, cos))
                        }
                    }
                }
            }

            class Legacy(override val parent: ModeValueGroup<*>) : World("Legacy") {

                private val size by float("Size", 0.5f, 0.1f..2f)

                private val height by float("Height", 0.1f, 0.02f..2f)

                private val color by color("Color", defaultColor)

                private val extraYOffset by float("ExtraYOffset", 0.1f, 0f..1f)

                override fun WorldRenderEnvironment.render(entity: Entity, partialTicks: Float) {
                    val box = AABB(
                        -size.toDouble(), 0.0, -size.toDouble(),
                        size.toDouble(), height.toDouble(), size.toDouble()
                    )

                    val pos = entity.interpolateCurrentPosition(partialTicks)
                        .add(0.0, entity.bbHeight.toDouble() + extraYOffset.toDouble(), 0.0)

                    with(this) {
                        withPositionRelativeToCamera(pos) {
                            drawBox(box, color)
                        }
                    }
                }
            }

            class Circle(owner: ToggleableValueGroup, override val parent: ModeValueGroup<*>) : World("Circle") {

                private val radius by float("Radius", 0.85f, 0.1f..2f)
                private val innerRadius by float("InnerRadius", 0f, 0f..2f)
                    .onChange { min(radius, it) }

                private val heightMode = modes(owner, "HeightMode") {
                    arrayOf(
                        HeightMode.Feet(it),
                        HeightMode.Top(it),
                        HeightMode.Relative(it),
                        HeightMode.Health(it),
                        HeightMode.Animated(it),
                    )
                }

                private val outerColor by color("OuterColor", defaultColor)
                private val innerColor by color("InnerColor", defaultColor)

                private val outline = tree(Outline(owner))

                override fun WorldRenderEnvironment.render(entity: Entity, partialTicks: Float) {
                    val height = heightMode.activeMode.getHeight(entity, partialTicks)
                    val pos = entity.interpolateCurrentPosition(partialTicks).add(0.0, height, 0.0)

                    with(this) {
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

            class GlowingCircle(owner: ToggleableValueGroup, override val parent: ModeValueGroup<*>) :
                World("GlowingCircle") {
                private val radius by float("Radius", 0.85f, 0.1f..2f)

                private val heightMode = modes(owner, "HeightMode") {
                    arrayOf(
                        HeightMode.Feet(it),
                        HeightMode.Top(it),
                        HeightMode.Relative(it),
                        HeightMode.Health(it),
                        HeightMode.Animated(it),
                    )
                }

                private val color by color("OuterColor", defaultColor)
                private val glowColor by color("GlowColor", Color4b.LIQUID_BOUNCE.alpha(0))

                private val glowHeightSetting by float("GlowHeight", 0.3f, -1f..1f)

                private val outline = tree(Outline(owner))

                override fun WorldRenderEnvironment.render(entity: Entity, partialTicks: Float) {
                    val height = heightMode.activeMode.getHeight(entity, partialTicks)
                    val pos = entity.interpolateCurrentPosition(partialTicks).add(0.0, height, 0.0)

                    val currentHeightMode = heightMode.activeMode

                    val glowHeight = if (currentHeightMode is HeightMode.WithGlow) {
                        currentHeightMode.getGlowHeight(entity, partialTicks) - height
                    } else {
                        glowHeightSetting.toDouble()
                    }

                    with(this) {
                        withPositionRelativeToCamera(pos) {
                            // Don't use batch mode because `drawGradientCircle` uses TRIANGLE_STRIP
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
                    }
                }

            }

            private class Outline(parent: EventListener) : ToggleableValueGroup(parent, "Outline", true) {
                val color by color("Color", Color4b.fullAlpha(0x007CFF))
            }
        }

        sealed class Gui(name: String) : TargetRenderAppearance<GuiGraphics>(name) {

            class Image(owner: ToggleableValueGroup, override val parent: ModeValueGroup<*>) : Gui("Image") {

                private val scale by float("Scale", 1f, 0.01f..10f)
                private val color by color("ColorModulator", Color4b.WHITE)
                private val texture by file("File").toTextureProperty(owner)

                private val heightMode = modes(owner, "HeightMode") {
                    arrayOf(
                        HeightMode.Feet(it),
                        HeightMode.Top(it),
                        HeightMode.Relative(it),
                        HeightMode.Health(it),
                        HeightMode.Animated(it),
                    )
                }

                override fun GuiGraphics.render(entity: Entity, partialTicks: Float) {
                    val texture = texture ?: return
                    val nativeImage = texture.pixels ?: return

                    val height = heightMode.activeMode.getHeight(entity, partialTicks)
                    val pos = entity.interpolateCurrentPosition(partialTicks).add(0.0, height, 0.0)
                    val screenPos = calculateScreenPos(pos) ?: return

                    val scaledWidth = nativeImage.width * scale
                    val scaledHeight = nativeImage.height * scale
                    drawTexQuad(
                        texture.textureSetup,
                        x0 = screenPos.x - scaledWidth * 0.5f,
                        y0 = screenPos.y - scaledHeight * 0.5f,
                        x1 = screenPos.x + scaledWidth * 0.5f,
                        y1 = screenPos.y + scaledHeight * 0.5f,
                        argb = color.argb,
                    )
                }
            }

            class Text(owner: ToggleableValueGroup, override val parent: ModeValueGroup<*>) : Gui("Text") {

                private val textScale by float("Scale", 1f, 0.01f..10f)
                private val textShadow by boolean("Shadow", true)
                private val color by color("Color", Color4b.RED)

                private val texts by textList("Text", mutableListOf("TARGET"))

                private val heightMode = modes(owner, "HeightMode") {
                    arrayOf(
                        HeightMode.Feet(it),
                        HeightMode.Top(it),
                        HeightMode.Relative(it),
                        HeightMode.Health(it),
                        HeightMode.Animated(it),
                    )
                }

                private val fontRenderer get() = FontManager.FONT_RENDERER

                override fun GuiGraphics.render(entity: Entity, partialTicks: Float) {
                    val height = heightMode.activeMode.getHeight(entity, partialTicks)
                    val pos = entity.interpolateCurrentPosition(partialTicks).add(0.0, height, 0.0)
                    val screenPos = calculateScreenPos(pos) ?: return

                    texts.forEachIndexed { i, text ->
                        fontRenderer.draw(text.asPlainText(Style.EMPTY + color)) {
                            horizontalAnchor = HorizontalAnchor.CENTER
                            verticalAnchor = VerticalAnchor.MIDDLE
                            x = screenPos.x
                            y = screenPos.y + i * fontRenderer.height
                            shadow = textShadow
                            scale = textScale
                        }
                    }
                }
            }

            class Arrow(override val parent: ModeValueGroup<*>) : Gui("Arrow") {

                private val color by color("Color", Color4b.RED)
                private val outlineColor by color("OutlineColor", Color4b.TRANSPARENT)
                private val size by float("Size", 1.5f, 0.5f..20f)

                override fun GuiGraphics.render(entity: Entity, partialTicks: Float) {
                    val pos = entity.interpolateCurrentPosition(partialTicks)
                        .add(0.0, entity.bbHeight.toDouble(), 0.0)

                    val screenPos = calculateScreenPos(pos) ?: return
                    val minX = screenPos.x - 5 * size
                    val midX = screenPos.x
                    val maxX = screenPos.x + 5 * size
                    val minY = screenPos.y - 10 * size
                    val maxY = screenPos.y
                    drawTriangle(
                        x0 = minX, y0 = minY,
                        x1 = midX, y1 = maxY,
                        x2 = maxX, y2 = minY,
                        color,
                        outlineColor,
                    )
                }
            }
        }
    }

}

private val defaultColor = Color4b.LIQUID_BOUNCE.alpha(100)

private val ghostModeTexture = LiquidBounce.resource("particles/glow.png")
    .toNativeImage().asTexture { "TargetRenderer Ghost" }

private sealed class HeightMode(name: String) : Mode(name) {
    abstract fun getHeight(entity: Entity, partialTicks: Float): Double

    interface WithGlow {
        fun getGlowHeight(entity: Entity, partialTicks: Float): Double
    }

    class Feet(override val parent: ModeValueGroup<*>) : HeightMode("Feet") {
        private val offset by float("Offset", 0f, -1f..1f)
        override fun getHeight(entity: Entity, partialTicks: Float): Double = offset.toDouble()
    }

    class Top(override val parent: ModeValueGroup<*>) : HeightMode("Top") {
        private val offset by float("Offset", 0f, -1f..1f)
        override fun getHeight(entity: Entity, partialTicks: Float) = entity.box.maxY - entity.box.minY + offset
    }

    // Lets the user chose the height relative to the entity's height
    // Use 1 for it to always be at the top of the entity
    // Use 0 for it to always be at the feet of the entity

    class Relative(override val parent: ModeValueGroup<*>) : HeightMode("Relative") {
        private val height by float("Height", 0.5f, -0.5f..1.5f)

        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            val box = entity.box
            val entityHeight = box.maxY - box.minY
            return height * entityHeight
        }
    }

    class Health(override val parent: ModeValueGroup<*>) : HeightMode("Health") {
        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            if (entity !is LivingEntity) return 0.0
            val box = entity.box
            val entityHeight = box.maxY - box.minY
            return entity.health / entity.maxHealth * entityHeight
        }
    }

    class Animated(override val parent: ModeValueGroup<*>) : HeightMode("Animated"), WithGlow {
        private val speed by float("Speed", 0.18f, 0.01f..1f)
        private val heightMultiplier by float("HeightMultiplier", 0.4f, 0.1f..1f)
        private val heightOffset by float("HeightOffset", 1.3f, 0f..2f)
        private val glowOffset by float("GlowOffset", -1f, -3.1f..3.1f)

        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            return calculateHeight((entity.tickCount + partialTicks) * speed)
        }

        override fun getGlowHeight(entity: Entity, partialTicks: Float): Double {
            return calculateHeight((entity.tickCount + partialTicks) * speed + glowOffset)
        }

        private fun calculateHeight(time: Float) =
            (sin(time) * heightMultiplier + heightOffset).toDouble()
    }
}
