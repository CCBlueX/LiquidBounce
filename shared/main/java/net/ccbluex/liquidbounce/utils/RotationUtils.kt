/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition.WMovingObjectType
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.cos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.sin
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.toDegrees
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.toRadians
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.wrapAngleTo180_float
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.combat.FastBow
import net.ccbluex.liquidbounce.utils.RaycastUtils.EntityFilter
import net.ccbluex.liquidbounce.utils.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import java.lang.Double.isNaN
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class RotationUtils : MinecraftInstance(), Listenable
{
	enum class SearchCenterMode
	{
		SEARCH_GOOD_CENTER, LOCK_CENTER, OUT_BORDER, RANDOM_GOOD_CENTER
	}

	data class JitterData(val yawRate: Int, val pitchRate: Int, val minYaw: Float, val maxYaw: Float, val minPitch: Float, val maxPitch: Float)

	/**
	 * Handle minecraft tick
	 *
	 * @param event
	 * Tick event
	 */
	@EventTarget
	fun onTick(@Suppress("UNUSED_PARAMETER") event: TickEvent?)
	{
		if (targetRotation != null)
		{
			keepLength--
			if (keepLength <= 0) reset()
		}

		if (random.nextGaussian() > 0.8) x = Math.random()
		if (random.nextGaussian() > 0.8) y = Math.random()
		if (random.nextGaussian() > 0.8) z = Math.random()
	}

	/**
	 * Handle packet
	 *
	 * @param event
	 * Packet Event
	 */
	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (classProvider.isCPacketPlayer(packet))
		{
			val packetPlayer = packet.asCPacketPlayer()
			if (targetRotation != null && !keepCurrentRotation && (targetRotation!!.yaw != serverRotation.yaw || targetRotation!!.pitch != serverRotation.pitch))
			{
				packetPlayer.yaw = targetRotation!!.yaw
				packetPlayer.pitch = targetRotation!!.pitch
				packetPlayer.rotating = true
			}

			lastServerRotation = Rotation(serverRotation.yaw, serverRotation.pitch)

			if (packetPlayer.rotating) serverRotation = Rotation(packetPlayer.yaw, packetPlayer.pitch)
		}
	}

	/**
	 * @return YESSSS!!!
	 */
	override fun handleEvents(): Boolean = true

	companion object
	{
		private val random = Random()
		private var keepLength = 0
		private var minResetTurnSpeed = 180.0f
		private var maxResetTurnSpeed = 180.0f

		@JvmField
		var targetRotation: Rotation? = null

		@JvmField
		var serverRotation: Rotation = Rotation(0.0f, 0.0f)

		@JvmField
		var lastServerRotation = Rotation(0.0f, 0.0f)

		var keepCurrentRotation = false

		private var x = random.nextDouble()
		private var y = random.nextDouble()
		private var z = random.nextDouble()

		/**
		 * Face block
		 *
		 * @param blockPos
		 * target block
		 */
		fun faceBlock(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, blockPos: WBlockPos?): VecRotation?
		{
			if (blockPos == null) return null
			var vecRotation: VecRotation? = null
			val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)
			var xSearch = 0.1
			while (xSearch < 0.9)
			{
				var ySearch = 0.1
				while (ySearch < 0.9)
				{
					var zSearch = 0.1
					while (zSearch < 0.9)
					{
						val posVec = WVec3(blockPos).addVector(xSearch, ySearch, zSearch)
						val dist = eyesPos.distanceTo(posVec)
						val diffX = posVec.xCoord - eyesPos.xCoord
						val diffY = posVec.yCoord - eyesPos.yCoord
						val diffZ = posVec.zCoord - eyesPos.zCoord
						val diffXZ = StrictMath.hypot(diffX, diffZ)
						val rotation = Rotation(wrapAngleTo180_float(toDegrees(StrictMath.atan2(diffZ, diffX).toFloat()) - 90.0f), wrapAngleTo180_float(-toDegrees(StrictMath.atan2(diffY, diffXZ).toFloat())))
						val rotationVector = getVectorForRotation(rotation)
						val vector = eyesPos.addVector(rotationVector.xCoord * dist, rotationVector.yCoord * dist, rotationVector.zCoord * dist)
						val obj = theWorld.rayTraceBlocks(eyesPos, vector, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = false, returnLastUncollidableBlock = true)
						if (obj != null && obj.typeOfHit == WMovingObjectType.BLOCK)
						{
							val currentVec = VecRotation(posVec, rotation)
							if (vecRotation == null || getRotationDifference(currentVec.rotation) < getRotationDifference(vecRotation.rotation)) vecRotation = currentVec
						}
						zSearch += 0.1
					}
					ySearch += 0.1
				}
				xSearch += 0.1
			}
			return vecRotation
		}

		/**
		 * Face target with bow
		 *
		 * @param target
		 * your enemy
		 * @param silent
		 * client side rotations
		 * @param enemyPrediction
		 * enemyPrediction new enemy position
		 * @param playerPrediction
		 * enemyPrediction new player position
		 */
		fun faceBow(thePlayer: IEntityPlayerSP, target: IEntity, silent: Boolean, enemyPrediction: Boolean, playerPrediction: Boolean, minTurnSpeed: Float, maxTurnSpeed: Float, minSmoothingRatio: Float, maxSmoothingRatio: Float)
		{
			// Prediction
			val xDelta = (target.posX - target.lastTickPosX) * 0.4
			val yDelta = (target.posY - target.lastTickPosY) * 0.4
			val zDelta = (target.posZ - target.lastTickPosZ) * 0.4

			var distance = thePlayer.getDistanceToEntity(target).toDouble()
			distance -= distance % 0.8

			val sprinting = target.sprinting
			val xPrediction = distance / 0.8 * xDelta * if (sprinting) 1.25f else 1f
			val yPrediction = distance / 0.8 * yDelta
			val zPrediction = distance / 0.8 * zDelta * if (sprinting) 1.25f else 1f

			// Calculate the (predicted) target position
			val posX = target.posX + (if (enemyPrediction) xPrediction else 0.0) - (thePlayer.posX + if (playerPrediction) thePlayer.posX - thePlayer.prevPosX else 0.0)
			val posY = target.entityBoundingBox.minY + (if (enemyPrediction) yPrediction else 0.0) + target.eyeHeight - 0.15 - (thePlayer.entityBoundingBox.minY + if (enemyPrediction) thePlayer.posY - thePlayer.prevPosY else 0.0) - thePlayer.eyeHeight
			val posZ = target.posZ + (if (enemyPrediction) zPrediction else 0.0) - (thePlayer.posZ + if (playerPrediction) thePlayer.posZ - thePlayer.prevPosZ else 0.0)

			// Bow Power Calculation
			val fastBow = LiquidBounce.moduleManager[FastBow::class.java] as FastBow
			var velocity = (if (fastBow.state) fastBow.packetsValue.get() else thePlayer.itemInUseDuration) / 20.0f
			velocity = (velocity * velocity + velocity * 2) / 3
			if (velocity > 1) velocity = 1f

			// Calculate Rotation
			val posSqrt = StrictMath.hypot(posX, posZ)
			val rotation = Rotation(toDegrees(StrictMath.atan2(posZ, posX).toFloat()) - 90, -toDegrees(StrictMath.atan((velocity * velocity - sqrt(velocity * velocity * velocity * velocity - 0.006f * (0.006f * (posSqrt * posSqrt) + 2 * posY * (velocity * velocity)))) / (0.006f * posSqrt)).toFloat()))
			val limitedRotation = limitAngleChange(Rotation(thePlayer.rotationYaw, thePlayer.rotationPitch), rotation, nextFloat(min(minTurnSpeed, maxTurnSpeed), max(minTurnSpeed, maxTurnSpeed)), nextFloat(min(minSmoothingRatio, maxSmoothingRatio), max(minSmoothingRatio, maxSmoothingRatio)))

			// Apply Rotation
			if (silent)
			{
				setTargetRotation(limitedRotation)
				setNextResetTurnSpeed(minTurnSpeed, maxTurnSpeed)
			}
			else limitedRotation.applyRotationToPlayer(thePlayer)
		}

		/**
		 * Translate vec to rotation
		 *
		 * @param  vec
		 * target vec
		 * @param  playerPredict
		 * predict new location of your body
		 * @return               rotation
		 */
		private fun toRotation(thePlayer: IEntityPlayerSP, vec: WVec3, playerPredict: Boolean): Rotation
		{
			val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)
			if (playerPredict) eyesPos.addVector(thePlayer.motionX, thePlayer.motionY, thePlayer.motionZ)
			val diffX = vec.xCoord - eyesPos.xCoord
			val diffY = vec.yCoord - eyesPos.yCoord
			val diffZ = vec.zCoord - eyesPos.zCoord
			return Rotation(wrapAngleTo180_float(toDegrees(StrictMath.atan2(diffZ, diffX).toFloat()) - 90.0f), wrapAngleTo180_float(-toDegrees(StrictMath.atan2(diffY, StrictMath.hypot(diffX, diffZ)).toFloat())))
		}

		/**
		 * Get the center of a box
		 *
		 * @param  box
		 * your box
		 * @return     center of box
		 */
		private fun getCenter(box: IAxisAlignedBB): WVec3 = WVec3(box.minX + (box.maxX - box.minX) * 0.5, box.minY + (box.maxY - box.minY) * 0.5, box.minZ + (box.maxZ - box.minZ) * 0.5)

		/**
		 * Search good center
		 *
		 * @param  box
		 * enemy box
		 * @param  mode
		 * search center mode
		 * @param  jitter
		 * jitter option
		 * @param  jitterData
		 * jitter data option (minyawspeed, minpitchspeed etc.)
		 * @param  playerPrediction
		 * predict option
		 * @param  throughWalls
		 * throughWalls option
		 * @param  distance
		 * vec3 distance limit
		 * @param  hitboxDecrement
		 * decrement of the entity hitbox size. default is 0.2D
		 * @param  searchSensitivity
		 * count of step to search the good center. (*Warning If you set this value too low, it will make your minecraft SO SLOW AND SLOW.*) default is 0.2D
		 * @return                   center
		 */
		fun searchCenter(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, box: IAxisAlignedBB, mode: SearchCenterMode, jitter: Boolean, jitterData: JitterData, playerPrediction: Boolean, throughWalls: Boolean, distance: Float, hitboxDecrement: Double, searchSensitivity: Double): VecRotation?
		{
			val randomVec: WVec3
			val eyes = thePlayer.getPositionEyes(1.0f)
			when (mode)
			{
				SearchCenterMode.LOCK_CENTER ->
				{
					randomVec = getCenter(box)
					return VecRotation(randomVec, toRotation(thePlayer, randomVec, playerPrediction))
				}

				SearchCenterMode.OUT_BORDER ->
				{
					randomVec = WVec3(box.minX + (box.maxX - box.minX) * (x * 0.3 + 1.0), box.minY + (box.maxY - box.minY) * (y * 0.3 + 1.0), box.minZ + (box.maxZ - box.minZ) * (z * 0.3 + 1.0))
					return VecRotation(randomVec, toRotation(thePlayer, randomVec, playerPrediction))
				}

				else ->
				{
				}
			}

			randomVec = WVec3(box.minX + (box.maxX - box.minX) * x * 0.8, box.minY + (box.maxY - box.minY) * y * 0.8, box.minZ + (box.maxZ - box.minZ) * z * 0.8)

			val randomRotation = toRotation(thePlayer, randomVec, playerPrediction)
			var yawJitterAmount = 0f
			var pitchJitterAmount = 0f

			// Calculate jitter amount
			if (jitter)
			{
				val yawJitter = jitterData.yawRate > 0 && Random().nextInt(100) <= jitterData.yawRate
				val pitchJitter = jitterData.pitchRate > 0 && Random().nextInt(100) <= jitterData.pitchRate
				val yawNegative = Random().nextBoolean()
				val pitchNegative = Random().nextBoolean()

				if (yawJitter) yawJitterAmount = if (yawNegative) -nextFloat(jitterData.minYaw, jitterData.maxYaw) else nextFloat(jitterData.minYaw, jitterData.maxYaw)
				if (pitchJitter) pitchJitterAmount = if (pitchNegative) -nextFloat(jitterData.minPitch, jitterData.maxPitch) else nextFloat(jitterData.minPitch, jitterData.maxPitch)
			}

			// Search boundingbox center
			var vecRotation: VecRotation? = null
			var xSearch = hitboxDecrement

			while (xSearch < 1 - hitboxDecrement)
			{
				var ySearch = hitboxDecrement

				while (ySearch < 1 - hitboxDecrement)
				{
					var zSearch = hitboxDecrement

					while (zSearch < 1 - hitboxDecrement)
					{
						val vec3 = WVec3(box.minX + (box.maxX - box.minX) * xSearch, box.minY + (box.maxY - box.minY) * ySearch, box.minZ + (box.maxZ - box.minZ) * zSearch)
						val vecDist = eyes.distanceTo(vec3)

						if (vecDist > distance)
						{
							zSearch += searchSensitivity
							continue
						}

						if (throughWalls || isVisible(theWorld, thePlayer, vec3))
						{
							val rotation = toRotation(thePlayer, vec3, playerPrediction)
							val currentVec = VecRotation(vec3, rotation)
							if (vecRotation == null || (if (mode == SearchCenterMode.RANDOM_GOOD_CENTER) getRotationDifference(currentVec.rotation, randomRotation) < getRotationDifference(vecRotation.rotation, randomRotation) else getRotationDifference(currentVec.rotation) < getRotationDifference(vecRotation.rotation))) vecRotation = currentVec
						}

						zSearch += searchSensitivity
					}

					ySearch += searchSensitivity
				}

				xSearch += searchSensitivity
			}

			// Jitter
			if (vecRotation != null && jitter)
			{
				vecRotation.rotation.yaw = vecRotation.rotation.yaw + yawJitterAmount
				var pitch = vecRotation.rotation.pitch + pitchJitterAmount
				if (pitch > 90) pitch = 90f else if (pitch < -90) pitch = -90f
				vecRotation.rotation.pitch = pitch
			}
			return vecRotation
		}

		/**
		 * Calculate difference between the "client-sided rotation" and your entity
		 *
		 * @param  entity
		 * your entity
		 * @return        difference between rotation
		 */
		fun getClientRotationDifference(thePlayer: IEntityPlayerSP, entity: IEntity): Double
		{
			val rotation = toRotation(thePlayer, getCenter(entity.entityBoundingBox), true)
			return getRotationDifference(rotation, Rotation(thePlayer.rotationYaw, thePlayer.rotationPitch))
		}

		/**
		 * Calculate difference between the "server-sided rotation" and your entity
		 *
		 * @param  entity
		 * your entity
		 * @return        difference between rotation
		 */
		fun getServerRotationDifference(thePlayer: IEntityPlayerSP, entity: IEntity): Double
		{
			val rotation = toRotation(thePlayer, getCenter(entity.entityBoundingBox), true)
			return getRotationDifference(rotation, serverRotation)
		}

		// /**
		//  * Calculate difference between the client rotation and your entity
		//  *
		//  * @param  entity
		//  * your entity
		//  * @return        difference between rotation
		//  */
		// fun getRotationDifference(thePlayer: IEntityPlayerSP, entity: IEntity): Double
		// {
		// 	val rotation = toRotation(thePlayer, getCenter(entity.entityBoundingBox), true)
		// 	return getRotationDifference(rotation, Rotation(mc.thePlayer!!.rotationYaw, mc.thePlayer!!.rotationPitch))
		// }

		/**
		 * Calculate difference between the server rotation and your rotation
		 *
		 * @param  rotation
		 * your rotation
		 * @return          difference between rotation
		 */
		fun getRotationDifference(rotation: Rotation): Double = Optional.ofNullable(serverRotation).map { serverRotation1: Rotation? -> getRotationDifference(rotation, serverRotation1) }.orElse(0.0)

		/**
		 * Calculate difference between two rotations
		 *
		 * @param  first
		 * rotation
		 * @param  second
		 * rotation
		 * @return        difference between rotation
		 */
		fun getRotationDifference(first: Rotation, second: Rotation?): Double = StrictMath.hypot(getAngleDifference(first.yaw, second!!.yaw).toDouble(), (first.pitch - second.pitch).toDouble())

		/**
		 * Limit your rotation using a turn speed
		 *
		 * @param  currentRotation
		 * your current rotation
		 * @param  targetRotation
		 * your goal rotation
		 * @param  turnSpeed
		 * your turn speed
		 * @return                 limited rotation
		 */
		fun limitAngleChange(currentRotation: Rotation?, targetRotation: Rotation, turnSpeed: Float, acceleration: Float): Rotation
		{
			var yawDelta = getAngleDifference(targetRotation.yaw, currentRotation!!.yaw)
			var pitchDelta = getAngleDifference(targetRotation.pitch, currentRotation.pitch)
			val accel = min(acceleration, 1.0f)

			yawDelta -= yawDelta * accel
			pitchDelta -= pitchDelta * accel

			return Rotation(currentRotation.yaw + if (yawDelta > turnSpeed) turnSpeed else max(yawDelta, -turnSpeed), currentRotation.pitch + if (pitchDelta > turnSpeed) turnSpeed else max(pitchDelta, -turnSpeed))
		}

		private fun getAngleDifference(rot1: Rotation, rot2: Rotation): Double = StrictMath.hypot(getAngleDifference(rot1.yaw, rot2.yaw).toDouble(), getAngleDifference(rot1.pitch, rot2.pitch).toDouble())

		/**
		 * Calculate difference between two angle points
		 *
		 * @param  angle
		 * angle point
		 * @param  otherAngle
		 * angle point
		 * @return   difference between angle points
		 */
		private fun getAngleDifference(angle: Float, otherAngle: Float): Float = ((angle - otherAngle) % 360.0f + 540.0f) % 360.0f - 180.0f

		/**
		 * Calculate rotation to vector
		 *
		 * @param  rotation
		 * your rotation
		 * @return          target vector
		 */
		fun getVectorForRotation(rotation: Rotation): WVec3
		{
			val yawRadians = toRadians(rotation.yaw)
			val pitchRadians = toRadians(rotation.pitch)

			val yawCos = cos(-yawRadians - Math.PI.toFloat())
			val yawSin = sin(-yawRadians - Math.PI.toFloat())

			val pitchCos = -cos(-pitchRadians)
			val pitchSin = sin(-pitchRadians)

			return WVec3((yawSin * pitchCos).toDouble(), pitchSin.toDouble(), (yawCos * pitchCos).toDouble())
		}

		/**
		 * Allows you to check if your crosshair is over your target entity
		 *
		 * @param  targetEntity
		 * your target entity
		 * @param  blockReachDistance
		 * your reach
		 * @return                    if crosshair is over target
		 */
		fun isFaced(targetEntity: IEntity?, blockReachDistance: Double): Boolean = raycastEntity(blockReachDistance, object : EntityFilter
		{
			override fun canRaycast(entity: IEntity?): Boolean = targetEntity != null && targetEntity == entity
		}) != null

		/**
		 * Allows you to check if your enemy is behind a wall
		 */
		private fun isVisible(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, vec3: WVec3?): Boolean
		{
			val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)

			return theWorld.rayTraceBlocks(eyesPos, vec3!!) == null
		}

		/**
		 * Set your target rotation
		 *
		 * @param rotation
		 * your target rotation
		 */
		fun setTargetRotation(rotation: Rotation?, keepLength: Int)
		{
			if (rotation != null && (isNaN(rotation.yaw.toDouble()) || isNaN(rotation.pitch.toDouble()) || rotation.pitch > 90 || rotation.pitch < -90)) return
			rotation?.fixedSensitivity(mc.gameSettings.mouseSensitivity)
			targetRotation = rotation
			Companion.keepLength = keepLength
		}

		fun setNextResetTurnSpeed(min: Float, max: Float)
		{
			minResetTurnSpeed = min
			maxResetTurnSpeed = max
		}

		/**
		 * Set your target rotation
		 *
		 * @param rotation
		 * your target rotation
		 */
		fun setTargetRotation(rotation: Rotation?)
		{
			setTargetRotation(rotation, 0)
		}

		/**
		 * Reset your target rotation
		 */
		fun reset()
		{
			keepLength = 0

			val thePlayer = mc.thePlayer ?: return

			val goalRotation = Rotation(thePlayer.rotationYaw, thePlayer.rotationPitch)

			if (minResetTurnSpeed >= 180 || getAngleDifference(targetRotation ?: return, goalRotation) <= minResetTurnSpeed)
			{
				targetRotation = null

				// Reset the resetTurnSpeed
				minResetTurnSpeed = 180.0f
				maxResetTurnSpeed = 180.0f
			}
			else
			{
				val limited = limitAngleChange(targetRotation, goalRotation, if (maxResetTurnSpeed - minResetTurnSpeed > 0) nextFloat(minResetTurnSpeed, maxResetTurnSpeed) else maxResetTurnSpeed, 0f)
				limited.fixedSensitivity(mc.gameSettings.mouseSensitivity)
				targetRotation = limited
			}
		}
	}
}
