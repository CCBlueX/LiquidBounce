/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
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
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.TickEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.FastBow
import net.ccbluex.liquidbounce.utils.extensions.raycastEntity
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import java.lang.Double.isNaN
import java.util.*
import kotlin.math.*

class RotationUtils : MinecraftInstance(), Listenable
{
	class JitterData(val yawRate: Int, val pitchRate: Int, yaw1: Float, yaw2: Float, pitch1: Float, pitch2: Float)
	{
		val minYaw = min(yaw1, yaw2)
		val maxYaw = max(yaw1, yaw2)

		val minPitch = min(pitch1, pitch2)
		val maxPitch = max(pitch1, pitch2)
	}

	class MinMaxPair(first: Float, second: Float)
	{
		val min: Float = min(first, second)
		val max: Float = max(first, second)

		companion object
		{
			val ZERO = MinMaxPair(0F, 0F)
		}
	}

	/**
	 * Handle minecraft tick
	 *
	 * @param event
	 * Tick event
	 */
	@EventTarget
	fun onTick(@Suppress("UNUSED_PARAMETER") event: TickEvent)
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

			if (serverRotation !== lastServerRotation) lastServerRotation = serverRotation
			if (packetPlayer.rotating) serverRotation = Rotation(packetPlayer.yaw, packetPlayer.pitch)
		}
	}

	/**
	 * @return YESSSS!!!
	 */
	override fun handleEvents(): Boolean = true

	companion object
	{
		private val DEFAULT_ROTATION = Rotation(0F, 0F)

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
		var serverRotation = DEFAULT_ROTATION.copy()

		@JvmField
		var lastServerRotation = DEFAULT_ROTATION.copy()

		val clientRotation: Rotation
			get() = mc.thePlayer?.let { Rotation(it.rotationYaw, it.rotationPitch) } ?: DEFAULT_ROTATION.copy()

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
		fun faceBlock(theWorld: IWorld, thePlayer: IEntity, blockPos: WBlockPos): VecRotation?
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
						val posVec = WVec3(blockPos).plus(xSearch, ySearch, zSearch)
						val dist = eyesPos.distanceTo(posVec)
						val diffX = posVec.xCoord - eyesPos.xCoord
						val diffY = posVec.yCoord - eyesPos.yCoord
						val diffZ = posVec.zCoord - eyesPos.zCoord
						val diffXZ = hypot(diffX, diffZ)
						val rotation = Rotation(wrapAngleTo180_float(toDegrees(atan2(diffZ, diffX).toFloat()) - 90.0f), wrapAngleTo180_float(-toDegrees(atan2(diffY, diffXZ).toFloat())))
						val rotationVector = getVectorForRotation(rotation)
						val vector = eyesPos + rotationVector * dist
						val obj = theWorld.rayTraceBlocks(eyesPos, vector, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = false, returnLastUncollidableBlock = true)
						if (obj != null && obj.typeOfHit == WMovingObjectType.BLOCK)
						{
							val currentVec = VecRotation(posVec, rotation, obj.sideHit)
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
		fun faceBow(thePlayer: IEntityPlayer, target: IEntity, turnSpeed: MinMaxPair, smoothingRatio: MinMaxPair, playerPredictSize: MinMaxPair, flags: Int)
		{
			val targetPosX = target.posX
			val targetPosY = target.posY
			val targetPosZ = target.posZ

			var distance = thePlayer.getDistanceToEntity(target).toDouble()
			distance -= distance % 0.8

			val sprinting = target.sprinting

			val playerPrediction = flags and PLAYER_PREDICT != 0
			val enemyPrediction = flags and ENEMY_PREDICT != 0

			val playerPosX = thePlayer.posX
			val playerBBPosY = thePlayer.entityBoundingBox.minY
			val playerPosZ = thePlayer.posZ

			// Calculate the (predicted) target position
			val minPlayerPredictSize = playerPredictSize.min
			val maxPlayerPredictSize = playerPredictSize.max
			val posX = targetPosX + (if (enemyPrediction) distance / 0.8 * ((targetPosX - target.lastTickPosX) * 0.4) * if (sprinting) 1.25f else 1f else 0.0) - (playerPosX + if (playerPrediction) (playerPosX - thePlayer.prevPosX) * nextFloat(minPlayerPredictSize, maxPlayerPredictSize) else 0.0)
			val posY = target.entityBoundingBox.minY + (if (enemyPrediction) distance / 0.8 * ((targetPosY - target.lastTickPosY) * 0.4) else 0.0) + target.eyeHeight - 0.15 - (playerBBPosY + if (playerPrediction) (thePlayer.posY - thePlayer.prevPosY) * nextFloat(minPlayerPredictSize, maxPlayerPredictSize) else 0.0) - thePlayer.eyeHeight
			val posZ = targetPosZ + (if (enemyPrediction) distance / 0.8 * ((targetPosZ - target.lastTickPosZ) * 0.4) * if (sprinting) 1.25f else 1f else 0.0) - (playerPosZ + if (playerPrediction) (playerPosZ - thePlayer.prevPosZ) * nextFloat(minPlayerPredictSize, maxPlayerPredictSize) else 0.0)

			// Bow Power Calculation
			val fastBow = LiquidBounce.moduleManager[FastBow::class.java] as FastBow
			val velocity = (((if (fastBow.state) fastBow.packetsValue.get() else thePlayer.itemInUseDuration) * 0.05f).let { it * it + it * 2f } / 3f).coerceAtMost(1f)
			val velocitySq = velocity.pow(2)

			// Calculate Rotation
			val distSq = posX * posX + posZ * posZ
			val dist = sqrt(distSq)
			val rotation = Rotation(toDegrees(atan2(posZ, posX).toFloat()) - 90, -toDegrees(atan((velocitySq - sqrt(velocity.pow(4) - 0.006f * (0.006f * distSq + 2 * posY * velocitySq))) / (0.006f * dist)).toFloat()))
			val limitedRotation = limitAngleChange(clientRotation, rotation, nextFloat(turnSpeed.min, turnSpeed.max), nextFloat(smoothingRatio.min, smoothingRatio.max))

			// Apply Rotation
			if (flags and SILENT_ROTATION != 0)
			{
				setTargetRotation(limitedRotation)
				setNextResetTurnSpeed(turnSpeed.min, turnSpeed.max)
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
		fun toRotation(thePlayer: IEntity, vec: WVec3, playerPredict: Boolean, playerPredictSize: MinMaxPair): Rotation
		{
			val posX = thePlayer.posX
			val posZ = thePlayer.posZ
			var eyesPos = WVec3(posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, posZ)

			if (playerPredict)
			{
				val xPredict = (posX - thePlayer.prevPosX) * nextFloat(playerPredictSize.min, playerPredictSize.max)
				val yPredict = (thePlayer.posY - thePlayer.prevPosY) * nextFloat(playerPredictSize.min, playerPredictSize.max)
				val zPredict = (posZ - thePlayer.prevPosZ) * nextFloat(playerPredictSize.min, playerPredictSize.max)

				eyesPos = eyesPos.plus(xPredict, yPredict, zPredict)
			}

			val diffX = vec.xCoord - eyesPos.xCoord
			val diffY = vec.yCoord - eyesPos.yCoord
			val diffZ = vec.zCoord - eyesPos.zCoord

			return Rotation(wrapAngleTo180_float(toDegrees(atan2(diffZ, diffX).toFloat()) - 90.0f), wrapAngleTo180_float(-toDegrees(atan2(diffY, hypot(diffX, diffZ)).toFloat())))
		}

		/**
		 * Get the center of a box
		 *
		 * @param  box
		 * your box
		 * @return     center of box
		 */
		fun getCenter(box: IAxisAlignedBB): WVec3 = WVec3(box.minX + (box.maxX - box.minX) * 0.5, box.minY + (box.maxY - box.minY) * 0.5, box.minZ + (box.maxZ - box.minZ) * 0.5)

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
		 * @param  boxShrink
		 * decrement of the entity hitbox size. default is 0.2D
		 * @param  steps
		 * count of step to search the good center. (*Warning If you set this value too low, it will make your minecraft SO SLOW AND SLOW.*) default is 0.2D
		 * @return                   center
		 */
		fun searchCenter(theWorld: IWorld, thePlayer: IEntity, targetBox: IAxisAlignedBB, flags: Int, jitterData: JitterData?, playerPredictSize: MinMaxPair, distance: Float, boxShrink: Double, steps: Int, randomCenterSize: Double, distanceOutOfRangeCallback: (() -> Unit)? = null): VecRotation?
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
					return VecRotation(randomVec, toRotation(thePlayer, randomVec, playerPredict, playerPredictSize))
				}

				flags and OUT_BORDER != 0 ->
				{
					randomVec = WVec3(minX + (maxX - minX) * (x * 0.3 + 1.0), minY + (maxY - minY) * (y * 0.3 + 1.0), minZ + (maxZ - minZ) * (z * 0.3 + 1.0))
					return VecRotation(randomVec, toRotation(thePlayer, randomVec, playerPredict, playerPredictSize))
				}

				else ->
				{
				}
			}

			randomVec = WVec3(minX + (maxX - minX) * x * randomCenterSize, minY + (maxY - minY) * y * randomCenterSize, minZ + (maxZ - minZ) * z * randomCenterSize)

			val randomRotation = toRotation(thePlayer, randomVec, playerPredict, playerPredictSize)

			// Calculate jitter amount
			val jitter = flags and JITTER != 0
			var yawJitterAmount = 0f
			var pitchJitterAmount = 0f
			if (jitter && jitterData != null)
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

			val shrink = boxShrink.coerceIn(0.0, 1.0) // The final failsafe
			val boxEnd = 1 - shrink

			// Parse flags
			val skipVisibleCheck = flags and SKIP_VISIBLE_CHECK != 0
			val randomCenter = flags and RANDOM_CENTER != 0

			var distanceCheckPassed = false

			val increment = 1.0 / steps.toDouble()

			var xSearch = shrink
			while (xSearch < boxEnd)
			{
				var ySearch = shrink
				while (ySearch < boxEnd)
				{
					var zSearch = shrink
					while (zSearch < boxEnd)
					{
						val vec3 = WVec3(minX + (maxX - minX) * xSearch, minY + (maxY - minY) * ySearch, minZ + (maxZ - minZ) * zSearch)
						val vecDist = eyes.distanceTo(vec3)

						// Distance check
						if (vecDist > distance)
						{
							zSearch += increment
							continue
						}
						if (!distanceCheckPassed) distanceCheckPassed = true

						// Visibility check
						if (skipVisibleCheck || isVisible(theWorld, thePlayer, vec3))
						{
							val newRotation = toRotation(thePlayer, vec3, playerPredict, playerPredictSize)
							val newVecRotation = VecRotation(vec3, newRotation)

							if (vecRotation == null || run {
									// Check the new rotation is better than the old one
									val second = if (randomCenter) randomRotation else serverRotation
									getRotationDifference(newRotation, second) < getRotationDifference(vecRotation!!.rotation, second)
								}) vecRotation = newVecRotation
						}
						zSearch += increment
					}
					ySearch += increment
				}
				xSearch += increment
			}

			if (!distanceCheckPassed) distanceOutOfRangeCallback?.invoke()

			// Apply Jitter
			if (vecRotation != null && jitter)
			{
				vecRotation.rotation.yaw += yawJitterAmount

				// Enforce pitch to 90 ~ -90
				vecRotation.rotation.pitch = (vecRotation.rotation.pitch + pitchJitterAmount).coerceIn(-90f, 90f)
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
		fun getClientRotationDifference(thePlayer: IEntity, entity: IEntity, playerPredict: Boolean, playerPredictSize: MinMaxPair): Double = getRotationDifference(toRotation(thePlayer, getCenter(entity.entityBoundingBox), playerPredict, playerPredictSize), clientRotation)

		/**
		 * Calculate difference between the "client-sided rotation" and your block position
		 *
		 * @param  blockPos
		 * your block position
		 * @return        difference between rotation
		 */
		fun getClientRotationDifference(thePlayer: IEntity, blockPos: WBlockPos, playerPredict: Boolean, playerPredictSize: MinMaxPair): Double = getRotationDifference(toRotation(thePlayer, WVec3(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5), playerPredict, playerPredictSize), clientRotation)

		/**
		 * Calculate difference between the "server-sided rotation" and your entity
		 *
		 * @param  entity
		 * your entity
		 * @return        difference between rotation
		 */
		fun getServerRotationDifference(thePlayer: IEntity, entity: IEntity, playerPredict: Boolean, playerPredictSize: MinMaxPair): Double = getServerRotationDifference(thePlayer, getCenter(entity.entityBoundingBox), playerPredict, playerPredictSize)

		/**
		 * Calculate difference between the "server-sided rotation" and your block position
		 *
		 * @param  blockPos
		 * your block position
		 * @return        difference between rotation
		 */
		fun getServerRotationDifference(thePlayer: IEntity, blockPos: WBlockPos, playerPredict: Boolean, playerPredictSize: MinMaxPair): Double = getServerRotationDifference(thePlayer, WVec3(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5), playerPredict, playerPredictSize)

		private fun getServerRotationDifference(thePlayer: IEntity, pos: WVec3, playerPredict: Boolean, playerPredictSize: MinMaxPair): Double = getRotationDifference(toRotation(thePlayer, pos, playerPredict, playerPredictSize), serverRotation)

		// /**
		//  * Calculate difference between the client rotation and your entity
		//  *
		//  * @param  entity
		//  * your entity
		//  * @return        difference between rotation
		//  */
		// fun getRotationDifference(thePlayer: IEntity, entity: IEntity): Double
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
		fun getRotationDifference(first: Rotation, second: Rotation): Double = hypot(getAngleDifference(first.yaw, second.yaw).toDouble(), (first.pitch - second.pitch).toDouble())

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

			// Apply Acceleration
			val accel = acceleration.coerceAtMost(1.0f)
			yawDelta -= yawDelta * accel
			pitchDelta -= pitchDelta * accel

			// Apply TurnSpeed limit
			return Rotation(currentRotation.yaw + if (yawDelta > turnSpeed) turnSpeed else max(yawDelta, -turnSpeed), currentRotation.pitch + if (pitchDelta > turnSpeed) turnSpeed else max(pitchDelta, -turnSpeed))
		}

		private fun getAngleDifference(rot1: Rotation, rot2: Rotation): Double = hypot(getAngleDifference(rot1.yaw, rot2.yaw).toDouble(), getAngleDifference(rot1.pitch, rot2.pitch).toDouble())

		/**
		 * Calculate difference between two angle points
		 *
		 * @param  angle
		 * angle point
		 * @param  otherAngle
		 * angle point
		 * @return   difference between angle points
		 */
		fun getAngleDifference(angle: Float, otherAngle: Float): Float = ((angle - otherAngle) % 360.0f + 540.0f) % 360.0f - 180.0f

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

			val pitchCos = -cos(-pitchRadians)

			return WVec3((sin(-yawRadians - PI) * pitchCos).toDouble(), sin(-pitchRadians).toDouble(), (cos(-yawRadians - PI) * pitchCos).toDouble())
		}

		/**
		 * Allows you to check if your crosshair is over your target entity
		 *
		 * @param  targetEntity
		 * your target entity
		 * @param  reachDistance
		 * your reach
		 * @return                    if crosshair is over target
		 */
		fun isFaced(theWorld: IWorld, thePlayer: IEntity, targetEntity: IEntity?, reachDistance: Double, expandRange: Double = 1.0, aabbGetter: (IEntity) -> IAxisAlignedBB = IEntity::entityBoundingBox): Boolean
		{
			targetEntity ?: return false
			return theWorld.raycastEntity(thePlayer, reachDistance, expandRange = expandRange, aabbGetter = aabbGetter, entityFilter = { entity -> targetEntity == entity }) != null
		}

		/**
		 * Allows you to check if your enemy is behind a wall
		 */
		fun isVisible(theWorld: IWorld, thePlayer: IEntity, vec3: WVec3): Boolean = theWorld.rayTraceBlocks(WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ), vec3) == null

		/**
		 * Set your target rotation
		 *
		 * @param rotation
		 * your target rotation
		 */
		fun setTargetRotation(rotation: Rotation?, keepLength: Int = 0)
		{
			if (rotation != null && (isNaN(rotation.yaw.toDouble()) || isNaN(rotation.pitch.toDouble()) || rotation.pitch > 90 || rotation.pitch < -90)) return

			targetRotation = rotation?.apply { fixedSensitivity(mc.gameSettings.mouseSensitivity) }

			Companion.keepLength = keepLength
		}

		fun setNextResetTurnSpeed(min: Float, max: Float)
		{
			minResetTurnSpeed = min
			maxResetTurnSpeed = max
		}

		/**
		 * Reset your target rotation
		 */
		fun reset()
		{
			keepLength = 0

			val goalRotation = clientRotation

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
