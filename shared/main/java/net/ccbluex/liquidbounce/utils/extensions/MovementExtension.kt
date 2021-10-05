/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.utils.MovementUtils
import kotlin.math.hypot

val IEntityPlayerSP.isMoving: Boolean
	get() = movementInput.moveForward != 0f || movementInput.moveStrafe != 0f

val IEntity.speed: Float
	get() = hypot(motionX, motionZ).toFloat()

val IEntityLivingBase.cantBoostUp: Boolean
	get() = isInWater || isInLava || isInWeb || isOnLadder || isRiding || sneaking

val IEntity.hasMotion: Boolean
	get() = motionX != 0.0 && motionZ != 0.0 && motionY != 0.0

val IEntityPlayer.moveDirectionRadians: Float
	get() = WMathHelper.toRadians(moveDirectionDegrees)

val IEntityPlayer.moveDirectionDegrees: Float
	get() = MovementUtils.getDirectionDegrees(rotationYaw, moveForward, moveStrafing)

/**
 * @return The amplifier of Speed potion effect which applied on thePlayer (1~) (If thePlayer doesn't have Speed potion effect, it returns 0)
 */
val IEntityLivingBase.speedEffectAmplifier: Int
	get() = getEffectAmplifier(PotionType.MOVE_SPEED)

fun IEntityLivingBase.getEffectAmplifier(potionType: PotionType): Int = getActivePotionEffect(LiquidBounce.wrapper.classProvider.getPotionEnum(potionType))?.amplifier?.plus(1) ?: 0

fun IEntity.zeroXZ()
{
	motionX = 0.0
	motionZ = 0.0
}

fun IEntity.zeroXYZ()
{
	zeroXZ()
	motionY = 0.0
}

fun IEntityPlayerSP.strafe(speed: Float = this.speed, directionDegrees: Float = moveDirectionDegrees)
{
	if (!isMoving) return

	val func = LiquidBounce.wrapper.functions
	val dir = WMathHelper.toRadians(directionDegrees)
	motionX = (-func.sin(dir) * speed).toDouble()
	motionZ = (func.cos(dir) * speed).toDouble()
}

fun IEntityPlayer.boost(speed: Float = this.speed, directionDegrees: Float = moveDirectionDegrees)
{
	val func = LiquidBounce.wrapper.functions
	val dir = WMathHelper.toRadians(directionDegrees)
	motionX -= func.sin(dir) * speed
	motionZ += func.cos(dir) * speed
}

fun IEntity.forward(length: Double, directionDegrees: Float = rotationYaw)
{
	val func = LiquidBounce.wrapper.functions
	val dir = WMathHelper.toRadians(directionDegrees)
	setPosition(posX - func.sin(dir) * length, posY, posZ + func.cos(dir) * length)
}

fun IEntity.multiply(multiplier: Float) = multiply(multiplier.toDouble())

fun IEntity.multiply(multiplier: Double)
{
	motionX *= multiplier
	motionZ *= multiplier
}

fun IEntity.divide(divisor: Float) = divide(divisor.toDouble())

fun IEntity.divide(divisor: Double)
{
	motionX /= divisor
	motionZ /= divisor
}
