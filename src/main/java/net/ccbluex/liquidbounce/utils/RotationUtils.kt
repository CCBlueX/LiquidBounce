/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.combat.FastBow
import net.ccbluex.liquidbounce.features.module.modules.render.Rotations
import net.ccbluex.liquidbounce.utils.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextDouble
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.*
import java.security.SecureRandom
import java.util.*
import kotlin.math.*

object RotationUtils : MinecraftInstance(), Listenable {

    private val secureRandom = SecureRandom()
    private var currentSpot = Vec3(0.0, 0.0, 0.0)

    private val random = Random()

    private var targetRotation: Rotation? = null

    var currentRotation: Rotation? = null
    var serverRotation = Rotation(0f, 0f)
    var lastServerRotation = Rotation(0f, 0f)
    var secondLastRotation = Rotation(0f, 0f)
    
    var rotationData: RotationData? = null

    var resetTicks = 0

    var sameYawDiffTicks = 0
    var samePitchDiffTicks = 0

    private fun findNextAimSpot() {
        val nextSpot = currentSpot.add(
            Vec3(secureRandom.nextGaussian(), secureRandom.nextGaussian(), secureRandom.nextGaussian())
        )

        currentSpot = nextSpot.times(nextDouble(0.85, 0.9))
    }

    /**
     * Face block
     *
     * @param blockPos target block
     */
    fun faceBlock(blockPos: BlockPos?, throughWalls: Boolean = true): VecRotation? {
        val world = mc.theWorld ?: return null
        val player = mc.thePlayer ?: return null

        if (blockPos == null)
            return null

        val eyesPos = player.eyes
        val startPos = Vec3(blockPos)

        var visibleVec: VecRotation? = null
        var invisibleVec: VecRotation? = null

        for (x in 0.0..1.0) {
            for (y in 0.0..1.0) {
                for (z in 0.0..1.0) {
                    val block = blockPos.getBlock() ?: return null

                    val posVec = startPos.add(block.lerpWith(x, y, z))

                    val dist = eyesPos.distanceTo(posVec)

                    val (diffX, diffY, diffZ) = posVec - eyesPos
                    val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)

                    val rotation = Rotation(
                        MathHelper.wrapAngleTo180_float(atan2(diffZ, diffX).toDegreesF() - 90f),
                        MathHelper.wrapAngleTo180_float(-atan2(diffY, diffXZ).toDegreesF())
                    ).fixedSensitivity()

                    val rotationVector = getVectorForRotation(rotation)
                    val vector = eyesPos + (rotationVector * dist)

                    val currentVec = VecRotation(posVec, rotation)
                    val raycast = world.rayTraceBlocks(eyesPos, vector, false, true, false)

                    val currentRotation = currentRotation ?: player.rotation

                    if (raycast != null && raycast.blockPos == blockPos) {
                        if (visibleVec == null || getRotationDifference(
                                currentVec.rotation,
                                currentRotation
                            ) < getRotationDifference(visibleVec.rotation, currentRotation)
                        ) {
                            visibleVec = currentVec
                        }
                    } else if (throughWalls) {
                        val invisibleRaycast = performRaytrace(blockPos, rotation) ?: continue

                        if (invisibleRaycast.blockPos != blockPos) {
                            continue
                        }

                        if (invisibleVec == null || getRotationDifference(
                                currentVec.rotation,
                                currentRotation
                            ) < getRotationDifference(invisibleVec.rotation, currentRotation)
                        ) {
                            invisibleVec = currentVec
                        }
                    }
                }
            }
        }

        return visibleVec ?: invisibleVec
    }

    /**
     * Face trajectory of arrow by default, can be used for calculating other trajectories (eggs, snowballs)
     * by specifying `gravity` and `velocity` parameters
     *
     * @param target      your enemy
     * @param predict     predict new enemy position
     * @param predictSize predict size of predict
     * @param gravity     how much gravity does the projectile have, arrow by default
     * @param velocity    with what velocity will the projectile be released, velocity for arrow is calculated when null
     */
    fun faceTrajectory(
        target: Entity,
        predict: Boolean,
        predictSize: Float,
        gravity: Float = 0.05f,
        velocity: Float? = null,
    ): Rotation {
        val player = mc.thePlayer

        val posX =
            target.posX + (if (predict) (target.posX - target.prevPosX) * predictSize else .0) - (player.posX + if (predict) player.posX - player.prevPosX else .0)
        val posY =
            target.entityBoundingBox.minY + (if (predict) (target.entityBoundingBox.minY - target.prevPosY) * predictSize else .0) + target.eyeHeight - 0.15 - (player.entityBoundingBox.minY + (if (predict) player.posY - player.prevPosY else .0)) - player.getEyeHeight()
        val posZ =
            target.posZ + (if (predict) (target.posZ - target.prevPosZ) * predictSize else .0) - (player.posZ + if (predict) player.posZ - player.prevPosZ else .0)
        val posSqrt = sqrt(posX * posX + posZ * posZ)

        var velocity = velocity
        if (velocity == null) {
            velocity = if (FastBow.handleEvents()) 1f else player.itemInUseDuration / 20f
            velocity = ((velocity * velocity + velocity * 2) / 3).coerceAtMost(1f)
        }

        val gravityModifier = 0.12f * gravity

        return Rotation(
            atan2(posZ, posX).toDegreesF() - 90f,
            -atan((velocity * velocity - sqrt(
                velocity * velocity * velocity * velocity - gravityModifier * (gravityModifier * posSqrt * posSqrt + 2 * posY * velocity * velocity)
            )) / (gravityModifier * posSqrt)
            ).toDegreesF()
        )
    }

    /**
     * Translate vec to rotation
     *
     * @param vec     target vec
     * @param predict predict new location of your body
     * @return rotation
     */
    fun toRotation(vec: Vec3, predict: Boolean = false, fromEntity: Entity = mc.thePlayer): Rotation {
        val eyesPos = fromEntity.eyes
        if (predict) eyesPos.addVector(fromEntity.motionX, fromEntity.motionY, fromEntity.motionZ)

        val (diffX, diffY, diffZ) = vec - eyesPos
        return Rotation(
            MathHelper.wrapAngleTo180_float(
                atan2(diffZ, diffX).toDegreesF() - 90f
            ), MathHelper.wrapAngleTo180_float(
                -atan2(diffY, sqrt(diffX * diffX + diffZ * diffZ)).toDegreesF()
            )
        )
    }

    /**
     * Search good center
     *
     * @param bb                entity box to search rotation for
     * @param outborder         outborder option
     * @param random            random option
     * @param predict           predict, offsets rotation by player's motion
     * @param lookRange         look range
     * @param attackRange       attack range, rotations in attack range will be prioritized
     * @param throughWallsRange through walls range,
     * @return center
     */
    fun searchCenter(
        bb: AxisAlignedBB, outborder: Boolean, random: Boolean, gaussianOffset: Boolean, predict: Boolean,
        lookRange: Float, attackRange: Float, throughWallsRange: Float = 0f,
    ): Rotation? {
        val lookRange = lookRange.coerceAtLeast(attackRange)

        val spot = if (random && gaussianOffset) {
            findNextAimSpot()
            currentSpot
        } else if (random && this.random.nextGaussian() > 0.8) {
            Vec3(this.random.nextDouble(), this.random.nextDouble(), this.random.nextDouble())
        } else null

        if (outborder) {
            val vec3 = bb.lerpWith(nextDouble(0.5, 1.3), nextDouble(0.9, 1.3), nextDouble(0.5, 1.3))

            return toRotation(vec3, predict).fixedSensitivity()
        }

        val eyes = mc.thePlayer.eyes

        val isInsideEnemy = bb.isVecInside(eyes)

        val currRotation = currentRotation ?: mc.thePlayer.rotation

        if (random && spot != null) {
            val randomVec = if (gaussianOffset) {
                bb.center.add(spot)
            } else {
                bb.lerpWith(spot)
            }

            val randomRotation = toRotation(randomVec, predict).fixedSensitivity()
            val vector = eyes + getVectorForRotation(randomRotation) * attackRange.toDouble()

            val intercept = if (isInsideEnemy) {
                return currRotation
            } else {
                bb.calculateIntercept(eyes, vector)
            }

            val dist = eyes.distanceTo(randomVec)

            if (dist <= attackRange && (intercept != null && isVisible(intercept.hitVec) || dist <= throughWallsRange))
                return randomRotation
        }

        val vector = eyes + getVectorForRotation(currRotation) * attackRange.toDouble()

        val intercept = if (isInsideEnemy) {
            return currRotation
        } else {
            bb.calculateIntercept(eyes, vector)
        }

        if (intercept != null && !random) {
            val spot = intercept.hitVec
            val dist = eyes.distanceTo(spot)

            if (dist <= attackRange && (dist <= throughWallsRange || isVisible(spot)))
                return currRotation
        }

        var attackRotation: Pair<Rotation, Float>? = null
        var lookRotation: Pair<Rotation, Float>? = null

        for (x in 0.0..1.0) {
            for (y in 0.0..1.0) {
                for (z in 0.0..1.0) {
                    val vec = bb.lerpWith(x, y, z)

                    val rotation = toRotation(vec, predict).fixedSensitivity()

                    // Calculate actual hit vec after applying fixed sensitivity to rotation
                    val gcdVec = bb.calculateIntercept(eyes,
                        eyes + getVectorForRotation(rotation) * lookRange.toDouble()
                    )?.hitVec ?: continue

                    val distance = eyes.distanceTo(gcdVec)

                    // Check if vec is in range
                    // Skip if a rotation that is in attack range was already found and the vec is out of attack range
                    if (distance > lookRange || (attackRotation != null && distance > attackRange))
                        continue

                    // Check if vec is reachable through walls
                    if (!isVisible(gcdVec) && distance > throughWallsRange)
                        continue

                    val rotationWithDiff = rotation to getRotationDifference(rotation, currRotation)

                    if (distance <= attackRange) {
                        if (attackRotation == null || rotationWithDiff.second < attackRotation.second)
                            attackRotation = rotationWithDiff
                    } else {
                        if (lookRotation == null || rotationWithDiff.second < lookRotation.second)
                            lookRotation = rotationWithDiff
                    }
                }
            }
        }

        return attackRotation?.first ?: lookRotation?.first ?: run {
            val vec = getNearestPointBB(eyes, bb)
            val dist = eyes.distanceTo(vec)

            if (dist <= lookRange && (dist <= throughWallsRange || isVisible(vec)))
                toRotation(vec, predict)
            else null
        }
    }

    /**
     * Calculate difference between the client rotation and your entity
     *
     * @param entity your entity
     * @return difference between rotation
     */
    fun getRotationDifference(entity: Entity) =
        getRotationDifference(toRotation(entity.hitBox.center, true), mc.thePlayer.rotation)

    /**
     * Calculate difference between two rotations
     *
     * @param a rotation
     * @param b rotation
     * @return difference between rotation
     */
    fun getRotationDifference(a: Rotation, b: Rotation = serverRotation) =
        hypot(getAngleDifference(a.yaw, b.yaw), a.pitch - b.pitch)

    /**
     * Limit your rotation using a turn speed
     *
     * @param currentRotation your current rotation
     * @param targetRotation your goal rotation
     * @param turnSpeed your turn speed
     * @param smootherMode your smoother mode
     * @param startOffSlow used for modules that do not utilize rotations
     * @return limited rotation
     */
    fun limitAngleChange(
        currentRotation: Rotation,
        targetRotation: Rotation,
        turnSpeed: Float,
        smootherMode: String = "Linear",
        startOffSlow: Boolean = false,
    ): Rotation {
        return limitAngleChange(currentRotation,
            targetRotation,
            turnSpeed..turnSpeed,
            smootherMode = smootherMode,
            nonDataStartOffSlow = startOffSlow
        )
    }

    fun limitAngleChange(
        currentRotation: Rotation,
        targetRotation: Rotation,
        hSpeed: ClosedFloatingPointRange<Float>,
        vSpeed: ClosedFloatingPointRange<Float> = hSpeed,
        smootherMode: String,
        nonDataStartOffSlow: Boolean = false,
    ): Rotation {
        if (rotationData?.simulateShortStop == true && Math.random() > 0.75) {
            return currentRotation
        }

        val firstSlow = rotationData?.startOffSlow == true || nonDataStartOffSlow
        
        return performAngleChange(currentRotation, targetRotation, hSpeed.random(), vSpeed.random(), firstSlow, Rotations.startSecondRotationSlow, Rotations.slowDownOnDirectionChange, smootherMode)   
    }

    fun limitAngleChange(
        currentRotation: Rotation,
        targetRotation: Rotation,
        rotationData: RotationData,
    ): Rotation {
        return limitAngleChange(currentRotation,
            targetRotation,
            rotationData.hSpeed,
            rotationData.vSpeed,
            rotationData.smootherMode.modeName
        )
    }

    private fun performAngleChange(
        currentRotation: Rotation, targetRotation: Rotation, hSpeed: Float,
        vSpeed: Float, startFirstSlow: Boolean, startSecondSlow: Boolean,
        slowDownOnDirChange: Boolean, smootherMode: String
    ): Rotation {
        val yawDifference = getAngleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDifference = getAngleDifference(targetRotation.pitch, currentRotation.pitch)

        val yawTicks = ClientUtils.runTimeTicks - sameYawDiffTicks
        val pitchTicks = ClientUtils.runTimeTicks - samePitchDiffTicks
        
        val oldYawDiff = getAngleDifference(serverRotation.yaw, lastServerRotation.yaw)
        val oldPitchDiff = getAngleDifference(serverRotation.pitch, lastServerRotation.pitch)

        val secondOldYawDiff = getAngleDifference(lastServerRotation.yaw, secondLastRotation.yaw)
        val secondOldPitchDiff = getAngleDifference(lastServerRotation.pitch, secondLastRotation.pitch)
        
        val rotationDifference = hypot(yawDifference, pitchDifference)

        val (hFactor, vFactor) = if (smootherMode == "Relative") {
            computeFactor(rotationDifference, hSpeed) to computeFactor(rotationDifference, vSpeed)
        } else {
            hSpeed to vSpeed
        }
        
        var straightLineYaw = abs(yawDifference / rotationDifference) * hFactor
        var straightLinePitch = abs(pitchDifference / rotationDifference) * vFactor

        var (yawDirChange, pitchDirChange) = false to false
    
        straightLineYaw = computeSlowDown(straightLineYaw, oldYawDiff, secondOldYawDiff, yawTicks, startFirstSlow, startSecondSlow, slowDownOnDirChange, tickUpdate = { sameYawDiffTicks = ClientUtils.runTimeTicks }) { yawDirChange = true }
        straightLinePitch = computeSlowDown(straightLinePitch, oldPitchDiff, secondOldPitchDiff, pitchTicks, startFirstSlow, startSecondSlow, slowDownOnDirChange, tickUpdate = { samePitchDiffTicks = ClientUtils.runTimeTicks }) { pitchDirChange = true }

        val coercedYaw = if (yawDirChange) {
            oldYawDiff * nextFloat(0f, 0.2f)
        } else yawDifference.coerceIn(-straightLineYaw, straightLineYaw)
        val coercedPitch = if (pitchDirChange) {
            oldPitchDiff * nextFloat(0f, 0.2f)  
        } else pitchDifference.coerceIn(-straightLinePitch, straightLinePitch)
        
        val finalPitchDiff = if (Rotations.experimentalCurve) {
            createCurvedPath(currentRotation, targetRotation, vSpeed, coercedYaw, coercedPitch)
        } else coercedPitch
        
        return Rotation(currentRotation.yaw + coercedYaw, currentRotation.pitch + finalPitchDiff)
    }

    /**
    * Ease rotation simulation. A technique unknowingly performed by most humans when they move their mouse.
    * 
    * Useful for rotation-sensitive anti-cheats.
    */
    private fun computeSlowDown(newDiff: Float, oldDiff: Float,
        secondOldDiff: Float, ticks: Int, firstSlow: Boolean,
        secondSlow: Boolean, slowDownOnDirChange: Boolean, tickUpdate: () -> Unit, onDirChange: () -> Unit
    ): Float {
        val result = abs(oldDiff / newDiff)

        val diffDir = newDiff != 0f && oldDiff != 0f && newDiff.sign != oldDiff.sign
        val secondDiffDir = secondOldDiff == 0f || secondOldDiff.sign != oldDiff.sign

        val shouldStartSlow = firstSlow && (oldDiff == 0f || ticks == 1 && secondSlow)
        val shouldEaseOnDirChange = slowDownOnDirChange && (diffDir && (secondDiffDir || secondOldDiff <= oldDiff))

        // Are we going to rotate the other direction?
        if (shouldEaseOnDirChange) {
            onDirChange()
            return newDiff
        }
        
        // Have we not rotated the previous tick and should start slow?
        val factor = if (shouldStartSlow) {
            if (oldDiff == 0f) {
                tickUpdate()
            }

            result + nextFloat(0f, 0.3f - if (oldDiff == 0f) 0.2f else 0f)
        } else 1f
       
        return newDiff * factor
    }
 
    private fun createCurvedPath(currentRotation: Rotation, targetRotation: Rotation, 
        vSpeed: Float,
        yawDifference: Float, pitchDifference: Float
    ): Float {
        val control = (targetRotation.pitch + 10).coerceIn(-90f, 90f)

        val diff = yawDifference + if (yawDifference == 0f) pitchDifference else 0f
        
        var t = ((diff.coerceIn(-vSpeed, vSpeed) / 120f) % 1f)
    
        return bezierInterpolate(currentRotation.pitch, control, targetRotation.pitch, 1 - t).coerceIn(-90f, 90f) - currentRotation.pitch
    }

    private fun computeFactor(rotationDifference: Float, turnSpeed: Float): Float {
        var min = (4f..6f).random()
        var t = rotationDifference / min
        
        if (t < 1.0f) {
            min = t
        }
        
        return (rotationDifference / 180 * turnSpeed).coerceIn(min, 180f)
    }

    fun bezierInterpolate(start: Float, control: Float, end: Float, t: Float): Float {
        return (1 - t) * (1 - t) * start + 2 * (1 - t) * t * control + t * t * end
    }
    
    /**
     * Calculate difference between two angle points
     *
     * @param a angle point
     * @param b angle point
     * @return difference between angle points
     */
    fun getAngleDifference(a: Float, b: Float) = MathHelper.wrapAngleTo180_float(a - b)

    /**
     * Calculate rotation to vector
     *
     * @param [yaw] [pitch] your rotation
     * @return target vector
     */
    fun getVectorForRotation(yaw: Float, pitch: Float): Vec3 {
        val yawRad = yaw.toRadians()
        val pitchRad = pitch.toRadians()

        val f = MathHelper.cos(-yawRad - PI.toFloat())
        val f1 = MathHelper.sin(-yawRad - PI.toFloat())
        val f2 = -MathHelper.cos(-pitchRad)
        val f3 = MathHelper.sin(-pitchRad)

        return Vec3((f1 * f2).toDouble(), f3.toDouble(), (f * f2).toDouble())
    }

    fun getVectorForRotation(rotation: Rotation) = getVectorForRotation(rotation.yaw, rotation.pitch)

    /**
     * Allows you to check if your crosshair is over your target entity
     *
     * @param targetEntity       your target entity
     * @param blockReachDistance your reach
     * @return if crosshair is over target
     */
    fun isFaced(targetEntity: Entity, blockReachDistance: Double) =
        raycastEntity(blockReachDistance) { entity: Entity -> targetEntity == entity } != null

    /**
     * Allows you to check if your crosshair is over your target entity
     *
     * @param targetEntity       your target entity
     * @param blockReachDistance your reach
     * @return if crosshair is over target
     */
    fun isRotationFaced(targetEntity: Entity, blockReachDistance: Double, rotation: Rotation) = raycastEntity(
        blockReachDistance,
        rotation.yaw,
        rotation.pitch
    ) { entity: Entity -> targetEntity == entity } != null

    /**
     * Allows you to check if your enemy is behind a wall
     */
    fun isVisible(vec3: Vec3) = mc.theWorld.rayTraceBlocks(mc.thePlayer.eyes, vec3) == null

    /**
     * Set your target rotation
     *
     * @param rotation your target rotation
     */
    fun setTargetRotation(
        rotation: Rotation,
        keepLength: Int = 1,
        strafe: Boolean = false,
        strict: Boolean = false,
        applyClientSide: Boolean = false,
        turnSpeed: Pair<ClosedFloatingPointRange<Float>, ClosedFloatingPointRange<Float>> = 180f..180f to 180f..180f,
        angleThresholdForReset: Float = 180f,
        smootherMode: String = "Linear",
        simulateShortStop: Boolean = false,
        startOffSlow: Boolean = false,
        immediate: Boolean = false,
        prioritizeRequest: Boolean = false,
    ) {
        if (rotation.yaw.isNaN() || rotation.pitch.isNaN() || rotation.pitch > 90 || rotation.pitch < -90) {
            return
        }

        if (!prioritizeRequest && rotationData?.prioritizeRequest == true) {
            return
        }

        if (applyClientSide) {
            currentRotation?.let {
                mc.thePlayer.rotationYaw = it.yaw
                mc.thePlayer.rotationPitch = it.pitch
            }

            resetRotation()
        }

        targetRotation = rotation

        rotationData = RotationData(
            turnSpeed.first,
            turnSpeed.second,
            SmootherMode.values().first { it.modeName == smootherMode },
            strafe,
            strict,
            applyClientSide,
            immediate,
            angleThresholdForReset,
            prioritizeRequest,
            simulateShortStop,
            startOffSlow
        )

        this.resetTicks = if (applyClientSide) 1 else keepLength

        if (immediate) {
            update()
        }
    }

    private fun resetRotation() {
        resetTicks = 0
        currentRotation?.let { rotation ->
            mc.thePlayer?.let {
                it.rotationYaw = rotation.yaw + getAngleDifference(it.rotationYaw, rotation.yaw)
                syncRotations()
            }
        }
        targetRotation = null
        currentRotation = null
        rotationData = null
    }

    /**
     * Returns the smallest angle difference possible with a specific sensitivity ("gcd")
     */
    fun getFixedAngleDelta(sensitivity: Float = mc.gameSettings.mouseSensitivity) =
        (sensitivity * 0.6f + 0.2f).pow(3) * 1.2f

    /**
     * Returns angle that is legitimately accomplishable with player's current sensitivity
     */
    fun getFixedSensitivityAngle(targetAngle: Float, startAngle: Float = 0f, gcd: Float = getFixedAngleDelta()) =
        startAngle + ((targetAngle - startAngle) / gcd).roundToInt() * gcd

    /**
     * Creates a raytrace even when the target [blockPos] is not visible
     */
    fun performRaytrace(
        blockPos: BlockPos,
        rotation: Rotation,
        reach: Float = mc.playerController.blockReachDistance,
    ): MovingObjectPosition? {
        val world = mc.theWorld ?: return null
        val player = mc.thePlayer ?: return null

        val eyes = player.eyes

        return blockPos.getBlock()?.collisionRayTrace(
            world,
            blockPos,
            eyes,
            eyes + (getVectorForRotation(rotation) * reach.toDouble())
        )
    }

    fun performRayTrace(blockPos: BlockPos, vec: Vec3, eyes: Vec3 = mc.thePlayer.eyes) =
        mc.theWorld?.let { blockPos.getBlock()?.collisionRayTrace(it, blockPos, eyes, vec) }

    fun syncRotations() {
        val player = mc.thePlayer ?: return

        player.prevRotationYaw = player.rotationYaw
        player.prevRotationPitch = player.rotationPitch
        player.renderArmYaw = player.rotationYaw
        player.renderArmPitch = player.rotationPitch
        player.prevRenderArmYaw = player.rotationYaw
        player.prevRotationPitch = player.rotationPitch
    }

    private fun update() {
        val data = rotationData ?: return
        val player = mc.thePlayer ?: return

        val playerRotation = player.rotation

        val shouldUpdate = !InventoryUtils.serverOpenContainer && !InventoryUtils.serverOpenInventory

        if (!shouldUpdate) {
            return
        }

        if (resetTicks == 0) {
            val distanceToPlayerRotation = getRotationDifference(currentRotation ?: serverRotation, playerRotation)

            if (distanceToPlayerRotation <= data.resetThreshold || data.clientSide) {
                resetRotation()
                return
            }

            currentRotation = limitAngleChange(currentRotation ?: serverRotation,
                playerRotation,
                data
            ).fixedSensitivity()
            return
        }

        targetRotation?.let {
            limitAngleChange(currentRotation ?: playerRotation, it, data).let { rotation ->
                if (data.clientSide) {
                    rotation.toPlayer(player)
                } else {
                    currentRotation = rotation.fixedSensitivity()
                }
            }
        }

        if (resetTicks > 0) {
            resetTicks--
        }
    }

    @EventTarget(priority = -1)
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.POST) {
            return
        }
        
        rotationData?.let {
            // Was the rotation update immediate? Allow updates the next tick.
            if (it.immediate) {
                it.immediate = false
                return
            }
        }

        update()
    }

    /**
     * Handle strafing
     */
    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        val data = rotationData ?: return

        if (!data.strafe) {
            return
        }

        currentRotation?.let {
            it.applyStrafeToPlayer(event, data.strict)
            event.cancelEvent()
        }
    }

    /**
     * Handle rotation-packet modification
     */
    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet !is C03PacketPlayer || !packet.rotating) {
            return
        }

        currentRotation?.let {
            packet.yaw = it.yaw
            packet.pitch = it.pitch

            val yawDiff = getAngleDifference(it.yaw, serverRotation.yaw)
            val pitchDiff = getAngleDifference(it.pitch, serverRotation.pitch)
       
            if (Rotations.debugRotations) {
                ClientUtils.displayChatMessage("DIFF | YAW: ${yawDiff}, PITCH: ${pitchDiff}, sameYawTick: ${ClientUtils.runTimeTicks - sameYawDiffTicks}, samePitchDiff: ${ClientUtils.runTimeTicks - samePitchDiffTicks}")
            }
        }
        
        serverRotation = Rotation(packet.yaw, packet.pitch)
    }

    enum class SmootherMode(val modeName: String) { LINEAR("Linear"), RELATIVE("Relative") }

    data class RotationData(
        var hSpeed: ClosedFloatingPointRange<Float>, var vSpeed: ClosedFloatingPointRange<Float>,
        var smootherMode: SmootherMode, var strafe: Boolean, var strict: Boolean, var clientSide: Boolean,
        var immediate: Boolean, var resetThreshold: Float, val prioritizeRequest: Boolean,
        val simulateShortStop: Boolean, val startOffSlow: Boolean,
    )

    /**
     * @return YESSSS!
     */
    override fun handleEvents() = true

}
