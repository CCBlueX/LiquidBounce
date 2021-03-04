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
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.PI
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.cos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.sin
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.toDegrees
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.toRadians
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.wrapAngleTo180_float
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.TickEvent
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

			val targetRot = targetRotation
			if (targetRot != null && !keepCurrentRotation && (targetRot.yaw != serverRotation.yaw || targetRot.pitch != serverRotation.pitch))
			{
				packetPlayer.yaw = targetRot.yaw
				packetPlayer.pitch = targetRot.pitch
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
		// Flag constants for searchCenter() and faceBow()
		const val LOCK_CENTER = 0b1
		const val OUT_BORDER = 0b10
		const val RANDOM_CENTER = 0b100

		const val JITTER = 0b1000
		const val SKIP_VISIBLE_CHECK = 0b10000

		const val PLAYER_PREDICT = 0b100000
		const val ENEMY_PREDICT = 0b1000000

		const val SILENT_ROTATION = 0b10000000

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

		private var x = random.nextGaussian()
		private var y = random.nextGaussian()
		private var z = random.nextGaussian()

		/**
		 * Face block
		 *
		 * @param blockPos
		 * target block
		 */
		fun faceBlock(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, blockPos: WBlockPos): VecRotation?
		{
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
		 * @param silentRotation
		 * client side rotations
		 * @param enemyPrediction
		 * enemyPrediction new enemy position
		 * @param playerPrediction
		 * enemyPrediction new player position
		 */
		fun faceBow(thePlayer: IEntityPlayerSP, target: IEntity, minTurnSpeed: Float, maxTurnSpeed: Float, minSmoothingRatio: Float, maxSmoothingRatio: Float, minPlayerPredictSize: Float, maxPlayerPredictSize: Float, flags: Int)
		{
			val targetPosX = target.posX
			val targetPosY = target.posY
			val targetPosZ = target.posZ

			var distance = thePlayer.getDistanceToEntity(target).toDouble()
			distance -= distance % 0.8

			val sprinting = target.sprinting

			val playerPrediction = flags and PLAYER_PREDICT != 0
			val enemyPrediction = flags and ENEMY_PREDICT != 0

			// Calculate the (predicted) target position
			val posX = targetPosX + (if (enemyPrediction) distance / 0.8 * ((targetPosX - target.lastTickPosX) * 0.4) * if (sprinting) 1.25f else 1f else 0.0) - (thePlayer.posX + if (playerPrediction) (thePlayer.posX - thePlayer.prevPosX) * nextFloat(minPlayerPredictSize, maxPlayerPredictSize) else 0.0)
			val posY = target.entityBoundingBox.minY + (if (enemyPrediction) distance / 0.8 * ((targetPosY - target.lastTickPosY) * 0.4) else 0.0) + target.eyeHeight - 0.15 - (thePlayer.entityBoundingBox.minY + if (playerPrediction) (thePlayer.posY - thePlayer.prevPosY) * nextFloat(minPlayerPredictSize, maxPlayerPredictSize) else 0.0) - thePlayer.eyeHeight
			val posZ = targetPosZ + (if (enemyPrediction) distance / 0.8 * ((targetPosZ - target.lastTickPosZ) * 0.4) * if (sprinting) 1.25f else 1f else 0.0) - (thePlayer.posZ + if (playerPrediction) (thePlayer.posZ - thePlayer.prevPosZ) * nextFloat(minPlayerPredictSize, maxPlayerPredictSize) else 0.0)

			// Bow Power Calculation
			val fastBow = LiquidBounce.moduleManager[FastBow::class.java] as FastBow
			var velocity = (if (fastBow.state) fastBow.packetsValue.get() else thePlayer.itemInUseDuration) * 0.05f
			velocity = (velocity * velocity + velocity * 2) / 3
			if (velocity > 1) velocity = 1f

			// Calculate Rotation
			val posSqrt = StrictMath.hypot(posX, posZ)
			val rotation = Rotation(toDegrees(StrictMath.atan2(posZ, posX).toFloat()) - 90, -toDegrees(StrictMath.atan((velocity * velocity - sqrt(velocity * velocity * velocity * velocity - 0.006f * (0.006f * (posSqrt * posSqrt) + 2 * posY * (velocity * velocity)))) / (0.006f * posSqrt)).toFloat()))
			val limitedRotation = limitAngleChange(Rotation(thePlayer.rotationYaw, thePlayer.rotationPitch), rotation, nextFloat(min(minTurnSpeed, maxTurnSpeed), max(minTurnSpeed, maxTurnSpeed)), nextFloat(min(minSmoothingRatio, maxSmoothingRatio), max(minSmoothingRatio, maxSmoothingRatio)))

			// Apply Rotation
			if (flags and SILENT_ROTATION != 0)
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
		 * predict new location of your body (based on position delta, not motion)
		 * @param minPlayerPredictSize
		 * minimum predict size of your body
		 * @param maxPlayerPredictSize
		 * maximum predict size of your body
		 * @return               rotation
		 */
		private fun toRotation(thePlayer: IEntityPlayerSP, vec: WVec3, minPlayerPredictSize: Float, maxPlayerPredictSize: Float, playerPredict: Boolean): Rotation
		{
			val posX = thePlayer.posX
			val posZ = thePlayer.posZ
			var eyesPos = WVec3(posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, posZ)

			if (playerPredict)
			{
				val xPredict = (posX - thePlayer.prevPosX) * nextFloat(minPlayerPredictSize, maxPlayerPredictSize)
				val yPredict = (thePlayer.posY - thePlayer.prevPosY) * nextFloat(minPlayerPredictSize, maxPlayerPredictSize)
				val zPredict = (posZ - thePlayer.prevPosZ) * nextFloat(minPlayerPredictSize, maxPlayerPredictSize)

				eyesPos = eyesPos.addVector(xPredict, yPredict, zPredict)
			}

			// if (playerPredict) eyesPos.addVector(thePlayer.motionX, thePlayer.motionY, thePlayer.motionZ)

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
		 * @param  targetBox
		 * enemy box
		 * @param  mode
		 * search center mode
		 * @param  jitter
		 * jitter option
		 * @param  jitterData
		 * jitter data option (minyawspeed, minpitchspeed etc.)
		 * @param  playerPredict
		 * predict option
		 * @param  shouldAttackThroughWalls
		 * throughWalls option
		 * @param  distance
		 * vec3 distance limit
		 * @param  hitboxDecrement
		 * decrement of the entity hitbox size. default is 0.2D
		 * @param  searchSensitivity
		 * count of step to search the good center. (*Warning If you set this value too low, it will make your minecraft SO SLOW AND SLOW.*) default is 0.2D
		 * @return                   center
		 */
		fun searchCenter(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, targetBox: IAxisAlignedBB, flags: Int, jitterData: JitterData, minPlayerPredict: Float, maxPlayerPredict: Float, distance: Float, hitboxDecrement: Double, searchSensitivity: Double): VecRotation?
		{
			val randomVec: WVec3
			val eyes = thePlayer.getPositionEyes(1.0f)

			// Target box
			val minX = targetBox.minX
			val maxX = targetBox.maxX
			val minY = targetBox.minY
			val maxY = targetBox.maxY
			val minZ = targetBox.minZ
			val maxZ = targetBox.maxZ

			val playerPredict = flags and PLAYER_PREDICT != 0

			when
			{
				flags and LOCK_CENTER != 0 ->
				{
					randomVec = getCenter(targetBox)
					return VecRotation(randomVec, toRotation(thePlayer, randomVec, minPlayerPredict, maxPlayerPredict, playerPredict))
				}

				flags and OUT_BORDER != 0 ->
				{
					randomVec = WVec3(minX + (maxX - minX) * (x * 0.3 + 1.0), minY + (maxY - minY) * (y * 0.3 + 1.0), minZ + (maxZ - minZ) * (z * 0.3 + 1.0))
					return VecRotation(randomVec, toRotation(thePlayer, randomVec, minPlayerPredict, maxPlayerPredict, playerPredict))
				}

				else ->
				{
				}
			}

			randomVec = WVec3(minX + (maxX - minX) * x * 0.8, minY + (maxY - minY) * y * 0.8, minZ + (maxZ - minZ) * z * 0.8)

			val randomRotation = toRotation(thePlayer, randomVec, minPlayerPredict, maxPlayerPredict, playerPredict)
			var yawJitterAmount = 0f
			var pitchJitterAmount = 0f

			val jitter = flags and JITTER != 0

			// Calculate jitter amount
			if (jitter)
			{
				val yawJitter = jitterData.yawRate > 0 && random.nextInt(100) <= jitterData.yawRate
				val pitchJitter = jitterData.pitchRate > 0 && random.nextInt(100) <= jitterData.pitchRate

				if (yawJitter)
				{
					val yawNegative = random.nextBoolean()

					val minYaw = jitterData.minYaw
					val maxYaw = jitterData.maxYaw

					yawJitterAmount = if (yawNegative) -nextFloat(minYaw, maxYaw) else nextFloat(minYaw, maxYaw)
				}

				if (pitchJitter)
				{
					val pitchNegative = random.nextBoolean()

					val minPitch = jitterData.minPitch
					val maxPitch = jitterData.maxPitch

					pitchJitterAmount = if (pitchNegative) -nextFloat(minPitch, maxPitch) else nextFloat(minPitch, maxPitch)
				}
			}

			// Search boundingbox center
			var vecRotation: VecRotation? = null

			val fixedHitboxDecrement = hitboxDecrement.coerceAtLeast(0.0).coerceAtMost(1.0) // Last Fail-safe

			val skipVisibleCheck = flags and SKIP_VISIBLE_CHECK != 0
			val randomCenter = flags and RANDOM_CENTER != 0

			var xSearch = fixedHitboxDecrement
			while (xSearch < 1 - fixedHitboxDecrement)
			{
				var ySearch = fixedHitboxDecrement
				while (ySearch < 1 - fixedHitboxDecrement)
				{
					var zSearch = fixedHitboxDecrement
					while (zSearch < 1 - fixedHitboxDecrement)
					{
						val vec3 = WVec3(minX + (maxX - minX) * xSearch, minY + (maxY - minY) * ySearch, minZ + (maxZ - minZ) * zSearch)
						val vecDist = eyes.distanceTo(vec3)

						if (vecDist > distance)
						{
							zSearch += searchSensitivity
							continue
						}

						if (skipVisibleCheck || isVisible(theWorld, thePlayer, vec3))
						{
							val rotation = toRotation(thePlayer, vec3, minPlayerPredict, maxPlayerPredict, playerPredict)
							val currentVec = VecRotation(vec3, rotation)

							if (vecRotation == null || (if (randomCenter) getRotationDifference(currentVec.rotation, randomRotation) < getRotationDifference(vecRotation.rotation, randomRotation) else getRotationDifference(currentVec.rotation) < getRotationDifference(vecRotation.rotation))) vecRotation = currentVec
						}
						zSearch += searchSensitivity
					}
					ySearch += searchSensitivity
				}
				xSearch += searchSensitivity
			}

			// Apply Jitter
			if (vecRotation != null && jitter)
			{
				vecRotation.rotation.yaw = vecRotation.rotation.yaw + yawJitterAmount

				// Enforce pitch to 90 ~ -90
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
		fun getClientRotationDifference(thePlayer: IEntityPlayerSP, entity: IEntity, playerPredict: Boolean, minPlayerPredictSize: Float, maxPlayerPredictSize: Float): Double
		{
			val rotation = toRotation(thePlayer, getCenter(entity.entityBoundingBox), minPlayerPredictSize, maxPlayerPredictSize, playerPredict)
			return getRotationDifference(rotation, Rotation(thePlayer.rotationYaw, thePlayer.rotationPitch))
		}

		/**
		 * Calculate difference between the "server-sided rotation" and your entity
		 *
		 * @param  entity
		 * your entity
		 * @return        difference between rotation
		 */
		fun getServerRotationDifference(thePlayer: IEntityPlayerSP, entity: IEntity, playerPredict: Boolean, minPlayerPredictSize: Float, maxPlayerPredictSize: Float): Double
		{
			val rotation = toRotation(thePlayer, getCenter(entity.entityBoundingBox), minPlayerPredictSize, maxPlayerPredictSize, playerPredict)
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
		fun getRotationDifference(rotation: Rotation): Double = getRotationDifference(rotation, serverRotation)

		/**
		 * Calculate difference between two rotations
		 *
		 * @param  first
		 * rotation
		 * @param  second
		 * rotation
		 * @return        difference between rotation
		 */
		private fun getRotationDifference(first: Rotation, second: Rotation): Double = StrictMath.hypot(getAngleDifference(first.yaw, second.yaw).toDouble(), (first.pitch - second.pitch).toDouble())

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
		fun limitAngleChange(currentRotation: Rotation, targetRotation: Rotation, turnSpeed: Float, acceleration: Float): Rotation
		{
			var yawDelta = getAngleDifference(targetRotation.yaw, currentRotation.yaw)
			var pitchDelta = getAngleDifference(targetRotation.pitch, currentRotation.pitch)
			val accel = acceleration.coerceAtMost(1.0f)

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

			val yawCos = cos(-yawRadians - PI)
			val yawSin = sin(-yawRadians - PI)

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
		fun isFaced(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, targetEntity: IEntity?, blockReachDistance: Double): Boolean = raycastEntity(theWorld, thePlayer, blockReachDistance, object : EntityFilter
		{
			override fun canRaycast(entity: IEntity?): Boolean = targetEntity != null && targetEntity == entity
		}) != null

		/**
		 * Allows you to check if your enemy is behind a wall
		 */
		private fun isVisible(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, vec3: WVec3): Boolean
		{
			val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)

			return theWorld.rayTraceBlocks(eyesPos, vec3) == null
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

			val targetRotation = targetRotation ?: return

			if (minResetTurnSpeed >= 180 || getAngleDifference(targetRotation, goalRotation) <= minResetTurnSpeed)
			{
				this.targetRotation = null

				// Reset the resetTurnSpeed
				minResetTurnSpeed = 180.0f
				maxResetTurnSpeed = 180.0f
			}
			else this.targetRotation = limitAngleChange(targetRotation, goalRotation, if (maxResetTurnSpeed - minResetTurnSpeed > 0) nextFloat(minResetTurnSpeed, maxResetTurnSpeed) else maxResetTurnSpeed, 0f).apply { fixedSensitivity(mc.gameSettings.mouseSensitivity) }
		}
	}
}
