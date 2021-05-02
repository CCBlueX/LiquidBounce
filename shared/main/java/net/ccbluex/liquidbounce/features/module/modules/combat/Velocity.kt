/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "Velocity", description = "Allows you to modify the amount of knockback you take. (a.k.a. AntiKnockback)", category = ModuleCategory.COMBAT)
class Velocity : Module()
{

	/**
	 * OPTIONS
	 */
	val horizontalValue = FloatValue("Horizontal", 0F, 0F, 1F)
	val verticalValue = FloatValue("Vertical", 0F, 0F, 1F)
	val modeValue = ListValue("Mode", arrayOf("Simple", "AAC3.1.2", "AACPush", "AAC3.2.0-Reverse", "AAC3.3.4-Reverse", "AAC3.5.0-Zero", "Jump", "Glitch"), "Simple")

	// AAC Reverse
	private val reverseStrengthValue = FloatValue("AAC3.2.0-Reverse-Strength", 1F, 0.1F, 1F)
	private val reverse2StrengthValue = FloatValue("AAC3.3.4-Reverse-Strength", 0.05F, 0.02F, 0.1F)

	// AAC Push
	private val aacPushXZReducerValue = FloatValue("AACPushXZReducer", 2F, 1F, 3F)
	private val aacPushYReducerValue = BoolValue("AACPushYReducer", true)

	/**
	 * VALUES
	 */
	var velocityTimer = MSTimer()
	var velocityInput = false

	// SmoothReverse
	private var reverseHurt = false

	// AACPush
	private var jump = false

	override val tag: String
		get() = modeValue.get()

	override fun onDisable()
	{
		mc.thePlayer?.speedInAir = 0.02F
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.isInWater || thePlayer.isInLava || thePlayer.isInWeb) return

		when (modeValue.get().toLowerCase())
		{
			"jump" -> if (thePlayer.hurtTime > 0 && thePlayer.onGround)
			{
				thePlayer.motionY = 0.42

				val yaw = WMathHelper.toRadians(thePlayer.rotationYaw)

				val func = functions

				thePlayer.motionX -= func.sin(yaw) * 0.2
				thePlayer.motionZ += func.cos(yaw) * 0.2
			}

			"glitch" ->
			{
				thePlayer.noClip = velocityInput

				if (thePlayer.hurtTime == 7) thePlayer.motionY = 0.4

				velocityInput = false
			}

			"aac3.1.2" -> if (velocityInput && velocityTimer.hasTimePassed(80L))
			{
				thePlayer.motionX *= horizontalValue.get()
				thePlayer.motionZ *= horizontalValue.get()

				//mc.thePlayer.motionY *= verticalValue.get() ?

				velocityInput = false
			}

			"aac3.2.0-reverse" ->
			{
				if (!velocityInput) return

				if (!thePlayer.onGround) MovementUtils.strafe(thePlayer, MovementUtils.getSpeed(thePlayer) * reverseStrengthValue.get())
				else if (velocityTimer.hasTimePassed(80L)) velocityInput = false
			}

			"aac3.3.4-reverse" ->
			{
				if (!velocityInput)
				{
					thePlayer.speedInAir = 0.02F
					return
				}

				if (thePlayer.hurtTime > 0) reverseHurt = true

				if (!thePlayer.onGround)
				{
					if (reverseHurt) thePlayer.speedInAir = reverse2StrengthValue.get()
				}
				else if (velocityTimer.hasTimePassed(80L))
				{
					velocityInput = false
					reverseHurt = false
				}
			}

			"aacpush" ->
			{
				if (jump)
				{
					if (thePlayer.onGround) jump = false
				}
				else
				{

					// Strafe
					if (thePlayer.hurtTime > 0 && thePlayer.motionX != 0.0 && thePlayer.motionZ != 0.0) thePlayer.onGround = true

					// Reduce Y
					if (thePlayer.hurtResistantTime > 0 && aacPushYReducerValue.get() && !LiquidBounce.moduleManager[Speed::class.java].state) thePlayer.motionY -= 0.014999993
				}

				// Reduce XZ
				if (thePlayer.hurtResistantTime >= 19)
				{
					val reduce = aacPushXZReducerValue.get()

					thePlayer.motionX /= reduce
					thePlayer.motionZ /= reduce
				}
			}

			"aac3.5.0-zero" -> if (thePlayer.hurtTime > 0)
			{
				if (!velocityInput || thePlayer.onGround || thePlayer.fallDistance > 2F) return

				// Generate AAC Movement-check flag
				thePlayer.motionY -= 1.0
				thePlayer.isAirBorne = true
				thePlayer.onGround = true
			}
			else velocityInput = false
		}
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val packet = event.packet

		if (classProvider.isSPacketEntityVelocity(packet))
		{
			val packetEntityVelocity = packet.asSPacketEntityVelocity()


			if ((mc.theWorld?.getEntityByID(packetEntityVelocity.entityID) ?: return) != thePlayer) return

			velocityTimer.reset()

			when (modeValue.get().toLowerCase())
			{
				"simple" ->
				{
					val horizontal = horizontalValue.get()
					val vertical = verticalValue.get()

					if (horizontal == 0F && vertical == 0F) event.cancelEvent()

					packetEntityVelocity.motionX = (packetEntityVelocity.motionX * horizontal).toInt()
					packetEntityVelocity.motionY = (packetEntityVelocity.motionY * vertical).toInt()
					packetEntityVelocity.motionZ = (packetEntityVelocity.motionZ * horizontal).toInt()
				}

				"aac3.1.2", "aac3.2.0-reverse", "aac3.3.4-reverse", "aac3.5.0-zero" -> velocityInput = true

				"glitch" ->
				{
					if (!thePlayer.onGround) return

					velocityInput = true
					event.cancelEvent()
				}
			}
		}

		// Explosion packets are handled by MixinNetHandlerPlayClient
	}

	@EventTarget
	fun onJump(event: JumpEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.isInWater || thePlayer.isInLava || thePlayer.isInWeb) return

		when (modeValue.get().toLowerCase())
		{
			"aacpush" ->
			{
				jump = true

				if (!thePlayer.isCollidedVertically) event.cancelEvent()
			}

			"aac3.5.0-zero" -> if (thePlayer.hurtTime > 0) event.cancelEvent()
		}
	}
}
