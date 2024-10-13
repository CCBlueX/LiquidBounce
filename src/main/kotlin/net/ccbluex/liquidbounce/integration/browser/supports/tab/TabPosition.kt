package net.ccbluex.liquidbounce.integration.browser.supports.tab

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

    companion object {
        val FULLSCREEN
            get() = TabPosition(0, 0, mc.window.framebufferWidth, mc.window.framebufferHeight, true)
    }
}
