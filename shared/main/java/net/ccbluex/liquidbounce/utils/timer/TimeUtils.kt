/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.timer

import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextInt
import java.util.concurrent.TimeUnit

object TimeUtils
{
    @JvmStatic
    fun randomDelay(minDelay: Int, maxDelay: Int): Long = nextInt(minDelay, maxDelay).toLong()

    @JvmStatic
    fun randomClickDelay(minCPS: Int, maxCPS: Int): Long = (Math.random() * (1000 / minCPS - 1000 / maxCPS + 1) + 1000 / maxCPS).toLong()

    @JvmStatic
    fun nanosecondsToString(nanoseconds: Long): String = "${nanoseconds}ns, ${TimeUnit.NANOSECONDS.toMicros(nanoseconds)}us, ${TimeUnit.NANOSECONDS.toMillis(nanoseconds)}ms"
}
