/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

/**
 * @author superblaubeere27
 */
object CPSCounter {
    private const val MAX_CPS = 50
    private val TIMESTAMP_BUFFERS = arrayOfNulls<RollingArrayLongBuffer>(MouseButton.values().size)

    init {
        TIMESTAMP_BUFFERS.fill(RollingArrayLongBuffer(MAX_CPS))
    }

    /**
     * Registers a mouse button click
     *
     * @param button The clicked button
     */
    fun registerClick(button: MouseButton) = TIMESTAMP_BUFFERS[button.ordinal]!!.add(System.currentTimeMillis())

    /**
     * Gets the count of clicks that have occurrence since the last 1000ms
     *
     * @param button The mouse button
     * @return The CPS
     */
    fun getCPS(button: MouseButton) = TIMESTAMP_BUFFERS[button.ordinal]!!.getTimestampsSince(System.currentTimeMillis() - 1000L)

    enum class MouseButton { LEFT, MIDDLE, RIGHT }
}
