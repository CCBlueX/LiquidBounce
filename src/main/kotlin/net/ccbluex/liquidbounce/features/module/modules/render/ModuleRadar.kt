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
import net.ccbluex.fastutil.enumSetAllOf
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.CurveValue.Axis.Companion.axis
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTeams
import net.ccbluex.liquidbounce.render.GenericDistanceHSBColorMode
import net.ccbluex.liquidbounce.render.GenericEntityHealthColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorRenderState
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorRenderer
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorStyle
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorVisibility
import net.ccbluex.liquidbounce.render.gui.indicator.TextureIndicatorStyle
import net.ccbluex.liquidbounce.render.gui.indicator.TriangleIndicatorStyle
import net.ccbluex.liquidbounce.utils.text.asPlayerHead
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.cameraDistance
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.simpleDisplayName
import net.ccbluex.liquidbounce.utils.math.floorToInt
import net.ccbluex.liquidbounce.utils.render.asTexture
import net.ccbluex.liquidbounce.utils.render.readNativeImage
import net.ccbluex.liquidbounce.utils.render.textureSetup
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.texture.AbstractTexture
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
    private val show by multiEnumChoice("Show", enumSetAllOf<IndicatorVisibility>(), canBeNone = false)
    private val edgeInset by float("EdgeInset", 0f, 0f..0.9f)

    private val pointerModes = choices("PointerMode", 0) {
        arrayOf(
            PointerMode.Triangle,
            PointerMode.ImageMode("Image1", LiquidBounce.resource("misc/triangle1.png").readNativeImage()),
            PointerMode.ImageMode("Image2", LiquidBounce.resource("misc/triangle2.png").readNativeImage()),
        )
    }

    /**
     * The arrow the active pointer mode draws, built once per frame so the reusable styles stay free of
     * configuration.
     */
    private sealed class PointerMode(name: String) : Mode(name) {

        final override val parent: ModeValueGroup<*>
            get() = pointerModes

        abstract fun style(): IndicatorStyle

        object Triangle : PointerMode("Triangle") {
            private val width by float("Width", 8f, 1f..100f)
            private val height by float("Height", 10f, 1f..100f)
            private val tailConcaveSize by float("TailConcaveSize", 2f, 0f..50f).onChange {
                minOf(it, height)
            }

            override fun style() = TriangleIndicatorStyle(width, height, tailConcaveSize)
        }

        class ImageMode(name: String, val texture: AbstractTexture) : PointerMode(name) {
            constructor(name: String, nativeImage: NativeImage) : this(name, nativeImage.asTexture { "Radar $name" })

            private val size by float("Size", 10f, 1f..100f)

            /**
             * Built once: the [textureSetup] property allocates a setup per access, and a native image texture is
             * ready as soon as it is created.
             */
            private val textureSetup: TextureSetup = texture.textureSetup

            override fun style() = TextureIndicatorStyle(
                textureSetup = textureSetup,
                size = size,
                // The images are stored tip up.
                u1 = 1f, v1 = 1f, u2 = 0f, v2 = 0f,
            )
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
        override val visibleAt: Collection<IndicatorVisibility>,
        override val edgeInset: Float,
        private val entity: Entity,
    ) : IndicatorRenderState {

        /**
         * `{head} {name}` on a single line, centred on the origin.
         */
        override fun measureTag(): BoundingBox2f {
            val font = mc.font
            val head = (entity as? Player)?.profile?.asPlayerHead()
            val width = (head?.let { font.width(it) + font.width(SPACE) } ?: 0) + font.width(entity.simpleDisplayName)
            val halfHeight = font.lineHeight * 0.5F

            return BoundingBox2f(-width * 0.5F, -halfHeight, width * 0.5F, halfHeight)
        }

        override fun extractTag(ctx: GuiGraphicsExtractor) {
            val font = mc.font
            val name = entity.simpleDisplayName
            val head = (entity as? Player)?.profile?.asPlayerHead()
            val headWidth = head?.let { font.width(it) + font.width(SPACE) } ?: 0

            val x = -(headWidth + font.width(name)) * 0.5F
            val y = -font.lineHeight * 0.5F

            if (head != null) {
                // White, otherwise the arrow's color is applied to the head as well.
                ctx.text(font, head, x.toInt(), y.toInt(), Color4b.WHITE.argb, TEXT_SHADOW)
            }

            ctx.text(font, name, (x + headWidth).toInt(), y.toInt(), indicatorColor.argb, TEXT_SHADOW)
        }

        private companion object {
            const val SPACE = " "
            const val TEXT_SHADOW = true
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

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> {
        val style = pointerModes.activeMode.style()
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
                indicatorStyle = style,
                indicatorColor = colorMode.getColor(entity).alpha(alpha),
                visibleAt = show,
                edgeInset = edgeInset,
                entity = entity,
            ))
        }
    }

}
