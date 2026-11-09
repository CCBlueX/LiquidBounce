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

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP.getColor
import net.ccbluex.liquidbounce.render.drawGradientQuad
import net.ccbluex.liquidbounce.render.drawHorizontalLine
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.drawVerticalLine
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Rect
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.Mth
import net.minecraft.util.Util
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB

object Esp2DMode : EspMode.BoxBased("2D") {

    private object Outline : ToggleableValueGroup(this, "Outline", true) {
        val thickness by float("Thickness", 1f, 1f..9f, "px")
    }

    private object Corner : ToggleableValueGroup(this, "Corner", false) {
        val corner by float("Gap", 50f, 1f..100f, "%")

        /**
         * Keeps the sides whole, so that only the top and the bottom edge have the gap: `[ ]`.
         */
        val brackets by boolean("Brackets", false)
    }

    private object Border : ToggleableValueGroup(this, "Border", true) {
        val thickness by float("Thickness", 1f, 1f..9f, "px")
    }

    private object Glow : ToggleableValueGroup(this, "Glow", false) {
        val radius by int("Radius", 6, 1..16, "px")
        val opacity by float("Opacity", 0.6f, 0f..1f)
    }

    /**
     * Fades the box from the color of the entity at the top to [secondColor] at the bottom. Both ends swap their
     * colors and return to them [speed] times a second.
     */
    private object Gradient : ToggleableValueGroup(this, "Gradient", false) {
        val secondColor by color("SecondColor", Color4b.WHITE)
        val speed by float("Speed", 0.5f, 0f..4f, "Hz")
    }

    private val fill by boolean("Fill", true)
    private val fillOpacity by float("FillOpacity", 0.2f, 0f..1f)

    /**
     * Rounds the outline, its border, the fill and the glow. A rounded outline has no [Corner] gaps.
     */
    private val cornerRadius by float("CornerRadius", 0f, 0f..16f, "px")

    object HealthBar : ToggleableValueGroup(this, "HealthBar", true) {
        val spacing by float("Spacing", 2f, 0f..32f, "px")
        val position by enumChoice("Position", Position.LEFT)
        val highColor by color("HighColor", Color4b.GREEN)
        val lowColor by color("LowColor", Color4b.RED)

        enum class Position(override val tag: String) : Tagged {
            LEFT("Left"),
            RIGHT("Right"),
            TOP("Top"),
            BOTTOM("Bottom"),
        }
    }

    /**
     * Fades a box in when its target appears and out at the last place of the target once it is gone.
     */
    private object Fade : ToggleableValueGroup(this, "Fade", false) {
        val inTime by int("InTime", 250, 0..2000, "ms")
        val outTime by int("OutTime", 250, 0..2000, "ms")
    }

    init {
        tree(Outline)
        tree(Corner)
        tree(Border)
        tree(Glow)
        tree(Gradient)
        tree(HealthBar)
        tree(Fade)
    }

    private class FadingBox(val entity: Entity, var box: AABB) {
        var opacity = 0f
        var present = true
    }

    private val fadingBoxes = Reference2ObjectOpenHashMap<Entity, FadingBox>()
    private var lastFadeMillis = 0L

    private val thickness get() = Outline.thickness / mc.window.guiScale
    private val borderThickness get() = if (Border.enabled) Border.thickness / mc.window.guiScale else 0f
    private val radius get() = cornerRadius / mc.window.guiScale

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val boxes = collectPreparedBoxes(event.tickDelta)

        if (!Fade.enabled) {
            resetFade()
            for ((entity, _, _, box) in boxes) {
                val rect = WorldToScreen.calculateScreenRect(box) ?: continue
                event.context.drawBox(entity, rect, 1f)
            }
            return@handler
        }

        updateFade(boxes)
        for (fadingBox in fadingBoxes.values) {
            val rect = WorldToScreen.calculateScreenRect(fadingBox.box) ?: continue
            event.context.drawBox(fadingBox.entity, rect, fadingBox.opacity)
        }
    }

    override fun disable() {
        super.disable()
        resetFade()
    }

    private fun resetFade() {
        fadingBoxes.clear()
        lastFadeMillis = 0L
    }

    private fun updateFade(boxes: List<BoxBasedEspRenderState>) {
        val now = Util.getMillis()
        val elapsed = if (lastFadeMillis == 0L) 0f else (now - lastFadeMillis).toFloat()
        lastFadeMillis = now

        for (fadingBox in fadingBoxes.values) {
            fadingBox.present = false
        }
        for ((entity, _, _, box) in boxes) {
            val fadingBox = fadingBoxes.getOrPut(entity) { FadingBox(entity, box) }
            fadingBox.box = box
            fadingBox.present = true
        }

        val iterator = fadingBoxes.values.iterator()
        while (iterator.hasNext()) {
            val fadingBox = iterator.next()
            if (fadingBox.present) {
                fadingBox.opacity = (fadingBox.opacity + fadeStep(elapsed, Fade.inTime)).coerceAtMost(1f)
            } else {
                fadingBox.opacity -= fadeStep(elapsed, Fade.outTime)
                if (fadingBox.opacity <= 0f) {
                    iterator.remove()
                }
            }
        }
    }

    private fun fadeStep(elapsed: Float, time: Int) = if (time == 0) 1f else elapsed / time

    private fun GuiGraphicsExtractor.drawBox(entity: Entity, rect: Rect, opacity: Float) {
        val color = getColor(entity).with(a = 255)
        var topColor = color
        var bottomColor = color

        if (Gradient.enabled) {
            val secondColor = Gradient.secondColor.with(a = 255)
            val swap = 0.5f - 0.5f * Mth.cos(Util.getMillis() / 1000.0 * Gradient.speed * Mth.TWO_PI)
            topColor = color.interpolateTo(secondColor, swap)
            bottomColor = secondColor.interpolateTo(color, swap)
        }

        topColor = topColor.fade(opacity)
        bottomColor = bottomColor.fade(opacity)
        val borderColor = Color4b.BLACK.fade(opacity)

        pose().withPush {
            translate(rect.x1, rect.y1)

            if (Glow.enabled) {
                drawGlow(rect.w, rect.h, topColor, bottomColor)
            }
            if (fill) {
                drawFill(rect.w, rect.h, topColor.fade(fillOpacity), bottomColor.fade(fillOpacity))
            }
            if (Outline.enabled) {
                drawOutline(rect.w, rect.h, topColor, bottomColor, borderColor)
            }
            if (HealthBar.enabled && entity is LivingEntity) {
                drawHealthBar(entity, rect.w, rect.h, borderColor, opacity)
            }
        }
    }

    private fun GuiGraphicsExtractor.drawGlow(width: Float, height: Float, topColor: Color4b, bottomColor: Color4b) {
        val pixel = 1f / mc.window.guiScale
        val edge = thickness / 2 + borderThickness

        for (layer in 1..Glow.radius) {
            val layerOpacity = Glow.opacity * (1f - layer / (Glow.radius + 1f))
            val expand = edge + layer * pixel

            drawRoundedRect(
                -expand, -expand, width + expand, height + expand,
                radius = radius + expand,
                fillColor = null,
                outlineColor = topColor.fade(layerOpacity),
                outlineBottomColor = bottomColor.fade(layerOpacity),
                // Half a pixel of overlap, the antialiased edges of the rings leave seams otherwise.
                outlineWidth = pixel * 1.5f,
            )
        }
    }

    private fun GuiGraphicsExtractor.drawFill(width: Float, height: Float, topColor: Color4b, bottomColor: Color4b) {
        if (radius > 0f) {
            drawRoundedRect(0f, 0f, width, height, radius, fillColor = topColor, fillBottomColor = bottomColor)
        } else {
            drawGradientQuad(0f, 0f, width, height, topColor, bottomColor)
        }
    }

    private fun GuiGraphicsExtractor.drawOutline(
        width: Float,
        height: Float,
        topColor: Color4b,
        bottomColor: Color4b,
        borderColor: Color4b,
    ) {
        val t = thickness
        val b = borderThickness

        if (radius > 0f) {
            if (b > 0f) {
                drawRoundedRect(
                    -t / 2 - b, -t / 2 - b, width + t / 2 + b, height + t / 2 + b,
                    radius = radius + t / 2 + b,
                    fillColor = null,
                    outlineColor = borderColor,
                    outlineWidth = t + 2 * b,
                )
            }

            drawRoundedRect(
                -t / 2, -t / 2, width + t / 2, height + t / 2,
                radius = radius + t / 2,
                fillColor = null,
                outlineColor = topColor,
                outlineBottomColor = bottomColor,
                outlineWidth = t,
            )
            return
        }

        fun colorAt(y: Float) = if (topColor == bottomColor) {
            topColor
        } else {
            topColor.interpolateTo(bottomColor, (y + t / 2) / (height + t))
        }

        if (b > 0f) {
            forEachLine(width, height) { x1, y1, x2, y2 ->
                drawQuad(x1 - b, y1 - b, x2 + b, y2 + b, borderColor)
            }
        }

        forEachLine(width, height) { x1, y1, x2, y2 ->
            drawGradientQuad(x1, y1, x2, y2, colorAt(y1), colorAt(y2))
        }
    }

    /**
     * Calls [line] with the bounds of every line of the outline: its four edges, or what [Corner] leaves of them.
     */
    private inline fun forEachLine(
        width: Float,
        height: Float,
        line: (x1: Float, y1: Float, x2: Float, y2: Float) -> Unit,
    ) {
        val t = thickness
        val left = -t / 2
        val top = -t / 2
        val right = width + t / 2
        val bottom = height + t / 2

        if (!Corner.enabled) {
            line(left, top, right, top + t)
            line(left, bottom - t, right, bottom)
            line(left, top, left + t, bottom)
            line(right - t, top, right, bottom)
            return
        }

        val gap = Corner.corner / 100f
        val cornerWidth = width * (1f - gap) / 2f
        val cornerHeight = if (Corner.brackets) height / 2f else height * (1f - gap) / 2f

        line(left, top, cornerWidth, top + t)
        line(width - cornerWidth, top, right, top + t)
        line(left, bottom - t, cornerWidth, bottom)
        line(width - cornerWidth, bottom - t, right, bottom)

        line(left, top, left + t, cornerHeight)
        line(left, height - cornerHeight, left + t, bottom)
        line(right - t, top, right, cornerHeight)
        line(right - t, height - cornerHeight, right, bottom)
    }

    private fun GuiGraphicsExtractor.drawHealthBar(
        entity: LivingEntity,
        width: Float,
        height: Float,
        borderColor: Color4b,
        opacity: Float,
    ) {
        val maxHealth = entity.maxHealth.coerceAtLeast(1f) // prevent division by zero
        val healthPercentage = (entity.getActualHealth() / maxHealth).coerceIn(0f, 1f)
        val healthColor = HealthBar.lowColor.interpolateTo(HealthBar.highColor, healthPercentage).fade(opacity)

        val t = thickness
        val b = borderThickness
        val offset = HealthBar.spacing / mc.window.guiScale + t + if (Outline.enabled) 2 * b else 0f

        when (val position = HealthBar.position) {
            HealthBar.Position.LEFT, HealthBar.Position.RIGHT -> {
                val x = if (position == HealthBar.Position.LEFT) -t / 2 - offset else width - t / 2 + offset

                if (b > 0f) {
                    drawVerticalLine(x - b, -t / 2 - b, height + t / 2 + b, t + 2 * b, borderColor)
                }
                drawVerticalLine(x, height - height * healthPercentage - t / 2, height + t / 2, t, healthColor)
            }

            HealthBar.Position.TOP, HealthBar.Position.BOTTOM -> {
                val y = if (position == HealthBar.Position.TOP) -t / 2 - offset else height - t / 2 + offset

                if (b > 0f) {
                    drawHorizontalLine(-t / 2 - b, width + t / 2 + b, y - b, t + 2 * b, borderColor)
                }
                drawHorizontalLine(-t / 2, width * healthPercentage + t / 2, y, t, healthColor)
            }
        }
    }

}
