/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import kotlin.math.hypot

object MovementUtils : MinecraftInstance()
{
	fun getSpeed(thePlayer: IEntityPlayerSP): Float
	{
		val mX = thePlayer.motionX
		val mZ = thePlayer.motionZ
		return hypot(mX, mZ).toFloat()
	}

	@JvmStatic
	fun isMoving(thePlayer: IEntityPlayerSP): Boolean = thePlayer.movementInput.moveForward != 0f || thePlayer.movementInput.moveStrafe != 0f

	fun hasMotion(thePlayer: IEntityPlayerSP): Boolean = thePlayer.motionX != 0.0 && thePlayer.motionZ != 0.0 && thePlayer.motionY != 0.0

	@JvmStatic
	@JvmOverloads
	fun strafe(thePlayer: IEntityPlayerSP, speed: Float = getSpeed(thePlayer))
	{
		if (!isMoving(thePlayer)) return

		val dir = getDirection(thePlayer)
		thePlayer.motionX = (-functions.sin(dir) * speed).toDouble()
		thePlayer.motionZ = (functions.cos(dir) * speed).toDouble()
	}

	@JvmStatic
	fun forward(thePlayer: IEntityPlayerSP, length: Double)
	{
		val yaw = WMathHelper.toRadians(thePlayer.rotationYaw)
		thePlayer.setPosition(thePlayer.posX + -functions.sin(yaw) * length, thePlayer.posY, thePlayer.posZ + functions.cos(yaw) * length)
	}

	@JvmStatic
	fun getDirection(thePlayer: IEntityPlayerSP): Float = WMathHelper.toRadians(getDirectionDegrees(thePlayer))

	@JvmStatic
	fun getDirectionDegrees(thePlayer: IEntityPlayerSP): Float
	{
		var rotationYaw = thePlayer.rotationYaw
		var forward = 1f

		if (thePlayer.moveForward < 0f)
		{
			rotationYaw += 180f
			forward = -0.5f
		}
		else if (thePlayer.moveForward > 0f) forward = 0.5f

		if (thePlayer.moveStrafing > 0f) rotationYaw -= 90f * forward
		if (thePlayer.moveStrafing < 0f) rotationYaw += 90f * forward

		return rotationYaw
	}
}
