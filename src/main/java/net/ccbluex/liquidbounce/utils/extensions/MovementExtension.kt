/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion
import kotlin.math.hypot

val EntityPlayerSP.isMoving: Boolean
    get() = movementInput.moveForward != 0f || movementInput.moveStrafe != 0f

val Entity.speed: Float
    get() = hypot(motionX, motionZ).toFloat()

val EntityLivingBase.cantBoostUp: Boolean
    get() = isInWater || isInLava || isInWeb || isOnLadder || isRiding || isSneaking

val Entity.hasMotion: Boolean
    get() = motionX != 0.0 && motionZ != 0.0 && motionY != 0.0

val EntityPlayer.moveDirectionRadians: Float
    get() = moveDirectionDegrees.toRadians

val EntityPlayer.moveDirectionDegrees: Float
    get() = MovementUtils.getDirectionDegrees(rotationYaw, moveForward, moveStrafing)

/**
 * @return The amplifier of Speed potion effect which applied on thePlayer (1~) (If thePlayer doesn't have Speed potion effect, it returns 0)
 */
val EntityLivingBase.speedEffectAmplifier: Int
    get() = getEffectAmplifier(Potion.moveSpeed)

fun EntityLivingBase.getEffectAmplifier(type: Potion): Int = getActivePotionEffect(type)?.amplifier?.plus(1) ?: 0

fun Entity.zeroXZ()
{
    motionX = 0.0
    motionZ = 0.0
}

fun Entity.zeroXYZ()
{
    zeroXZ()
    motionY = 0.0
}

fun EntityPlayerSP.strafe(speed: Float = this.speed, directionDegrees: Float = moveDirectionDegrees)
{
    if (!isMoving) return

    val dir = directionDegrees.toRadians
    motionX = (-dir.sin * speed).toDouble()
    motionZ = (dir.cos * speed).toDouble()
}

fun EntityPlayer.boost(speed: Float, directionDegrees: Float = moveDirectionDegrees)
{
    val dir = directionDegrees.toRadians
    motionX -= dir.sin * speed
    motionZ += dir.cos * speed
}

fun Entity.forward(length: Double, directionDegrees: Float = rotationYaw)
{
    val dir = directionDegrees.toRadians
    setPosition(posX - dir.sin * length, posY, posZ + dir.cos * length)
}

fun Entity.multiply(multiplier: Float) = multiply(multiplier.toDouble())

fun Entity.multiply(multiplier: Double)
{
    motionX *= multiplier
    motionZ *= multiplier
}

fun Entity.divide(divisor: Float) = divide(divisor.toDouble())

fun Entity.divide(divisor: Double)
{
    motionX /= divisor
    motionZ /= divisor
}
