/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import java.util.*
import javax.vecmath.Vector3d
import kotlin.math.*

object PathUtils : MinecraftInstance()
{
	@JvmStatic
	fun findBlinkPath(thePlayer: IEntityPlayerSP, tpX: Double, tpY: Double, tpZ: Double, xzoffset: Double, yoffset: Double): Iterable<Vector3d>
	{
		var curX = thePlayer.posX
		var curY = thePlayer.posY
		var curZ = thePlayer.posZ

		var distance = getDistance(curX, curY, curZ, tpX, tpY, tpZ)

		// ArrayList is faster than all other collections (even ArrayDeque) if the total capacity is unknown
		val positions = arrayListOf<Vector3d>()

		while (distance > 0.0)
		{
			distance = getDistance(curX, curY, curZ, tpX, tpY, tpZ)

			val diffX = curX - tpX
			val diffY = curY - tpY
			val diffZ = curZ - tpZ

			val minX = min(abs(diffX), xzoffset)

			if (diffX < 0.0) curX += minX
			if (diffX > 0.0) curX -= minX

			val minY = min(abs(diffY), yoffset)

			if (diffY < 0.0) curY += minY
			if (diffY > 0.0) curY -= minY

			val minZ = min(abs(diffZ), xzoffset)

			if (diffZ < 0.0) curZ += minZ
			if (diffZ > 0.0) curZ -= minZ

			positions.add(Vector3d(curX, curY, curZ))
		}

		return positions
	}

	@JvmStatic
	fun findPath(thePlayer: IEntityPlayerSP, tpX: Double, tpY: Double, tpZ: Double, offset: Double): Iterable<Vector3d>
	{
		val steps = ceil(getDistance(thePlayer.posX, thePlayer.posY, thePlayer.posZ, tpX, tpY, tpZ) / offset)
		val stepsInt = steps.toInt()

		val dX = tpX - thePlayer.posX
		val dY = tpY - thePlayer.posY
		val dZ = tpZ - thePlayer.posZ

		// ArrayDeque is littlebit faster than ArrayList if the total capacity is known
		val positions = ArrayDeque<Vector3d>(stepsInt)

		var d = 1.0
		while (d <= steps)
		{
			positions.add(Vector3d(thePlayer.posX + dX * d / steps, thePlayer.posY + dY * d / steps, thePlayer.posZ + dZ * d / steps))
			++d
		}

		return positions
	}

	private fun getDistance(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double
	{
		val xDiff = x1 - x2
		val yDiff = y1 - y2
		val zDiff = z1 - z2

		return sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff)
	}
}
