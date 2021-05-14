/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.util.ITimer
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
import kotlin.math.ceil

@ModuleInfo(name = "FastUse", description = "Allows you to use items faster.", category = ModuleCategory.PLAYER)
class FastUse : Module()
{

	private val modeValue = ListValue("Mode", arrayOf("Instant", "NCP", "AAC", "Custom"), "NCP")
	private val ncpModeValue = ListValue("NCP-Mode", arrayOf("AtOnce", "Constant"), "AtOnce")

	private val noMoveValue = BoolValue("NoMove", false)

	private val ncpWaitTicksValue = IntegerValue("NCP-AtOnce-WaitTicks", 14, 0, 25)
	private val ncpPacketsValue = IntegerValue("NCP-AtOnce-Packets", 20, 12, 100)
	private val ncpConstantPacketsValue = IntegerValue("NCP-Constant-Packets", 1, 1, 10)
	private val ncpTimerValue = FloatValue("NCP-Timer", 1.0f, 0.2f, 1.5f)

	private val aacTimerValue = FloatValue("AAC-Timer", 1.22f, 1.1f, 1.5f)

	private val delayValue = IntegerValue("CustomDelay", 0, 0, 300)
	private val customSpeedValue = IntegerValue("CustomSpeed", 2, 1, 35)
	private val customTimer = FloatValue("CustomTimer", 1.1f, 0.5f, 2f)

	private val msTimer = MSTimer()
	private var usedTimer = false

	@EventTarget(ignoreCondition = true)
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		perform(mc.thePlayer ?: return, mc.timer)
	}

	fun perform(thePlayer: IEntityPlayerSP, timer: ITimer, customItem: IItem? = null, usingItemTicks: Int? = null): Int
	{
		if (!state) return 32

		if (usedTimer)
		{
			timer.timerSpeed = 1F
			usedTimer = false
		}

		if (customItem == null && !thePlayer.isUsingItem)
		{
			msTimer.reset()
			return -1
		}

		val provider = classProvider
		val itemInUse = customItem ?: thePlayer.itemInUse?.item
		val itemInUseDuration = usingItemTicks ?: thePlayer.itemInUseDuration

		if (provider.isItemFood(itemInUse) || provider.isItemBucketMilk(itemInUse) || provider.isItemPotion(itemInUse))
		{
			val workers = WorkerUtils.workers

			val netHandler = mc.netHandler
			val onGround = thePlayer.onGround

			when (modeValue.get().toLowerCase())
			{
				"instant" ->
				{
					repeat(35) {
						netHandler.addToSendQueue(provider.createCPacketPlayer(onGround))
					}

					mc.playerController.onStoppedUsingItem(thePlayer)

					return 0
				}

				"ncp" ->
				{
					timer.timerSpeed = ncpTimerValue.get()

					usedTimer = true

					when (ncpModeValue.get().toLowerCase())
					{
						"atonce" ->
						{
							if (itemInUseDuration > ncpWaitTicksValue.get())
							{
								repeat(ncpPacketsValue.get()) {
									netHandler.addToSendQueue(provider.createCPacketPlayer(onGround))
								}

								mc.playerController.onStoppedUsingItem(thePlayer)
							}

							return ncpWaitTicksValue.get() + 2
						}

						"constant" ->
						{
							repeat(ncpConstantPacketsValue.get()) {
								netHandler.addToSendQueue(provider.createCPacketPlayer(onGround))
							}

							return 32 / (ncpConstantPacketsValue.get() + 1)
						}
					}
				}

				"aac" ->
				{
					timer.timerSpeed = aacTimerValue.get()
					usedTimer = true

					return 32
				}

				"custom" ->
				{
					timer.timerSpeed = customTimer.get()
					usedTimer = true

					if (msTimer.hasTimePassed(delayValue.get().toLong()))
					{
						workers.execute {
							repeat(customSpeedValue.get()) {
								netHandler.addToSendQueue(provider.createCPacketPlayer(onGround))
							}
						}

						msTimer.reset()
					}

					return ceil(32.0F / ((customSpeedValue.get().toFloat() + 1) * (1600.0F * (delayValue.get().toFloat() / 1600.0F)))).coerceAtMost(32.0F).toInt()
				}
			}
		}

		return -1
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
