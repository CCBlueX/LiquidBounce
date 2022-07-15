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

val ZERO: Pair<Double, Double> = (0.0 to 0.0)

val EntityPlayerSP.isMoving: Boolean
    get() = movementInput.moveForward != 0f || movementInput.moveStrafe != 0f

val Entity.speed: Double
    get() = hypot(motionX, motionZ)

@Deprecated(message = "Use speed instead; it can't, use speed.toFloat() instead", replaceWith = ReplaceWith("speed"))
val Entity.speed_f: Float
    get() = speed.toFloat()

val EntityLivingBase.cantBoostUp: Boolean
    get() = isInWater || isInLava || isInWeb || isOnLadder || isRiding || isSneaking

val Entity.hasMotion: Boolean
    get() = motionX != 0.0 && motionZ != 0.0 && motionY != 0.0

val EntityLivingBase.moveDirectionRadians: Float
    get() = moveDirectionDegrees.toRadians

val EntityLivingBase.moveDirectionDegrees: Float
    get() = MovementUtils.getDirectionDegrees(rotationYaw, moveForward, moveStrafing)

private fun getAmount(speed: Float, directionDegrees: Float): Pair<Float, Float> = getAmountRadians(speed, directionDegrees.toRadians)

private fun getAmount(speed: Double, directionDegrees: Float): Pair<Double, Double> = getAmountRadians(speed, directionDegrees.toRadians)

private fun getAmountRadians(speed: Float, directionRadians: Float): Pair<Float, Float> = -directionRadians.sin * speed to directionRadians.cos * speed

private fun getAmountRadians(speed: Double, directionRadians: Float): Pair<Double, Double> = -directionRadians.sin * speed to directionRadians.cos * speed

fun EntityLivingBase.getForwardAmount(length: Float, directionDegrees: Float = moveDirectionDegrees): Pair<Double, Double> = (posX to posZ).applyForward(length, directionDegrees)

fun EntityLivingBase.getForwardAmount(length: Double, directionDegrees: Float = moveDirectionDegrees): Pair<Double, Double> = (posX to posZ).applyForward(length, directionDegrees)

fun Pair<Double, Double>.applyForward(length: Float, directionDegrees: Float): Pair<Double, Double> = getAmount(length, directionDegrees).let { (xAmount, zAmount) -> first + xAmount to second + zAmount }

fun Pair<Double, Double>.applyForward(length: Double, directionDegrees: Float): Pair<Double, Double> = applyForwardRadians(length, directionDegrees.toRadians)

fun Pair<Double, Double>.applyForwardRadians(length: Double, directionRadians: Float): Pair<Double, Double> = getAmountRadians(length, directionRadians).let { (xAmount, zAmount) -> first + xAmount to second + zAmount }

fun Pair<Double, Double>.applyStrafe(forward: Float, strafe: Float, friction: Float, directionDegrees: Float): Pair<Double, Double>
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

        return applyStrafe(_forward, _strafe, directionDegrees)
    }

    return this
}

fun Pair<Double, Double>.applyStrafe(forward: Float, strafe: Float, directionDegrees: Float): Pair<Double, Double>
{
    val dir = directionDegrees.toRadians
    val sin = dir.sin
    val cos = dir.cos
    return first + strafe * cos - forward * sin to second + forward * cos + strafe * sin
}

fun EntityPlayer.simulateStrafe(forward: Float, strafe: Float, friction: Float, directionDegrees: Float)
{
    val (newX, newZ) = (motionX to motionZ).applyStrafe(forward, strafe, friction, directionDegrees)
    motionX = newX
    motionZ = newZ
}

fun EntityPlayer.simulateStrafe(forward: Float, strafe: Float, directionDegrees: Float)
{
    val (newX, newZ) = (motionX to motionZ).applyStrafe(forward, strafe, directionDegrees)
    motionX = newX
    motionZ = newZ
}

fun EntityLivingBase.strafe(speed: Double = this.speed, directionDegrees: Float = moveDirectionDegrees)
{
    if (this !is EntityPlayerSP || !isMoving) return

    val (xAmount, zAmount) = getAmount(speed, directionDegrees)
    motionX = xAmount
    motionZ = zAmount
}

fun EntityLivingBase.strafe(speed: Float = this.speed.toFloat(), directionDegrees: Float = moveDirectionDegrees)
{
    if (this !is EntityPlayerSP || !isMoving) return

    val (xAmount, zAmount) = getAmount(speed, directionDegrees)
    motionX = xAmount.toDouble()
    motionZ = zAmount.toDouble()
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

fun Entity.forward(length: Double, directionDegrees: Float = rotationYaw)
{
    val (xAmount, zAmount) = getAmount(length, directionDegrees)
    setPosition(posX + xAmount, posY, posZ + zAmount)
}

fun MoveEvent.forward(speed: Float, directionDegrees: Float)
{
    val (xAmount, zAmount) = getAmount(speed, directionDegrees)
    x = xAmount.toDouble()
    z = zAmount.toDouble()
}

fun MoveEvent.forward(speed: Double, directionDegrees: Float)
{
    val (xAmount, zAmount) = getAmount(speed, directionDegrees)
    x = xAmount
    z = zAmount
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
