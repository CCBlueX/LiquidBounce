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
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTeams
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.GenericDistanceHSBColorMode
import net.ccbluex.liquidbounce.render.GenericEntityHealthColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.drawTexQuad
import net.ccbluex.liquidbounce.render.drawTriangle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorRenderState
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorRenderer
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorStyle
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.cameraDistance
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.kotlin.optional
import net.ccbluex.liquidbounce.utils.math.floorToInt
import net.ccbluex.liquidbounce.utils.render.asTexture
import net.ccbluex.liquidbounce.utils.render.readNativeImage
import net.ccbluex.liquidbounce.utils.render.textureSetup
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.contents.ObjectContents
import net.minecraft.network.chat.contents.objects.PlayerSprite
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f

/**
 * Radar module
 *
 * Shows the direction of rendered entities on GUI.
 */
object ModuleRadar : ClientModule("Radar", ModuleCategories.RENDER, aliases = listOf("PointerESP")) {

    private val onlyPlayers by boolean("OnlyPlayers", false)
    private val hideTeammates by boolean("HideTeammates", false)
    private val showInView by boolean("ShowInView", true)
    private val edgeInset by float("EdgeInset", 0f, 0f..0.9f)

    private val pointerModes = choices("PointerMode", 0) {
        arrayOf(
            PointerMode.Triangle,
            PointerMode.ImageMode("Image1", LiquidBounce.resource("misc/triangle1.png").readNativeImage()),
            PointerMode.ImageMode("Image2", LiquidBounce.resource("misc/triangle2.png").readNativeImage()),
        )
    }

    /**
     * Arrows are drawn along their local +Y axis, base first, so that [length] is the distance from the base to the
     * tip [IndicatorRenderer] puts on the anchor.
     */
    private sealed class PointerMode(name: String) : Mode(name), IndicatorStyle {

        final override val parent: ModeValueGroup<*>
            get() = pointerModes

        object Triangle : PointerMode("Triangle") {
            private val width by float("Width", 8f, 1f..100f)
            private val height by float("Height", 10f, 1f..100f)
            private val tailConcaveSize by float("TailConcaveSize", 2f, 0f..50f).onChange {
                minOf(it, height)
            }

            override val length: Float
                get() = height

            override fun extract(ctx: GuiGraphicsExtractor, color: Color4b) {
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

            override val length: Float
                get() = size

            override fun extract(ctx: GuiGraphicsExtractor, color: Color4b) {
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

    private class RadarIndicatorState(
        override val pos: Vec3,
        override val priority: Int,
        override val indicatorStyle: IndicatorStyle,
        override val indicatorColor: Color4b,
        override val showInView: Boolean,
        override val edgeInset: Float,
        private val entity: Entity,
    ) : IndicatorRenderState {

        /**
         * `{head} {name}`, laid out on the side of the anchor the arrow is not on so it stays on screen and clear
         * of the arrow.
         */
        override fun extractTag(ctx: GuiGraphicsExtractor, outwardX: Float, outwardY: Float) {
            val font = mc.font
            val name = entity.displayName
            val head = (entity as? Player)?.let(::headOf)

            val headWidth = head?.let { font.width(it) + font.width(SPACE) } ?: 0
            val width = headWidth + font.width(name)

            val x = when {
                outwardX > AXIS_TOLERANCE -> -width
                outwardX < -AXIS_TOLERANCE -> 0
                else -> -width / 2
            }
            val y = when {
                outwardY > AXIS_TOLERANCE -> -font.lineHeight
                outwardY < -AXIS_TOLERANCE -> 0
                else -> -font.lineHeight / 2
            }

            if (head != null) {
                // White, otherwise the arrow's color is applied to the head as well.
                ctx.text(font, head, x, y, Color4b.WHITE.argb, TEXT_SHADOW)
            }

            ctx.text(font, name, x + headWidth, y, indicatorColor.argb, TEXT_SHADOW)
        }

        private companion object {
            const val SPACE = " "
            const val TEXT_SHADOW = true

            /**
             * Below this the arrow counts as pointing straight along the other axis, so the tag stays centered on it
             * rather than being pushed to one side.
             */
            const val AXIS_TOLERANCE = 0.1F
        }

    }

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
        super.onEnabled()
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
        super.onDisabled()
    }

    /**
     * The head renders through the font like any other text, but only a player's profile carries the skin.
     */
    private fun headOf(player: Player): Component = MutableComponent.create(
        ObjectContents(PlayerSprite(player.profile, false), optional())
    )

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> {
        val pointerMode = pointerModes.activeMode
        val colorMode = colorModes.activeMode

        for (entity in RenderedEntities) {
            if (entity === player || (onlyPlayers && entity !is Player)) continue
            if (hideTeammates && ModuleTeams.isInClientPlayersTeam(entity)) continue

            val pos = entity.interpolateCurrentPosition(it.tickDelta)
            val distance = pos.cameraDistance().toFloat()
            val alpha = (alpha.transform(distance) * 255).floorToInt()
            if (alpha == 0) continue

            IndicatorRenderer.submit(RadarIndicatorState(
                pos = pos,
                // Closer entities get a higher priority, so they end up covering the ones behind them.
                priority = -distance.toInt(),
                indicatorStyle = pointerMode,
                indicatorColor = colorMode.getColor(entity).alpha(alpha),
                showInView = showInView,
                edgeInset = edgeInset,
                entity = entity,
            ))
        }
    }

}
