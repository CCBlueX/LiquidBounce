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
package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.common.ClientLogoTexture
import net.ccbluex.liquidbounce.common.ClientLogoTexture.HEIGHT
import net.ccbluex.liquidbounce.common.ClientLogoTexture.WIDTH
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.ARGB
import org.joml.Matrix3x2fStack

/**
 * Loading overlay intro: the mark settles in, the wordmark wipes in from behind it, the BOUNCE
 * badge slides into place and the progress bar opens underneath.
 *
 * Everything is laid out in the logo texture's pixel space, so it scales with the window.
 */
object SplashScreenRenderer {

    /**
     * Length of the intro in ms; the overlay's playhead runs up to this.
     */
    const val LENGTH = 1700f

    private val MARK = Region(0, 0, 588, 588)
    private val TEXT = Region(680, 132, 1240, 308)
    private val BADGE = Region(1095, 486, 820, 235)

    private const val MARK_CENTER = 294f

    private const val BAR_Y = HEIGHT + 150f
    private const val BAR_HEIGHT = 10f

    /**
     * Part of the fade-out the logo takes to leave.
     */
    private const val OUTRO_EXIT = 0.45f

    /**
     * @param outro 0 until the overlay fades into the menu, then up to 1 at the end of the fade
     */
    fun render(graphics: GuiGraphicsExtractor, time: Float, progress: Float, alpha: Float, outro: Float) {
        val exit = Easing.QUAD_IN.at(outro / OUTRO_EXIT)
        val visible = alpha * (1f - exit)
        if (visible <= 0f) {
            return
        }

        withLogoSpace(graphics) {
            // Steps back as the menu comes through
            val scale = 1f - 0.04f * exit
            translate(WIDTH / 2f, HEIGHT / 2f)
            scale(scale)
            translate(-WIDTH / 2f, -HEIGHT / 2f)

            mark(graphics, time, visible)
            wordmark(graphics, time, visible)
            badge(graphics, time, visible)
            progressBar(graphics, time, progress, visible)
        }
    }

    private fun mark(graphics: GuiGraphicsExtractor, time: Float, alpha: Float) {
        val fade = Easing.QUAD_OUT.at(time / 450f)
        val settle = Easing.EXPONENTIAL_OUT.at(time / 900f)
        val scale = 0.86f + 0.14f * settle

        graphics.pose().withPush {
            translateUnfold(time)
            translate(MARK_CENTER, MARK_CENTER + 40f * (1f - settle))
            scale(scale)
            translate(-MARK_CENTER, -MARK_CENTER)
            graphics.blit(MARK, MARK.x, MARK.width, alpha * fade)
        }
    }

    /**
     * Comes out from behind the mark while the mark moves over to make room.
     */
    private fun wordmark(graphics: GuiGraphicsExtractor, time: Float, alpha: Float) {
        val reveal = unfold(time)
        val shown = (TEXT.width * reveal).toInt()
        if (shown <= 0) {
            return
        }

        graphics.pose().withPush {
            translateUnfold(time)
            graphics.blit(TEXT, TEXT.x, shown, alpha * Easing.QUAD_OUT.at(reveal * 3f))
        }
    }

    private fun unfold(time: Float) = Easing.EXPONENTIAL_OUT.at((time - 600f) / 700f)

    /**
     * Keeps the mark centered on its own until the wordmark unfolds.
     */
    private fun Matrix3x2fStack.translateUnfold(time: Float) {
        val left = 1f - unfold(time)
        translate((WIDTH / 2f - MARK_CENTER) * left, (HEIGHT / 2f - MARK_CENTER) * left)
    }

    private fun badge(graphics: GuiGraphicsExtractor, time: Float, alpha: Float) {
        val t = Easing.EXPONENTIAL_OUT.at((time - 900f) / 500f)
        if (t <= 0f) {
            return
        }

        graphics.pose().withPush {
            translate(-80f * (1f - t), 0f)
            graphics.blit(BADGE, BADGE.x, BADGE.width, alpha * t)
        }
    }

    private fun progressBar(graphics: GuiGraphicsExtractor, time: Float, progress: Float, alpha: Float) {
        val show = Easing.EXPONENTIAL_OUT.at((time - 1100f) / 600f)
        if (show <= 0f) {
            return
        }

        // Opens out from the middle
        val half = WIDTH / 2f * show
        val left = WIDTH / 2f - half
        val right = WIDTH / 2f + half
        val radius = BAR_HEIGHT / 2f
        graphics.drawRoundedRect(
            left, BAR_Y, right, BAR_Y + BAR_HEIGHT, radius,
            fillColor = Color4b(ARGB.white(alpha * 0.14f))
        )

        val filled = left + (right - left) * progress.coerceIn(0f, 1f)
        if (filled - left >= BAR_HEIGHT) {
            graphics.drawRoundedRect(
                left, BAR_Y, filled, BAR_Y + BAR_HEIGHT, radius,
                fillColor = Color4b.LIQUID_BOUNCE.fade(alpha)
            )
        }
    }

    private fun Easing.at(x: Float) = transform(x.coerceIn(0f, 1f))

    private inline fun withLogoSpace(graphics: GuiGraphicsExtractor, block: Matrix3x2fStack.() -> Unit) {
        val screenWidth = graphics.guiWidth().toFloat()
        val screenHeight = graphics.guiHeight().toFloat()
        val scale = minOf(screenWidth * 0.4f / WIDTH, screenHeight * 0.25f / HEIGHT)

        graphics.pose().withPush {
            translate((screenWidth - WIDTH * scale) / 2f, (screenHeight - HEIGHT * scale) / 2f)
            scale(scale)
            block()
        }
    }

    private fun GuiGraphicsExtractor.blit(region: Region, u: Int, width: Int, alpha: Float) {
        if (alpha <= 0f) {
            return
        }

        blit(
            ClientRenderPipelines.GUI.SmoothTexture,
            ClientLogoTexture.CLIENT_LOGO,
            u,
            region.y,
            u.toFloat(),
            region.y.toFloat(),
            width,
            region.height,
            width,
            region.height,
            WIDTH,
            HEIGHT,
            ARGB.white(alpha)
        )
    }

    private class Region(val x: Int, val y: Int, val width: Int, val height: Int)

}
