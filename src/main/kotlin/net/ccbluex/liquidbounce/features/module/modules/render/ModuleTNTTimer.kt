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

import net.ccbluex.fastutil.enumSetAllOf
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.CurveValue.Axis.Companion.axis
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput
import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorRenderState
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorRenderer
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorStyle
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorVisibility
import net.ccbluex.liquidbounce.render.gui.indicator.NoneIndicatorStyle
import net.ccbluex.liquidbounce.render.gui.indicator.TriangleIndicatorStyle
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.entity.cameraDistance
import net.ccbluex.liquidbounce.utils.text.withFormat
import net.ccbluex.liquidbounce.utils.world.EntityLookup.Companion.EntityLookup
import net.ccbluex.liquidbounce.utils.world.filterTo
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import kotlin.math.sin

/**
 * TNTTimer module
 *
 * Highlight the active TNTs.
 */
object ModuleTNTTimer : ClientModule("TNTTimer", ModuleCategories.RENDER) {

    override val baseKey: String
        get() = "${ConfigSystem.KEY_PREFIX}.module.tntTimer"

    // Glow ESP
    val esp by boolean("ESP", true)

    private val OFFSCREEN_ARROW = TriangleIndicatorStyle(width = 8.0F, height = 10.0F)

    /**
     * Countdown of every lit TNT, drawn as an indicator so that the ones off screen still show up.
     */
    private object ShowTimer : ToggleableValueGroup(this, "ShowTimer", false) {
        private val scale = curve(
            "Scale",
            mutableListOf(Vector2f(0f, 1f), Vector2f(200f, 1f)),
            xAxis = "Distance" axis 0f..200f,
            yAxis = "Scale" axis 0.25f..4f,
        )
        private val renderY by float("RenderY", 1.0F, -2.0F..2.0F)
        private val ownerName by boolean("OwnerName", true)
        private val gameTimeUnit by enumChoice("TimeUnit", GameTimeUnit.TICKS)
        private val show by multiEnumChoice("Show", enumSetAllOf<IndicatorVisibility>(), canBeNone = false)

        private const val TEXT_SHADOW = true

        private val tntEntities by EntityLookup { set ->
            filterTo(set, EntityTypes.TNT) { it.fuse > 0 }
        }

        private class TntTimerIndicatorState(
            override val pos: Vec3,
            override val priority: Int,
            override val indicatorColor: Color4b,
            override val visibleAt: Collection<IndicatorVisibility>,
            private val scale: Float,
            private val text: FormattedCharSequence,
        ) : IndicatorRenderState {

            /**
             * An arrow only helps for a TNT that cannot be seen; on screen the label sits on the TNT itself.
             */
            override val indicatorStyle: IndicatorStyle = OFFSCREEN_ARROW

            override val inViewIndicatorStyle: IndicatorStyle = NoneIndicatorStyle

            override fun measureTag(): BoundingBox2f {
                val font = mc.font
                val width = font.width(text) * scale
                val height = font.lineHeight * scale

                return BoundingBox2f(-width * 0.5F, -height * 0.5F, width * 0.5F, height * 0.5F)
            }

            override fun extractTag(ctx: GuiGraphicsExtractor) {
                val font = mc.font

                ctx.pose().withPush {
                    scale(scale)
                    ctx.text(
                        font,
                        text,
                        -font.width(text) / 2,
                        -font.lineHeight / 2,
                        indicatorColor.argb,
                        TEXT_SHADOW,
                    )
                }
            }

        }

        override fun onDisabled() {
            tntEntities.clear()
            super.onDisabled()
        }

        @Suppress("unused")
        private val renderHandler = handler<OverlayRenderEvent> {
            for (tnt in tntEntities) {
                val pos = tnt.boundingBox.center.add(0.0, renderY.toDouble(), 0.0)

                // Yellow #ffff00 -> Red #ff0000
                val color = Color4b(255, Mth.floor(255F * tnt.fuse / DEFAULT_FUSE).coerceAtMost(255), 0)

                var text = gameTimeUnit.format(tnt.fuse).withFormat()

                if (ownerName) {
                    tnt.owner?.name?.let {
                        text = FormattedCharSequence.composite(
                            text,
                            " (".withFormat(),
                            it.sanitizeForeignInput(),
                            ")".withFormat(),
                        )
                    }
                }

                IndicatorRenderer.submit(TntTimerIndicatorState(
                    pos = pos,
                    // The TNT closest to blowing up is the one worth covering last.
                    priority = -tnt.fuse,
                    indicatorColor = color,
                    visibleAt = show,
                    scale = scale.transform(pos.cameraDistance().toFloat()),
                    text = text,
                ))
            }
        }
    }

    init {
        tree(ShowTimer)
    }

    private const val DEFAULT_FUSE = 80

    /**
     * Cycle light periodically according to the remaining time (`fuse`). The less time left, the faster the cycle.
     */
    fun getTntColor(fuse: Int): Color4b {
        val red = Mth.floor(255.0 * (1.0 + 0.5 * sin(2400.0 / (12 + fuse)))).coerceIn(0, 255)
        return Color4b(red, 0, 0)
    }

}
