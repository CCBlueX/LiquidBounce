/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.api.minecraft.client.settings.IKeyBinding
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "AntiAFK", description = "Prevents you from getting kicked for being AFK.", category = ModuleCategory.PLAYER)
class AntiAFK : Module()
{
	private val swingDelayTimer = MSTimer()
	private val delayTimer = MSTimer()

	private val modeValue = ListValue("Mode", arrayOf("Old", "Random", "Custom"), "Random")

	private val swingDelayValue = IntegerValue("SwingDelay", 100, 0, 1000)
	private val rotationDelayValue = IntegerValue("RotationDelay", 100, 0, 1000)
	private val rotationAngleValue = FloatValue("RotationAngle", 1f, -180F, 180F)

	private val jumpValue = BoolValue("Jump", true)
	private val moveValue = BoolValue("Move", true)
	private val rotateValue = BoolValue("Rotate", true)
	private val swingValue = BoolValue("Swing", true)

	private var shouldMove = false
	private var randomTimerDelay = 500L

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val gameSettings = mc.gameSettings

		when (modeValue.get().toLowerCase())
		{
			"old" ->
			{
				gameSettings.keyBindForward.pressed = true

				if (delayTimer.hasTimePassed(500))
				{
					thePlayer.rotationYaw += 180F
					delayTimer.reset()
				}
			}

			"random" ->
			{
				getRandomMoveKeyBind().pressed = shouldMove

				if (!delayTimer.hasTimePassed(randomTimerDelay)) return
				shouldMove = false
				randomTimerDelay = 500L
				when (RandomUtils.nextInt(0, 6))
				{
					0 -> if (thePlayer.onGround) thePlayer.jump()

					1 -> if (!thePlayer.isSwingInProgress) thePlayer.swingItem()

					2 ->
					{
						randomTimerDelay = RandomUtils.nextInt(0, 1000).toLong()
						shouldMove = true
					}

					3 ->
					{
						thePlayer.inventory.currentItem = RandomUtils.nextInt(0, 9)
						mc.playerController.updateController()
					}

					4 ->
					{
						thePlayer.rotationYaw += RandomUtils.nextFloat(-180.0F, 180.0F)
					}

					5 ->
					{
						if (thePlayer.rotationPitch <= -90 || thePlayer.rotationPitch >= 90) thePlayer.rotationPitch = 0F
						thePlayer.rotationPitch += RandomUtils.nextFloat(-10.0F, 10.0F)
					}
				}
				delayTimer.reset()
			}

			"custom" ->
			{
				if (moveValue.get()) gameSettings.keyBindForward.pressed = true

				if (jumpValue.get() && thePlayer.onGround) thePlayer.jump()

				if (rotateValue.get() && delayTimer.hasTimePassed(rotationDelayValue.get().toLong()))
				{
					thePlayer.rotationYaw += rotationAngleValue.get()
					if (thePlayer.rotationPitch <= -90 || thePlayer.rotationPitch >= 90) thePlayer.rotationPitch = 0F
					thePlayer.rotationPitch += RandomUtils.nextFloat(0F, 1F) * 2 - 1
					delayTimer.reset()
				}

				if (swingValue.get() && !thePlayer.isSwingInProgress && swingDelayTimer.hasTimePassed(swingDelayValue.get().toLong()))
				{
					thePlayer.swingItem()
					swingDelayTimer.reset()
				}
			}
		}
	}

	private fun getRandomMoveKeyBind(): IKeyBinding
	{
		val gameSettings = mc.gameSettings

		return when (RandomUtils.nextInt(0, 4))
		{
			0 -> gameSettings.keyBindRight
			1 -> gameSettings.keyBindLeft
			2 -> gameSettings.keyBindBack
			else -> gameSettings.keyBindForward
		}
	}

	override fun onDisable()
	{
		val gameSettings = mc.gameSettings

		if (!gameSettings.isKeyDown(gameSettings.keyBindForward)) gameSettings.keyBindForward.pressed = false
	}
}
