package net.ccbluex.liquidbounce.integration.browser.tab

import net.ccbluex.liquidbounce.utils.client.mc

/**
 * Tab position
 */
data class TabPosition(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val fullScreen: Boolean = false
) {

    fun x(x: Double) = x - this.x
    fun y(y: Double) = y - this.y

    /**
     * Get the scaled width for rendering based on quality setting
     */
    fun getScaledWidth(quality: Float): Int = (width * quality).toInt().coerceAtLeast(1)

    /**
     * Get the scaled height for rendering based on quality setting
     */
    fun getScaledHeight(quality: Float): Int = (height * quality).toInt().coerceAtLeast(1)

    companion object {
        val FULLSCREEN
            get() = TabPosition(0, 0, mc.window.framebufferWidth, mc.window.framebufferHeight, true)
    }
}
