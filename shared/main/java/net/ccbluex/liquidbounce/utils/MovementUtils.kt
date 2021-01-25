/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import kotlin.math.hypot

object MovementUtils : MinecraftInstance()
{
	val speed: Float
		get()
		{
			val mX = mc.thePlayer!!.motionX
			val mZ = mc.thePlayer!!.motionZ
			return hypot(mX, mZ).toFloat()
		}

	@JvmStatic
	val isMoving: Boolean
		get() = mc.thePlayer != null && (mc.thePlayer!!.movementInput.moveForward != 0f || mc.thePlayer!!.movementInput.moveStrafe != 0f)

	fun hasMotion(): Boolean = mc.thePlayer!!.motionX != 0.0 && mc.thePlayer!!.motionZ != 0.0 && mc.thePlayer!!.motionY != 0.0

	@JvmStatic
	@JvmOverloads
	fun strafe(speed: Float = this.speed)
	{
		if (!isMoving) return
		val yaw = direction
		val thePlayer = mc.thePlayer!!
		thePlayer.motionX = (-functions.sin(yaw) * speed).toDouble()
		thePlayer.motionZ = (functions.cos(yaw) * speed).toDouble()
	}

	@JvmStatic
	fun forward(length: Double)
	{
		val thePlayer = mc.thePlayer ?: return

		val yaw = WMathHelper.toRadians(thePlayer.rotationYaw)
		thePlayer.setPosition(thePlayer.posX + -functions.sin(yaw) * length, thePlayer.posY, thePlayer.posZ + functions.cos(yaw) * length)
	}

	@JvmStatic
	val direction: Float
		get() = WMathHelper.toRadians(directionDegrees)

	@JvmStatic
	val directionDegrees: Float
		get()
		{
			val thePlayer = mc.thePlayer!!
			var rotationYaw = thePlayer.rotationYaw
			var forward = 1f
			if (thePlayer.moveForward < 0f)
			{
				rotationYaw += 180f
				forward = -0.5f
			} else if (thePlayer.moveForward > 0f) forward = 0.5f
			if (thePlayer.moveStrafing > 0f) rotationYaw -= 90f * forward
			if (thePlayer.moveStrafing < 0f) rotationYaw += 90f * forward
			return rotationYaw
		}
}
