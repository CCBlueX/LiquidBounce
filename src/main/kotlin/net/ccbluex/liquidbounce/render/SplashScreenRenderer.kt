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
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.util.ARGB
import org.joml.Matrix3x2fStack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp

/**
 * Loading overlay intro: the logo mark falls in as a drop, splashes and bounces into place,
 * then LIQUID slides out from behind it and the BOUNCE badge drops in.
 *
 * Everything is laid out in the logo texture's pixel space, so it scales with the window.
 */
object SplashScreenRenderer {

    /**
     * Length of the intro in ms; the overlay's playhead runs up to this.
     */
    const val LENGTH = 2000f

    private val MARK = Region(0, 0, 588, 588)
    private val TEXT = Region(680, 132, 1240, 308)
    private val BADGE = Region(1095, 486, 820, 235)

    private const val MARK_CENTER = 294f
    private const val FLOOR = 588f

    private const val IMPACT = 460f
    private const val LANDING = 820f
    private const val FALL_HEIGHT = 2200f
    private const val BOUNCE_HEIGHT = 230f
    private const val DROP_SCALE = 0.55f

    private const val BAR_Y = HEIGHT + 150f
    private const val BAR_HEIGHT = 22f

    /**
     * Splash droplets thrown at the first impact: horizontal and vertical speed in units per ms.
     */
    private val DROPLETS = floatArrayOf(
        -1.10f, -1.45f,
        -0.70f, -1.95f,
        -0.30f, -2.30f,
        0.35f, -2.20f,
        0.75f, -1.80f,
        1.15f, -1.35f,
    )
    private const val GRAVITY = 0.0062f
    private const val DROPLET_LIFE = 620f

    /**
     * Enough for the outro wave to leave a 21:9 window from the logo's center.
     */
    private const val OUTRO_REACH = 7000f

    /**
     * Part of the fade-out the logo takes to leave.
     */
    private const val OUTRO_EXIT = 0.35f

    /**
     * @param outro 0 until the overlay fades into the menu, then up to 1 at the end of the fade
     */
    fun render(graphics: GuiGraphicsExtractor, time: Float, progress: Float, alpha: Float, outro: Float) {
        if (alpha <= 0f) {
            return
        }

        withLogoSpace(graphics) {
            // A last wave washes out over the menu while the background fades
            if (outro > 0f) {
                val radius = 300f + OUTRO_REACH * Easing.QUAD_OUT.transform(outro)
                val width = 340f * (1f - outro) + 40f
                val color = ARGB.color(0.6f * (1f - outro), Color4b.LIQUID_BOUNCE.argb)
                graphics.drawCircle(WIDTH / 2f, HEIGHT / 2f, radius, radius - width) { color }
            }

            // The logo rushes towards the viewer and is gone before the menu shows through
            val exit = (outro / OUTRO_EXIT).coerceAtMost(1f)
            val visible = alpha * (1f - exit)
            if (visible <= 0f) {
                return@withLogoSpace
            }

            val zoom = 1f + 0.6f * Easing.QUAD_IN.transform(exit)
            translate(WIDTH / 2f, HEIGHT / 2f)
            scale(zoom)
            translate(-WIDTH / 2f, -HEIGHT / 2f)

            ripple(graphics, time - IMPACT, 1f, visible)
            ripple(graphics, time - IMPACT - 140f, 0.6f, visible)
            ripple(graphics, time - LANDING, 0.45f, visible)
            droplets(graphics, time - IMPACT, visible)

            text(graphics, time, visible)
            badge(graphics, time, visible)
            mark(graphics, time, visible)
            progressBar(graphics, time, progress, visible)
        }
    }

    private fun mark(graphics: GuiGraphicsExtractor, time: Float, alpha: Float) {
        val lift: Float
        var stretch = 0f
        if (time < IMPACT) {
            val fall = time / IMPACT
            lift = FALL_HEIGHT * (1f - fall * fall)
            stretch = 0.22f * fall
        } else if (time < LANDING) {
            val hop = (time - IMPACT) / (LANDING - IMPACT)
            lift = BOUNCE_HEIGHT * 4f * hop * (1f - hop)
        } else {
            lift = 0f
        }

        val grow = Easing.BACK_OUT.at((time - IMPACT) / (LANDING - IMPACT))
        val size = DROP_SCALE + (1f - DROP_SCALE) * grow
        // Point up while falling like a drop, then turn upright
        val angle = (PI / 4).toFloat() * (1f - grow)
        val squash = squash(time - IMPACT, 0.38f) + squash(time - LANDING, 0.16f) - stretch

        val pose = graphics.pose()
        pose.withPush {
            translate(MARK_CENTER, FLOOR - lift)
            scale(1f + squash, 1f - squash)
            translate(0f, -MARK_CENTER * size)
            rotate(angle)
            scale(size)
            translate(-MARK_CENTER, -MARK_CENTER)
            graphics.blit(MARK, MARK.x, MARK.width, alpha * (time / 120f).coerceIn(0f, 1f))
        }
    }

    private fun text(graphics: GuiGraphicsExtractor, time: Float, alpha: Float) {
        val slide = Easing.EXPONENTIAL_OUT.at((time - 760f) / 700f)
        val hidden = (TEXT.width * (1f - slide)).toInt()
        if (hidden < TEXT.width) {
            graphics.blit(TEXT, TEXT.x + hidden, TEXT.width - hidden, alpha * slide, drawX = TEXT.x)
        }
    }

    private fun badge(graphics: GuiGraphicsExtractor, time: Float, alpha: Float) {
        val t = ((time - 1080f) / 720f).coerceIn(0f, 1f)
        if (t <= 0f) {
            return
        }

        // Springs up from below into place
        graphics.pose().withPush {
            translate(0f, 220f * (1f - Easing.BOUNCE_OUT.transform(t)))
            graphics.blit(BADGE, BADGE.x, BADGE.width, alpha * (t * 5f).coerceAtMost(1f))
        }
    }

    private fun progressBar(graphics: GuiGraphicsExtractor, time: Float, progress: Float, alpha: Float) {
        val show = Easing.EXPONENTIAL_OUT.at((time - 1350f) / 600f)
        if (show <= 0f) {
            return
        }

        // Grows out from the middle
        val half = WIDTH / 2f * show
        val left = WIDTH / 2f - half
        val right = WIDTH / 2f + half
        val radius = BAR_HEIGHT / 2f
        graphics.drawRoundedRect(
            left, BAR_Y, right, BAR_Y + BAR_HEIGHT, radius,
            fillColor = Color4b(ARGB.white(alpha * 0.18f))
        )

        val filled = left + (right - left) * progress.coerceIn(0f, 1f)
        if (filled - left >= BAR_HEIGHT) {
            graphics.drawRoundedRect(
                left, BAR_Y, filled, BAR_Y + BAR_HEIGHT, radius,
                fillColor = Color4b.LIQUID_BOUNCE.fade(alpha)
            )
        }
    }

    /**
     * An expanding ring on the floor, flattened to an ellipse.
     */
    private fun ripple(graphics: GuiGraphicsExtractor, time: Float, strength: Float, alpha: Float) {
        val t = time / 900f
        if (t !in 0f..1f) {
            return
        }

        val radius = 120f + 760f * Easing.EXPONENTIAL_OUT.transform(t)
        val color = ARGB.color(alpha * strength * 0.7f * (1f - t), Color4b.LIQUID_BOUNCE.argb)
        graphics.pose().withPush {
            translate(MARK_CENTER, FLOOR)
            scale(1f, 0.22f)
            graphics.drawCircle(0f, 0f, radius, radius - 22f / (0.4f + t)) { color }
        }
    }

    private fun droplets(graphics: GuiGraphicsExtractor, time: Float, alpha: Float) {
        if (time !in 0f..DROPLET_LIFE) {
            return
        }

        val life = 1f - time / DROPLET_LIFE
        val color = ARGB.white(alpha * life)
        for (i in DROPLETS.indices step 2) {
            val x = MARK_CENTER + DROPLETS[i] * time
            val y = FLOOR + DROPLETS[i + 1] * time + GRAVITY * time * time / 2f
            graphics.drawCircle(x, y, 30f * life + 6f) { color }
        }
    }

    /**
     * Damped spring after an impact [time] ms ago.
     */
    private fun squash(time: Float, amplitude: Float): Float {
        if (time < 0f) {
            return 0f
        }

        return amplitude * exp(-time / 140f) * cos(time / 240f * 2f * PI.toFloat())
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

    private fun GuiGraphicsExtractor.blit(region: Region, u: Int, width: Int, alpha: Float, drawX: Int = u) {
        if (alpha <= 0f) {
            return
        }

        blit(
            ClientRenderPipelines.GUI.SmoothTexture,
            ClientLogoTexture.CLIENT_LOGO,
            drawX,
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
