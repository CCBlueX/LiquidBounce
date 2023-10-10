/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.timing

class MSTimer {
    private var time = -1L

    fun hasTimePassed(ms: Number) = System.currentTimeMillis() >= time + ms.toLong()

    fun hasTimeLeft(ms: Number) = ms.toLong() + time - System.currentTimeMillis()

    fun reset() {
        time = System.currentTimeMillis()
    }

    fun zero() {
        time = -1L
    }
}
