/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper

object MovementUtils : MinecraftInstance()
{
	@JvmStatic
	fun getDirection(rotationYaw: Float, moveForward: Float, moveStrafing: Float): Float = WMathHelper.toRadians(getDirectionDegrees(rotationYaw, moveForward, moveStrafing))

	@JvmStatic
	fun getDirectionDegrees(rotationYaw: Float, moveForward: Float, moveStrafing: Float): Float
	{
		var yaw = rotationYaw % 360f
		var forward = 1f

		if (moveForward < 0f)
		{
			yaw += 180f
			forward = -0.5f
		}
		else if (moveForward > 0f) forward = 0.5f

		if (moveStrafing > 0f) yaw -= 90f * forward
		if (moveStrafing < 0f) yaw += 90f * forward

		return yaw
	}
}
