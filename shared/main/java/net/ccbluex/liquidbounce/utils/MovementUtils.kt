/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import kotlin.math.hypot

object MovementUtils : MinecraftInstance()
{
	@JvmStatic
	fun getSpeed(thePlayer: IEntity): Float
	{
		val mX = thePlayer.motionX
		val mZ = thePlayer.motionZ
		return hypot(mX, mZ).toFloat()
	}

	@JvmStatic
	fun isMoving(thePlayer: IEntityPlayerSP): Boolean = thePlayer.movementInput.moveForward != 0f || thePlayer.movementInput.moveStrafe != 0f

	@JvmStatic
	fun cantBoostUp(thePlayer: IEntityPlayer): Boolean = thePlayer.isInWater || thePlayer.isInLava || thePlayer.isInWeb || thePlayer.isOnLadder || thePlayer.isRiding

	@JvmStatic
	fun hasMotion(thePlayer: IEntity): Boolean = thePlayer.motionX != 0.0 && thePlayer.motionZ != 0.0 && thePlayer.motionY != 0.0

	@JvmStatic
	@JvmOverloads
	fun strafe(thePlayer: IEntityPlayerSP, speed: Float = getSpeed(thePlayer), directionDegrees: Float = getDirectionDegrees(thePlayer))
	{
		if (!isMoving(thePlayer)) return

		val dir = WMathHelper.toRadians(directionDegrees)
		thePlayer.motionX = (-functions.sin(dir) * speed).toDouble()
		thePlayer.motionZ = (functions.cos(dir) * speed).toDouble()
	}

	@JvmStatic
	@JvmOverloads
	fun boost(thePlayer: IEntityPlayer, motion: Float = getSpeed(thePlayer), directionDegrees: Float = getDirectionDegrees(thePlayer))
	{
		val dir = WMathHelper.toRadians(directionDegrees)
		thePlayer.motionX -= functions.sin(dir) * motion
		thePlayer.motionZ += functions.cos(dir) * motion
	}

	@JvmStatic
	fun forward(thePlayer: IEntity, length: Double, directionDegrees: Float = thePlayer.rotationYaw)
	{
		val func = functions

		val yaw = WMathHelper.toRadians(directionDegrees)

		thePlayer.setPosition(thePlayer.posX - func.sin(yaw) * length, thePlayer.posY, thePlayer.posZ + func.cos(yaw) * length)
	}

	@JvmStatic
	fun multiply(entity: IEntity, multiplier: Float) = multiply(entity, multiplier.toDouble())

	@JvmStatic
	fun multiply(entity: IEntity, multiplier: Double)
	{
		entity.motionX *= multiplier
		entity.motionZ *= multiplier
	}

	@JvmStatic
	fun divide(entity: IEntity, divisor: Float) = divide(entity, divisor.toDouble())

	@JvmStatic
	fun divide(entity: IEntity, divisor: Double)
	{
		entity.motionX /= divisor
		entity.motionZ /= divisor
	}

	@JvmStatic
	fun getDirection(thePlayer: IEntityPlayer): Float = WMathHelper.toRadians(getDirectionDegrees(thePlayer))

	@JvmStatic
	fun getDirection(rotationYaw: Float, moveForward: Float, moveStrafing: Float): Float = WMathHelper.toRadians(getDirectionDegrees(rotationYaw, moveForward, moveStrafing))

	@JvmStatic
	fun getDirectionDegrees(thePlayer: IEntityPlayer): Float = getDirectionDegrees(thePlayer.rotationYaw, thePlayer.moveForward, thePlayer.moveStrafing)

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

	/**
	 * @return The amplifier of Speed potion effect which applied on thePlayer (1~) (If thePlayer doesn't have Speed potion effect, it returns 0)
	 */
	@JvmStatic
	fun getSpeedEffectAmplifier(thePlayer: IEntityLivingBase) = getEffectAmplifier(thePlayer, PotionType.MOVE_SPEED)

	@JvmStatic
	fun getEffectAmplifier(thePlayer: IEntityLivingBase, potionType: PotionType) = thePlayer.getActivePotionEffect(classProvider.getPotionEnum(potionType))?.amplifier?.plus(1) ?: 0

	@JvmStatic
	fun zeroXZ(thePlayer: IEntity)
	{
		thePlayer.motionX = 0.0
		thePlayer.motionZ = 0.0
	}

	@JvmStatic
	fun zeroXYZ(thePlayer: IEntity)
	{
		zeroXZ(thePlayer)
		thePlayer.motionY = 0.0
	}
}
