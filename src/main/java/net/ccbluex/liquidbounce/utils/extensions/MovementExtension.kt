/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import kotlin.math.hypot
import kotlin.math.sqrt

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

    val (xAmount, zAmount) = getAmount(speed, directionDegrees)
    motionX = xAmount.toDouble()
    motionZ = zAmount.toDouble()
}

fun MoveEvent.strafe(speed: Float, directionDegrees: Float)
{
    val (xAmount, zAmount) = getAmount(speed, directionDegrees)
    x = xAmount.toDouble()
    z = zAmount.toDouble()
}

private fun getAmount(speed: Float, directionDegrees: Float): Pair<Float, Float>
{
    val dir = directionDegrees.toRadians
    return -dir.sin * speed to dir.cos * speed
}

private fun getAmount(speed: Double, directionDegrees: Float): Pair<Double, Double>
{
    val dir = directionDegrees.toRadians
    return -dir.sin * speed to dir.cos * speed
}

fun EntityPlayer.boost(speed: Float, directionDegrees: Float = moveDirectionDegrees)
{
    val (xAmount, zAmount) = getAmount(speed, directionDegrees)
    motionX += xAmount
    motionZ += zAmount
}

fun MoveEvent.boost(speed: Float, directionDegrees: Float)
{
    val (xAmount, zAmount) = getAmount(speed, directionDegrees)
    x += xAmount
    z += zAmount
}

fun EntityPlayer.simulateStrafe(forward: Float, strafe: Float, friction: Float, directionDegrees: Float)
{
    var _strafe = strafe
    var _forward = forward
    var f = _strafe * _strafe + _forward * _forward

    if (f >= 1.0E-4F)
    {
        f = sqrt(f)

        if (f < 1.0F) f = 1.0F

        f = friction / f
        _strafe *= f
        _forward *= f

        val _dir = directionDegrees.toRadians
        val sin = _dir.sin
        val cos = _dir.cos
        motionX += _strafe * cos - _forward * sin
        motionZ += _forward * cos + _strafe * sin
    }
}

fun Entity.forward(length: Double, directionDegrees: Float = rotationYaw)
{
    val (xAmount, zAmount) = getAmount(length, directionDegrees)
    setPosition(posX + xAmount, posY, posZ + zAmount)
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
