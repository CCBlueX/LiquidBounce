package net.ccbluex.liquidbounce.integration.backend.browser

import net.ccbluex.liquidbounce.utils.client.mc
import kotlin.math.ln

/**
 * Represents a browser viewport with position, dimensions and rendering quality utilities
 */
data class BrowserViewport(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val fullScreen: Boolean = false
) {

    /**
     * Transform global coordinates to viewport-relative coordinates
     * @return Pair of (transformedX, transformedY) coordinates
     */
    fun transform(globalX: Double, globalY: Double): Pair<Double, Double> =
        Pair(globalX - x, globalY - y)

    /**
     * Get the scaled dimensions for rendering based on quality setting
     * @return Pair of (scaledWidth, scaledHeight)
     */
    fun getScaledDimensions(quality: Float): Pair<Int, Int> =
        Pair(
            (width * quality).toInt().coerceAtLeast(1),
            (height * quality).toInt().coerceAtLeast(1)
        )

    /**
     * Calculate zoom level based on quality factor
     */
    fun getZoomLevel(quality: Float): Double = ln(quality.toDouble()) / ln(1.2)

    /**
     * Transform mouse coordinates according to quality scaling
     * @return Pair of (scaledX, scaledY) coordinates
     */
    fun transformMouse(mouseX: Double, mouseY: Double, quality: Float): Pair<Int, Int> =
        Pair((mouseX * quality).toInt(), (mouseY * quality).toInt())

    companion object {
        /**
         * Creates a fullscreen viewport matching the current window dimensions
         */
        val FULLSCREEN
            get() = BrowserViewport(
                x = 0,
                y = 0,
                width = mc.window.framebufferWidth,
                height = mc.window.framebufferHeight,
                fullScreen = true
            )
    }
}
