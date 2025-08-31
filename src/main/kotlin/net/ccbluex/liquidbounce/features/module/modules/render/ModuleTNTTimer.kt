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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.kotlin.forEachWithSelf
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.entity.TntEntity
import net.minecraft.util.math.MathHelper
import kotlin.math.sin

/**
 * TNTTimer module
 *
 * Highlight the active TNTs.
 */
object ModuleTNTTimer : ClientModule("TNTTimer", Category.RENDER) {

    override val baseKey: String
        get() = "liquidbounce.module.tntTimer"

    // Glow ESP
    val esp by boolean("ESP", true)

    private object ShowTimer : ToggleableConfigurable(this, "ShowTimer", false) {
        val scale by float("Scale", 1.5F, 0.25F..4F)
        val renderY by float("RenderY", 1.0F, -2.0F..2.0F)
        val border by boolean("Border", true)
        val timeUnit = choices(this, "TimeUnit", Ticks, arrayOf(Ticks, Seconds))

        sealed class TimeUnit(name: String) : Choice(name) {
            override val parent: ChoiceConfigurable<*>
                get() = timeUnit

            abstract operator fun invoke(t: Int): String
        }

        private object Ticks : TimeUnit("Ticks") {
            override fun invoke(t: Int) = t.toString()
        }

        private object Seconds : TimeUnit("Seconds") {
            override fun invoke(t: Int) = "%.2fs".format(t * 0.05F)
        }
    }

    init {
        tree(ShowTimer)
    }

    private val fontRenderer
        get() = FontManager.FONT_RENDERER

    private const val DEFAULT_FUSE = 80

    /**
     * Cycle light periodically according to the remaining time (`fuse`). The less time left, the faster the cycle.
     */
    fun getTntColor(fuse: Int): Color4b {
        val red = MathHelper.floor(255.0 * (1.0 + 0.5 * sin(2400.0 / (12 + fuse)))).coerceIn(0, 255)
        return Color4b(red, 0, 0)
    }

    @Suppress("unused")
    val render2DHandler = handler<OverlayRenderEvent> {
        if (!ShowTimer.enabled) return@handler

        renderEnvironmentForGUI {
            fontRenderer.withBuffers { buf ->
                val c = size
                val fontScale = 1.0F / (c * 0.15F) * ShowTimer.scale

                world.entities.filterIsInstanceTo(hashSetOf<TntEntity>()).forEachWithSelf { tnt, i, self ->
                    if (tnt.fuse <= 0) return@forEachWithSelf

                    val pos = tnt.box.center.add(0.0, ShowTimer.renderY.toDouble(), 0.0)

                    val screenPos = WorldToScreen.calculateScreenPos(pos) ?: return@forEachWithSelf

                    // Yellow #ffff00 -> Red #ff0000
                    val color = Color4b(255, MathHelper.floor(255F * tnt.fuse / DEFAULT_FUSE).coerceAtMost(255), 0)

                    // ticks to seconds
                    val text = process(
                        ShowTimer.timeUnit.activeChoice(tnt.fuse),
                        color,
                    )

                    val width = text.widthWithShadow

                    withMatrixStack {
                        // text
                        translate(screenPos.x, screenPos.y, 1000.0F * i / self.size)
                        scale(fontScale, fontScale, 1.0F)

                        draw(
                            text,
                            -0.5F * width,
                            -0.5F * height,
                            shadow = true,
                            z = 0.001F,
                        )

                        commit(buf)

                        // background
                        val q1 = Vec3(-0.6F * width, -0.55F * height, 0.0F)
                        val q2 = Vec3(0.6F * width, 0.55f * height, 0.0F)

                        withColor(Color4b(0, 0, 0, 120)) {
                            drawQuad(q1, q2)
                        }

                        // border
                        if (ShowTimer.border) {
                            withColor(color) {
                                drawQuadOutlines(q1, q2)
                            }
                        }
                    }
                }
            }
        }
    }
}
