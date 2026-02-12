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

package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.platform.NativeImage
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.CurveValue.Axis.Companion.axis
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.GenericDistanceHSBColorMode
import net.ccbluex.liquidbounce.render.GenericEntityHealthColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.drawTexQuad
import net.ccbluex.liquidbounce.render.drawTriangle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.client.floorToInt
import net.ccbluex.liquidbounce.utils.client.scaledDimension
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.cameraDistance
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.kotlin.unaryMinus
import net.ccbluex.liquidbounce.utils.render.asTexture
import net.ccbluex.liquidbounce.utils.render.textureSetup
import net.ccbluex.liquidbounce.utils.render.toNativeImage
import net.minecraft.client.CameraType
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import org.joml.Matrix3x2f
import org.joml.Vector2f
import kotlin.math.atan2

/**
 * Radar module
 *
 * Shows the direction of rendered entities on GUI.
 */
object ModuleRadar : ClientModule("Radar", ModuleCategories.RENDER, aliases = listOf("PointerESP")) {

    private val tiltModes = choices("Tilt", 0) {
        arrayOf(TiltMode.Static, TiltMode.ByPitch)
    }

    private sealed class TiltMode(name: String) : Mode(name) {
        final override val parent: ModeValueGroup<*>
            get() = tiltModes

        abstract fun transform(pose: Matrix3x2f, partialTick: Float)

        object Static : TiltMode("Static") {
            private val angle by float("Angle", 90f, -90f..90f, "deg")

            override fun transform(pose: Matrix3x2f, partialTick: Float) {
                pose.scale(1f, angle.fastSin())
            }
        }

        object ByPitch : TiltMode("ByPitch") {
            private val limitation by floatRange("Limitation", 45f..90f, 0f..90f, "deg").onChanged {
                limitationNeg = -it
            }
            private var limitationNeg: ClosedFloatingPointRange<Float> = -limitation

            override fun transform(pose: Matrix3x2f, partialTick: Float) {
                val pitchRad = run {
                    val pitch = player.getXRot(partialTick)
                    if (pitch >= 0) {
                        pitch.coerceIn(limitation)
                    } else {
                        pitch.coerceIn(limitationNeg)
                    }
                }.toRadians()

                pose.scale(1f, pitchRad.fastSin())
            }
        }
    }

    private val radius by float("Radius", 40f, 2f..200f)

    private val onlyPlayers by boolean("OnlyPlayers", false)

    private val pointerModes = choices("PointerMode", 0) {
        arrayOf(
            PointerMode.Triangle,
            PointerMode.ImageMode("Image1", LiquidBounce.resource("misc/triangle1.png").toNativeImage()),
            PointerMode.ImageMode("Image2", LiquidBounce.resource("misc/triangle2.png").toNativeImage()),
        )
    }

    private sealed class PointerMode(name: String) : Mode(name) {
        final override val parent: ModeValueGroup<*>
            get() = pointerModes

        context(ctx: GuiGraphics)
        abstract fun draw(color: Color4b)

        object Triangle : PointerMode("Triangle") {
            private val width by float("Width", 8f, 1f..100f)
            private val height by float("Height", 10f, 1f..100f)
            private val tailConcaveSize by float("TailConcaveSize", 2f, 0f..50f).onChange {
                minOf(it, height)
            }

            context(ctx: GuiGraphics)
            override fun draw(color: Color4b) {
                if (Mth.equal(tailConcaveSize, 0f)) {
                    ctx.drawTriangle(
                        x0 = -width * 0.5f, y0 = 0f,
                        x1 = 0f, y1 = height,
                        x2 = width * 0.5f, y2 = 0f,
                        fillColor = color,
                        cull = false,
                    )
                } else {
                    ctx.drawTriangle(
                        x0 = -width * 0.5f, y0 = 0f,
                        x1 = 0f, y1 = height,
                        x2 = 0f, y2 = tailConcaveSize,
                        fillColor = color,
                        cull = false,
                    ) // left
                    ctx.drawTriangle(
                        x0 = 0f, y0 = tailConcaveSize,
                        x1 = 0f, y1 = height,
                        x2 = width * 0.5f, y2 = 0f,
                        fillColor = color,
                        cull = false,
                    ) // right
                }
            }
        }

        class ImageMode(name: String, val texture: AbstractTexture) : PointerMode(name) {
            constructor(name: String, nativeImage: NativeImage) : this(name, nativeImage.asTexture { "Radar $name" })

            private val size by float("Size", 10f, 1f..100f)

            context(ctx: GuiGraphics)
            override fun draw(color: Color4b) {
                ctx.drawTexQuad(
                    texture.textureSetup,
                    -size / 2f, 0f, size / 2f, size,
                    u1 = 1f, v1 = 1f, u2 = 0f, v2 = 0f,
                    argb = color.argb,
                    pipeline = ClientRenderPipelines.GUI.TexQuadNoCull,
                )
            }
        }
    }

    private val colorModes = choices("ColorMode", 0) {
        arrayOf(
            GenericDistanceHSBColorMode.entity(it, fixedAlpha = 1F),
            GenericEntityHealthColorMode(it),
            GenericStaticColorMode(it, Color4b.WHITE.with(a = 100)),
            GenericRainbowColorMode(it)
        )
    }

    private val alpha = curve(
        "Alpha",
        mutableListOf(Vector2f(0f, 1f), Vector2f(200f, 1f)),
        xAxis = "Distance" axis 0f..200f,
        yAxis = "Alpha" axis 0f..1f,
    )

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
        super.onEnabled()
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
        super.onDisabled()
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> {
        with(it.context) {
            pose().withPush {
                val (width, height) = mc.window.scaledDimension
                translate(width * 0.5f, height * 0.5f)

                val yawRad = player.getYRot(it.tickDelta).toRadians()
                val playerPos = player.interpolateCurrentPosition(it.tickDelta)

                tiltModes.activeMode.transform(this, it.tickDelta)

                if (mc.options.cameraType == CameraType.THIRD_PERSON_FRONT) {
                    scale(-1f, 1f)
                }

                rotate(-yawRad)

                for (entity in RenderedEntities) {
                    if (entity === player || (onlyPlayers && entity !is Player)) continue
                    val entityPos = entity.interpolateCurrentPosition(it.tickDelta)

                    val cameraDistance = entityPos.cameraDistance().toFloat()
                    val alpha = (alpha.transform(cameraDistance) * 255).floorToInt()
                    if (alpha == 0) continue

                    val color = colorModes.activeMode.getColor(entity).alpha(alpha)

                    val diffX = entityPos.x - playerPos.x
                    val diffZ = entityPos.z - playerPos.z

                    withPush {
                        rotate(atan2(diffZ, diffX).toFloat() + Mth.HALF_PI)
                        translate(0f, radius)
                        with(this@with) {
                            pointerModes.activeMode.draw(color = color)
                        }
                    }
                }
            }
        }
    }

}
