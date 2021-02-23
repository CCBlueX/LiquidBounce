/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition.WMovingObjectType
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.toRadians
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import kotlin.math.sqrt

class FallingPlayer(private val theWorld: IWorldClient, private val thePlayer: IEntityPlayerSP, private var x: Double, private var y: Double, private var z: Double, private var motionX: Double, private var motionY: Double, private var motionZ: Double, private val yaw: Float, private var strafe: Float, private var forward: Float) : MinecraftInstance()
{
	private fun calculateForTick()
	{
		strafe *= 0.98f
		forward *= 0.98f

		var v = strafe * strafe + forward * forward

		if (v >= 0.0001f)
		{
			v = sqrt(v.toDouble()).toFloat()

			if (v < 1.0f) v = 1.0f

			v = thePlayer.jumpMovementFactor / v

			strafe *= v
			forward *= v

			val yawRadians = toRadians(yaw)
			val sin = functions.sin(yawRadians)
			val cos = functions.cos(yawRadians)
			motionX += (strafe * cos - forward * sin).toDouble()
			motionZ += (forward * cos + strafe * sin).toDouble()
		}


		motionX *= 0.91

		motionY -= 0.08
		motionY *= 0.9800000190734863
		motionY *= 0.91

		motionZ *= 0.91

		x += motionX
		y += motionY
		z += motionZ
	}

	fun findCollision(ticks: Int): CollisionResult?
	{
		val start = WVec3(x, y, z)
		val end = WVec3(x, y, z)

		repeat(ticks) { i ->
			calculateForTick()

			var raytracedBlock: WBlockPos?
			val w = thePlayer.width * 0.5

			if (rayTrace(theWorld, start, end).also { raytracedBlock = it } != null) return CollisionResult(raytracedBlock!!, i)
			if (rayTrace(theWorld, start.addVector(w, 0.0, w), end).also { raytracedBlock = it } != null) return CollisionResult(raytracedBlock!!, i)
			if (rayTrace(theWorld, start.addVector(-w, 0.0, w), end).also { raytracedBlock = it } != null) return CollisionResult(raytracedBlock!!, i)
			if (rayTrace(theWorld, start.addVector(w, 0.0, -w), end).also { raytracedBlock = it } != null) return CollisionResult(raytracedBlock!!, i)
			if (rayTrace(theWorld, start.addVector(-w, 0.0, -w), end).also { raytracedBlock = it } != null) return CollisionResult(raytracedBlock!!, i)
			if (rayTrace(theWorld, start.addVector(w, 0.0, w * 0.5), end).also { raytracedBlock = it } != null) return CollisionResult(raytracedBlock!!, i)
			if (rayTrace(theWorld, start.addVector(-w, 0.0, w * 0.5), end).also { raytracedBlock = it } != null) return CollisionResult(raytracedBlock!!, i)
			if (rayTrace(theWorld, start.addVector(w * 0.5, 0.0, w), end).also { raytracedBlock = it } != null) return CollisionResult(raytracedBlock!!, i)
			if (rayTrace(theWorld, start.addVector(w * 0.5, 0.0, -w), end).also { raytracedBlock = it } != null) return CollisionResult(raytracedBlock!!, i)
		}

		return null
	}

	class CollisionResult(val pos: WBlockPos, val tick: Int)

	companion object
	{
		private fun rayTrace(theWorld: IWorldClient, start: WVec3, end: WVec3): WBlockPos?
		{
			val result = theWorld.rayTraceBlocks(start, end, true)
			return if (result != null && result.typeOfHit == WMovingObjectType.BLOCK && result.sideHit?.isUp() == true) result.blockPos else null
		}
	}
}
