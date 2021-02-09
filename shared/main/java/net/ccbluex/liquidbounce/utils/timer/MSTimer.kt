/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.timer

class MSTimer
{
	private var time = -1L

	fun hasTimePassed(millis: Long): Boolean = System.currentTimeMillis() >= time + millis

	fun hasTimeLeft(millis: Long): Long = millis + time - System.currentTimeMillis()

	fun getTime(): Long = System.currentTimeMillis() - time

	fun reset()
	{
		time = System.currentTimeMillis()
	}
}
