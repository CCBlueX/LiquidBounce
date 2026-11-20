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
package net.ccbluex.liquidbounce.render.splash

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.HorizontalAnchor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.util.Util
import kotlin.math.abs

/**
 * A screen that continues the splash in [phase], with the client's own text at the splash's `content` layer.
 */
abstract class SplashPhaseScreen(title: Component, private val phase: SplashTimeline.Phase) : Screen(title) {

    private var shownAt = -1L

    /**
     * Time since the splash has played into this screen's phase, in ms; the client's text fades in from there.
     */
    protected val age: Float
        get() = if (shownAt == -1L) 0f else (Util.getMillis() - shownAt).toFloat()

    /** False until the client's text starts to fade in; input waits for it too. */
    protected val isRevealed get() = shownAt != -1L

    /** Top of the client's text, in GUI pixels. */
    protected var contentTop = 0f
        private set

    private var contentCenter = 0f

    private var options = emptyList<String>()
    private var optionsTop = Float.NaN
    private var optionsHalfWidth = 0f
    private var lastMouseY = -1

    /** The option the arrow keys or the mouse point at. */
    protected var focusedOption = 0
        private set

    override fun init() {
        Splash.request(phase)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(0, 0, width, height, Splash.background)
    }

    /**
     * @param progress the loading progress, or null to keep the last one
     */
    protected fun extractSplash(graphics: GuiGraphicsExtractor, progress: Float?) {
        Splash.render(graphics, progress, 1f)
        if (shownAt == -1L && Splash.isSettled(phase)) {
            shownAt = Util.getMillis()
        }
        contentTop = Splash.contentY.takeUnless(Float::isNaN) ?: (height * DEFAULT_CONTENT_TOP)
        contentCenter = Splash.contentX.takeUnless(Float::isNaN) ?: (width / 2f)
    }

    /**
     * Draws [text] centered on the content anchor at [y] in LiquidBounce's font.
     */
    protected fun extractCenteredText(
        graphics: GuiGraphicsExtractor,
        text: String,
        y: Float,
        alpha: Float,
        scale: Float = 1f,
        color: Int = Splash.textColor,
    ) {
        if (alpha <= 0.02f) {
            return
        }

        if (!FontManager.isLoaded) {
            // The client's start failed before its font was ready
            graphics.pose().withPush {
                translate(contentCenter, y)
                scale(scale)
                graphics.centeredText(font, text, 0, 0, ARGB.multiplyAlpha(color, alpha))
            }
            return
        }

        val font = FontManager.FONT_RENDERER
        font.draw(
            graphics,
            text.asPlainText(),
            contentCenter,
            y,
            Color4b(color).fade(alpha),
            scale = font.scaleToVanillaFont * scale,
            horizontalAnchor = HorizontalAnchor.CENTER,
        )
    }

    /**
     * Draws [labels] one per line from [top], the focused one in the accent color; the mouse focuses the one it
     * moves onto. A click on one, or Enter on the focused one, calls [pickOption]. Pass the same list again while
     * the labels stay the same.
     */
    @Suppress("LongParameterList")
    protected fun extractOptions(
        graphics: GuiGraphicsExtractor,
        labels: List<String>,
        top: Float,
        alpha: Float,
        mouseX: Int,
        mouseY: Int,
    ) {
        if (labels !== options) {
            options = labels
            optionsHalfWidth = labels.maxOfOrNull(::textWidth)?.div(2f) ?: 0f
            focusedOption = focusedOption.coerceIn(0, (labels.size - 1).coerceAtLeast(0))
        }
        optionsTop = top
        if (mouseY != lastMouseY) {
            lastMouseY = mouseY
            optionAt(mouseX.toDouble(), mouseY.toDouble())?.let { focusedOption = it }
        }

        for (index in labels.indices) {
            val color = if (index == focusedOption) Splash.accentColor else Splash.textColor
            extractCenteredText(graphics, labels[index], top + index * OPTION_HEIGHT, alpha, color = color)
        }
    }

    protected open fun pickOption(index: Int) {}

    private fun optionAt(x: Double, y: Double): Int? {
        if (!isRevealed || optionsTop.isNaN() || y < optionsTop || abs(x - contentCenter) > optionsHalfWidth) {
            return null
        }
        return ((y - optionsTop) / OPTION_HEIGHT).toInt().takeIf { it in options.indices }
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        val index = optionAt(click.x(), click.y()) ?: return super.mouseClicked(click, doubled)
        pickOption(index)
        return true
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        if (!isRevealed || options.isEmpty()) {
            return super.keyPressed(input)
        }

        when (input.key()) {
            InputConstants.KEY_UP -> focusedOption = (focusedOption - 1).mod(options.size)
            InputConstants.KEY_DOWN -> focusedOption = (focusedOption + 1).mod(options.size)
            InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> pickOption(focusedOption)
            else -> return super.keyPressed(input)
        }
        return true
    }

    override fun shouldCloseOnEsc() = false

    override fun isPauseScreen() = false

    private companion object {
        const val DEFAULT_CONTENT_TOP = 0.7f
        const val OPTION_HEIGHT = 14f
    }

}

/** Width of [text] in GUI pixels at [scale] times the vanilla size; Minecraft's font until the client's loads. */
internal fun textWidth(text: String, scale: Float = 1f): Float {
    if (!FontManager.isLoaded) {
        return mc.font.width(text) * scale
    }
    val font = FontManager.FONT_RENDERER
    return font.getStringWidth(text.asPlainText(), font.scaleToVanillaFont * scale)
}

/**
 * Breaks [text] into lines no wider than [maxWidth] GUI pixels in LiquidBounce's font at [scale].
 */
internal fun wrapText(text: String, maxWidth: Float, scale: Float = 1f): List<String> =
    text.lines().flatMap { paragraph ->
        val lines = mutableListOf<String>()
        var line = ""
        for (word in paragraph.split(' ')) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (line.isNotEmpty() && textWidth(candidate, scale) > maxWidth) {
                lines += line
                line = word
            } else {
                line = candidate
            }
        }
        lines += line
        lines
    }
