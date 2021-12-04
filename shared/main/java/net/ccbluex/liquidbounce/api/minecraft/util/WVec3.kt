/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_2
import kotlin.math.floor
import kotlin.math.sqrt

class WVec3(val xCoord: Double, val yCoord: Double, val zCoord: Double)
{
	constructor(blockPos: WVec3i) : this(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble())

	operator fun plus(vec: WVec3): WVec3 = plus(vec.xCoord, vec.yCoord, vec.zCoord)

	operator fun plus(offset: Double): WVec3 = plus(offset, offset, offset)

	fun plus(x: Double, y: Double, z: Double): WVec3 = WVec3(xCoord + x, yCoord + y, zCoord + z)

	operator fun times(vec: WVec3): WVec3 = WVec3(xCoord * vec.xCoord, yCoord * vec.yCoord, zCoord * vec.zCoord)

	operator fun times(multiplier: Double): WVec3 = WVec3(xCoord * multiplier, yCoord * multiplier, zCoord * multiplier)

	fun distanceTo(vec: WVec3): Double
	{
		val deltaX: Double = vec.xCoord - xCoord
		val deltaY: Double = vec.yCoord - yCoord
		val deltaZ: Double = vec.zCoord - zCoord

		return sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
	}

	fun squareDistanceTo(vec: WVec3): Double
	{
		val deltaX = vec.xCoord - xCoord
		val deltaY = vec.yCoord - yCoord
		val deltaZ = vec.zCoord - zCoord

		return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ
	}

	fun rotatePitch(pitchRadians: Float): WVec3
	{
		val cos: Float = WMathHelper.cos(pitchRadians)
		val sin: Float = WMathHelper.sin(pitchRadians)
		val x = xCoord
		val y = yCoord * cos.toDouble() + zCoord * sin.toDouble()
		val z = zCoord * cos.toDouble() - yCoord * sin.toDouble()
		return WVec3(x, y, z)
	}

	fun rotateYaw(yawRadians: Float): WVec3
	{
		val cos: Float = WMathHelper.cos(yawRadians)
		val sin: Float = WMathHelper.sin(yawRadians)
		val x = xCoord * cos.toDouble() + zCoord * sin.toDouble()
		val y = yCoord
		val z = zCoord * cos.toDouble() - xCoord * sin.toDouble()
		return WVec3(x, y, z)
	}

	fun floor(): WVec3 = WVec3(floor(xCoord), floor(yCoord), floor(zCoord))

	override fun equals(other: Any?): Boolean
	{
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as WVec3

		if (xCoord != other.xCoord) return false
		if (yCoord != other.yCoord) return false
		if (zCoord != other.zCoord) return false

		return true
	}

	override fun hashCode(): Int
	{
		var result = xCoord.hashCode()
		result = 31 * result + yCoord.hashCode()
		result = 31 * result + zCoord.hashCode()
		return result
	}

	override fun toString(): String = "(${DECIMALFORMAT_2.format(xCoord)}, ${DECIMALFORMAT_2.format(yCoord)}, ${DECIMALFORMAT_2.format(zCoord)})"
}
