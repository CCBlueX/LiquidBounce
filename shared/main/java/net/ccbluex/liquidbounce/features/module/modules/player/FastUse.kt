/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.WorkerUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "FastUse", description = "Allows you to use items faster.", category = ModuleCategory.PLAYER)
class FastUse : Module()
{

	private val modeValue = ListValue("Mode", arrayOf("Instant", "NCP", "AAC", "Custom"), "NCP")
	private val ncpModeValue = ListValue("NCP-Mode", arrayOf("AtOnce", "Constant"), "AtOnce")

	private val noMoveValue = BoolValue("NoMove", false)

	private val ncpWaitTicksValue = IntegerValue("NCP-AtOnce-WaitTicks", 14, 0, 20)
	private val ncpPacketsValue = IntegerValue("NCP-AtOnce-Packets", 20, 10, 100)
	private val ncpConstantPacketsValue = IntegerValue("NCP-Constant-Packets", 1, 1, 10)
	private val ncpTimerValue = FloatValue("NCP-Timer", 1.0f, 0.2f, 1.5f)

	private val aacTimerValue = FloatValue("AAC-Timer", 1.22f, 1.1f, 1.5f)

	private val delayValue = IntegerValue("CustomDelay", 0, 0, 300)
	private val customSpeedValue = IntegerValue("CustomSpeed", 2, 1, 35)
	private val customTimer = FloatValue("CustomTimer", 1.1f, 0.5f, 2f)

	private val msTimer = MSTimer()
	private var usedTimer = false

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val timer = mc.timer

		if (usedTimer)
		{
			timer.timerSpeed = 1F
			usedTimer = false
		}

		if (!thePlayer.isUsingItem)
		{
			msTimer.reset()
			return
		}

		val usingItem = thePlayer.itemInUse?.item

		val provider = classProvider

		if (provider.isItemFood(usingItem) || provider.isItemBucketMilk(usingItem) || provider.isItemPotion(usingItem))
		{
			val netHandler = mc.netHandler
			val onGround = thePlayer.onGround

			when (modeValue.get().toLowerCase())
			{
				"instant" ->
				{
					WorkerUtils.workers.execute {
						repeat(35) {
							netHandler.addToSendQueue(provider.createCPacketPlayer(onGround))
						}
					}
				}

				"ncp" ->
				{
					timer.timerSpeed = ncpTimerValue.get()
					when (ncpModeValue.get().toLowerCase())
					{
						"atonce" -> if (thePlayer.itemInUseDuration > ncpWaitTicksValue.get())
						{
							WorkerUtils.workers.execute {
								repeat(ncpPacketsValue.get()) {
									netHandler.addToSendQueue(provider.createCPacketPlayer(onGround))
								}
							}
						}

						"constant" -> repeat(ncpConstantPacketsValue.get()) {
							netHandler.addToSendQueue(provider.createCPacketPlayer(onGround))
						}
					}

					usedTimer = true
				}

				"aac" ->
				{
					timer.timerSpeed = aacTimerValue.get()
					usedTimer = true
				}

				"custom" ->
				{
					timer.timerSpeed = customTimer.get()
					usedTimer = true

					if (!msTimer.hasTimePassed(delayValue.get().toLong())) return

					WorkerUtils.workers.execute {
						repeat(customSpeedValue.get()) {
							netHandler.addToSendQueue(provider.createCPacketPlayer(onGround))
						}
					}

					msTimer.reset()
				}
			}
		}
	}

	@EventTarget
	fun onMove(event: MoveEvent?)
	{
		event ?: return
		val thePlayer = mc.thePlayer ?: return

		if (!state || !thePlayer.isUsingItem || !noMoveValue.get()) return

		val usingItem = (thePlayer.itemInUse ?: return).item

		val provider = classProvider

		if (provider.isItemFood(usingItem) || provider.isItemBucketMilk(usingItem) || provider.isItemPotion(usingItem)) event.zero()
	}

	override fun onDisable()
	{
		if (usedTimer)
		{
			mc.timer.timerSpeed = 1F
			usedTimer = false
		}
	}

	override val tag: String
		get() = "${modeValue.get()}${if (modeValue.get().equals("NCP", ignoreCase = true)) "-${ncpModeValue.get()}" else ""}"
}
