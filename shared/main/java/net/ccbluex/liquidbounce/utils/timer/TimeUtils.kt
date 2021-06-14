/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.timer

import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextInt
import java.util.*
import java.util.concurrent.TimeUnit

object TimeUtils
{
	@JvmStatic
	private val TIMER_INSTANCE = Timer("LiquidBounce_Timer")

	@JvmStatic
	fun randomDelay(minDelay: Int, maxDelay: Int): Long = nextInt(minDelay, maxDelay).toLong()

	@JvmStatic
	fun randomClickDelay(minCPS: Int, maxCPS: Int): Long = (Math.random() * (1000 / minCPS - 1000 / maxCPS + 1) + 1000 / maxCPS).toLong()

	@JvmStatic
	fun nanosecondsToString(nanoseconds: Long): String = "${nanoseconds}ns, ${TimeUnit.NANOSECONDS.toMicros(nanoseconds)}us, ${TimeUnit.NANOSECONDS.toMillis(nanoseconds)}ms"

	@JvmStatic
	fun scheduleDelayedTask(task: () -> Unit, delay: Long)
	{
		TIMER_INSTANCE.schedule(object : TimerTask()
		{
			override fun run()
			{
				task()
			}
		}, delay)
	}
}
