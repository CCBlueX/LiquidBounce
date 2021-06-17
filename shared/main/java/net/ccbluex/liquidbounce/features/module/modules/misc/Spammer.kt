/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.TickEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.randomString
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.TextValue

import kotlin.random.Random

@ModuleInfo(name = "Spammer", description = "Spams the chat with a given message.", category = ModuleCategory.MISC)
class Spammer : Module()
{
	/**
	 * Options
	 */
	val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 1000, 0, 5000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val minDelayValueObject = minDelayValue.get()
			if (minDelayValueObject > newValue) set(minDelayValueObject)
			delay = TimeUtils.randomDelay(minDelayValue.get(), this.get())
		}
	}
	val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 500, 0, 5000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val maxDelayValueObject = maxDelayValue.get()
			if (maxDelayValueObject < newValue) set(maxDelayValueObject)
			delay = TimeUtils.randomDelay(this.get(), maxDelayValue.get())
		}
	}
	private val messageValue = TextValue("Message", LiquidBounce.CLIENT_NAME + " Client | liquidbounce(.net) | CCBlueX on yt")
	private val customValue = BoolValue("Custom", false)

	/**
	 * Variables
	 */
	private val msTimer = MSTimer()
	var delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val thePlayer = mc.thePlayer ?: return

		if (msTimer.hasTimePassed(delay))
		{
			val message = messageValue.get()

			thePlayer.sendChatMessage(if (customValue.get()) replace(message) else message + " >" + randomString(5 + Random.nextInt(5)) + "<")

			msTimer.reset()
			delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
		}
	}

	@EventTarget
	fun onWorld(event: WorldEvent)
	{
		if (event.worldClient == null) state = false // Disable module in case you left (or kicked) from the server
	}

	@EventTarget
	fun onTick(@Suppress("UNUSED_PARAMETER") event: TickEvent?)
	{
		if (mc.thePlayer == null || mc.theWorld == null) state = false // Disable module in case you left (or kicked) from the server
	}

	companion object
	{
		private fun replace(original: String): String
		{
			var replace = original

			while (replace.contains("%f")) replace = "${replace.substringBefore("%f")}${Random.nextFloat()}${replace.substring(replace.indexOf("%f") + 2)}"
			while (replace.contains("%i")) replace = "${replace.substringBefore("%i")}${Random.nextInt(10000)}${replace.substring(replace.indexOf("%i") + 2)}"
			while (replace.contains("%s")) replace = "${replace.substringBefore("%s")}${randomString(Random.nextInt(8) + 1)}${replace.substring(replace.indexOf("%s") + 2)}"
			while (replace.contains("%ss")) replace = "${replace.substringBefore("%ss")}${randomString(Random.nextInt(4) + 1)}${replace.substring(replace.indexOf("%ss") + 3)}"
			while (replace.contains("%ls")) replace = "${replace.substringBefore("%ls")}${randomString(Random.nextInt(15) + 1)}${replace.substring(replace.indexOf("%ls") + 3)}"

			return replace
		}
	}
}
