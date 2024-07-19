package net.ccbluex.liquidbounce.web.browser.supports.tab

import net.ccbluex.liquidbounce.utils.client.mc

/**
 * Tab dimension
 *
 * renderTexture will always draw the texture in the top left corner of the screen,
 * therefore we do not need to translate anything else
 */
data class TabMargin(
    val top: Int = 0,
    val left: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {

    fun width(width: Int = mc.window.width): Int {
        return width - right
    }

    fun height(height: Int = mc.window.height): Int {
        return height - bottom
    }

    fun x(x: Double): Double {
        return (x - left).coerceIn(0.0, width(mc.window.width).toDouble())
    }

    fun y(y: Double): Double {
        return (y - top).coerceIn(0.0, height(mc.window.height).toDouble())
    }

    fun leftScaled(): Double {
        return left.toDouble() * (mc.window.width / mc.window.scaledWidth)
    }

    fun topScaled(): Double {
        return top.toDouble() * (mc.window.height / mc.window.scaledHeight)
    }

    companion object {
        val DEFAULT = TabMargin()
    }
}
