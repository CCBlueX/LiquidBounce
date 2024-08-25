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
import net.minecraft.network.packet.c2s.play.C03PacketPlayer
import net.minecraft.util.*
import kotlin.math.*

object RotationUtils : MinecraftInstance(), Listenable {

    private var targetRotation: Rotation? = null

    var currentRotation: Rotation? = null
    var serverRotation = Rotation(0f, 0f)
    var lastServerRotation = Rotation(0f, 0f)
    var secondLastRotation = Rotation(0f, 0f)

    var rotationData: RotationData? = null

    var resetTicks = 0

    private var sameYawDiffTicks = 0
    private var samePitchDiffTicks = 0

    /**
     * Face block
     *
     * @param blockPos target block
     */
    fun faceBlock(blockPos: BlockPos?, throughWalls: Boolean = true): VecRotation? {
        val world = mc.world ?: return null
        val player = mc.player ?: return null

        if (blockPos == null)
            return null

        val eyesPos = player.eyes
        val startPos = Vec3d(blockPos)

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
                    val raycast = world.rayTrace(eyesPos, vector, false, true, false)

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
        val player = mc.player

        val posX =
            target.x + (if (predict) (target.x - target.prevX) * predictSize else .0) - (player.x + if (predict) player.x - player.prevPosX else .0)
        val posY =
            target.entityBoundingBox.minY + (if (predict) (target.entityBoundingBox.minY - target.prevY) * predictSize else .0) + target.eyeHeight - 0.15 - (player.entityBoundingBox.minY + (if (predict) player.y - player.prevY else .0)) - player.getEyeHeight()
        val posZ =
            target.z + (if (predict) (target.z - target.prevZ) * predictSize else .0) - (player.z + if (predict) player.z - player.prevZ else .0)
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
    fun toRotation(vec: Vec3d, predict: Boolean = false, fromEntity: Entity = mc.player): Rotation {
        val eyesPos = fromEntity.eyes
        if (predict) eyesPos.addVector(fromEntity.velocityX, fromEntity.velocityY, fromEntity.velocityZ)

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
        bb: Box, outborder: Boolean, random: Boolean, predict: Boolean,
        lookRange: Float, attackRange: Float, throughWallsRange: Float = 0f,
        bodyPoints: List<String> = listOf("Head", "Feet"), horizontalSearch: ClosedFloatingPointRange<Float> = 0f..1f,
    ): Rotation? {
        val lookRange = lookRange.coerceAtLeast(attackRange)

        val max = BodyPoint.fromString(bodyPoints[0]).range.endInclusive
        val min = BodyPoint.fromString(bodyPoints[1]).range.start

        if (outborder) {
            val Vec3d = bb.lerpWith(nextDouble(0.5, 1.3), nextDouble(0.9, 1.3), nextDouble(0.5, 1.3))

            return toRotation(Vec3d, predict).fixedSensitivity()
        }

        val eyes = mc.player.eyes

        var currRotation = currentRotation ?: mc.player.rotation

        var attackRotation: Pair<Rotation, Float>? = null
        var lookRotation: Pair<Rotation, Float>? = null

        if (random) {
            currRotation += Rotation(
                if (Math.random() > 0.25) nextFloat(-15f, 15f) else 0f,
                if (Math.random() > 0.25) nextFloat(-10f, 10f) else 0f
            )
        }

        val (hMin, hMax) = horizontalSearch.start.toDouble() to horizontalSearch.endInclusive.toDouble()

        for (x in hMin..hMax) {
            for (y in min..max) {
                for (z in hMin..hMax) {
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
        getRotationDifference(toRotation(entity.hitBox.center, true), mc.player.rotation)

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
        slowOnDirChange: Boolean = false,
        useStraightLinePath: Boolean = false,
        minRotationDifference: Float = 0f,
    ): Rotation {
        return limitAngleChange(
            currentRotation,
            targetRotation,
            hSpeed = turnSpeed..turnSpeed,
            smootherMode = smootherMode,
            nonDataStartOffSlow = startOffSlow,
            nonDataUseStraightLinePath = useStraightLinePath,
            nonDataSlowDownOnDirChange = slowOnDirChange,
            nonDataMinRotationDifference = minRotationDifference
        )
    }

    fun limitAngleChange(
        currentRotation: Rotation,
        targetRotation: Rotation,
        hSpeed: ClosedFloatingPointRange<Float>,
        vSpeed: ClosedFloatingPointRange<Float> = hSpeed,
        smootherMode: String,
        nonDataStartOffSlow: Boolean = false,
        nonDataSlowDownOnDirChange: Boolean = false,
        nonDataUseStraightLinePath: Boolean = false,
        nonDataMinRotationDifference: Float = 0f,
    ): Rotation {
        val firstSlow = rotationData?.startOffSlow == true || nonDataStartOffSlow
        val slowOnDirChange = rotationData?.slowDownOnDirChange == true || nonDataSlowDownOnDirChange
        val useStraightLine = rotationData?.useStraightLinePath == true || nonDataUseStraightLinePath
        val minRotDifference = rotationData?.minRotationDifference ?: nonDataMinRotationDifference

        return performAngleChange(
            currentRotation,
            targetRotation,
            hSpeed.random(),
            vSpeed.random(),
            firstSlow,
            useStraightLine,
            slowOnDirChange,
            minRotDifference,
            smootherMode,
        )
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
        vSpeed: Float, startFirstSlow: Boolean, useStraightLinePath: Boolean,
        slowDownOnDirChange: Boolean, minRotationDifference: Float, smootherMode: String,
    ): Rotation {
        var yawDifference = getAngleDifference(targetRotation.yaw, currentRotation.yaw)
        var pitchDifference = getAngleDifference(targetRotation.pitch, currentRotation.pitch)

        val yawTicks = ClientUtils.runTimeTicks - sameYawDiffTicks
        val pitchTicks = ClientUtils.runTimeTicks - samePitchDiffTicks

        val oldYawDiff = getAngleDifference(serverRotation.yaw, lastServerRotation.yaw)
        val oldPitchDiff = getAngleDifference(serverRotation.pitch, lastServerRotation.pitch)

        val secondOldYawDiff = getAngleDifference(lastServerRotation.yaw, secondLastRotation.yaw)
        val secondOldPitchDiff = getAngleDifference(lastServerRotation.pitch, secondLastRotation.pitch)

        if (rotationData?.simulateShortStop == true && Math.random() > 0.95 && yawDifference.sign == oldYawDiff.sign && secondOldYawDiff.sign == oldYawDiff.sign) {
            yawDifference = 0f
            pitchDifference = 0f
        }

        val rotationDifference = hypot(yawDifference, pitchDifference)

        val (hFactor, vFactor) = if (smootherMode == "Relative") {
            computeFactor(rotationDifference, hSpeed) to computeFactor(rotationDifference, vSpeed)
        } else {
            hSpeed to vSpeed
        }

        var straightLineYaw = if (useStraightLinePath) {
            abs(yawDifference safeDiv rotationDifference) * hFactor
        } else abs(yawDifference).coerceIn(-hFactor, hFactor)
        var straightLinePitch = if (useStraightLinePath) {
            abs(pitchDifference safeDiv rotationDifference) * vFactor
        } else abs(pitchDifference).coerceIn(-vFactor, vFactor)

        var (yawDirChange, pitchDirChange) = false to false

        straightLineYaw = applySlowDown(yawDifference.coerceIn(-straightLineYaw, straightLineYaw),
            oldYawDiff,
            secondOldYawDiff,
            yawTicks,
            startFirstSlow,
            slowDownOnDirChange,
            tickUpdate = { sameYawDiffTicks = ClientUtils.runTimeTicks }) { yawDirChange = true }
        straightLinePitch = applySlowDown(pitchDifference.coerceIn(-straightLinePitch, straightLinePitch),
            oldPitchDiff,
            secondOldPitchDiff,
            pitchTicks,
            startFirstSlow,
            slowDownOnDirChange,
            tickUpdate = { samePitchDiffTicks = ClientUtils.runTimeTicks }) { pitchDirChange = true }

        var coercedYaw = if (yawDirChange) {
            oldYawDiff * nextFloat(0f, 0.3f)
        } else yawDifference.coerceIn(-straightLineYaw, straightLineYaw)
        var coercedPitch = if (pitchDirChange) {
            oldPitchDiff * nextFloat(0f, 0.3f)
        } else pitchDifference.coerceIn(-straightLinePitch, straightLinePitch)

        val fixedSens = Rotation(coercedYaw, coercedPitch).fixedSensitivity()

        if (abs(fixedSens.yaw) <= nextFloat(min(minRotationDifference, getFixedAngleDelta()), minRotationDifference)) {
            coercedYaw = 0f
        }

        if (abs(fixedSens.pitch) < nextFloat(min(minRotationDifference, getFixedAngleDelta()), minRotationDifference)) {
            coercedPitch = 0f
        }

        return Rotation(currentRotation.yaw + coercedYaw, currentRotation.pitch + coercedPitch)
    }

    /**
     * Rotation slow down calculation, which simulates the humanistic rotation patterns stated below:
     *
     * - Starting off slow after not rotating.
     * - Starting off slow when changing directions.
     * - Slowing down before changing directions.
     *
     * Useful for top-notch anti-cheats.
     */
    private fun applySlowDown(
        newDiff: Float, oldDiff: Float, secondOldDiff: Float, ticks: Int, firstSlow: Boolean,
        slowDownOnDirChange: Boolean, tickUpdate: () -> Unit, onDirChange: () -> Unit,
    ): Float {
        val result = abs(oldDiff safeDiv newDiff)

        val shouldStartSlow = firstSlow && (oldDiff == 0f || ticks == 1) && newDiff != 0f

        val diffDir = oldDiff.sign != newDiff.sign && newDiff != 0f && oldDiff != 0f
        val secondDiffDir = secondOldDiff.sign != oldDiff.sign || abs(secondOldDiff) <= abs(oldDiff)

        val shouldSlowDownOnDirChange = slowDownOnDirChange && diffDir && secondDiffDir
        val shouldStartSlowAfterDirChange = slowDownOnDirChange && oldDiff.sign != newDiff.sign && !shouldSlowDownOnDirChange && newDiff != 0f

        // Have we not rotated the previous tick or have just changed directions and should start slow?
        val factor = if (shouldStartSlow || shouldStartSlowAfterDirChange) {
            if (oldDiff == 0f) {
                tickUpdate()
            }

            if (Rotations.debugRotations) {
                ClientUtils.displayChatMessage(if (shouldStartSlow) {
                    "STARTED OFF SLOW, TICKS SINCE LAST START: ${ticks}"
                } else "STARTED SLOW ON DIRECTION CHANGE, OLD DIFF: ${oldDiff}, SUPPOSED DIFF: $newDiff"
                )
            }

            val (min, max) = run {
                if (oldDiff != 0f && !shouldStartSlowAfterDirChange) {
                    0.2f to 0.3f
                } else 0f to 0.2f
            }

            (if (shouldStartSlow) result else 0f) + nextFloat(min, max)
        } else 1f

        if (!shouldStartSlow && !shouldStartSlowAfterDirChange && shouldSlowDownOnDirChange) {
            onDirChange()
            return newDiff
        }

        return abs(newDiff * factor)
    }

    private fun computeFactor(rotationDifference: Float, turnSpeed: Float): Float {
        return (rotationDifference / nextFloat(120f, 150f) * turnSpeed).coerceIn(nextFloat(0.5f, 1.5f), 180f)
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
    fun getVectorForRotation(yaw: Float, pitch: Float): Vec3d {
        val yawRad = yaw.toRadians()
        val pitchRad = pitch.toRadians()

        val f = MathHelper.cos(-yawRad - PI.toFloat())
        val f1 = MathHelper.sin(-yawRad - PI.toFloat())
        val f2 = -MathHelper.cos(-pitchRad)
        val f3 = MathHelper.sin(-pitchRad)

        return Vec3d((f1 * f2).toDouble(), f3.toDouble(), (f * f2).toDouble())
    }

    fun getVectorForRotation(rotation: Rotation) = getVectorForRotation(rotation.yaw, rotation.pitch)

    /**
     * Returns the inverted yaw angle.
     *
     * @param yaw The original yaw angle in degrees.
     * @return The yaw angle inverted by 180 degrees.
     */
    fun invertYaw(yaw: Float): Float {
        return (yaw + 180) % 360
    }

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
    fun isVisible(Vec3d: Vec3d) = mc.world.rayTrace(mc.player.eyes, Vec3d) == null

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
        slowDownOnDirChange: Boolean = false,
        useStraightLinePath: Boolean = false,
        minRotationDifference: Float = 0f,
    ) {
        if (rotation.yaw.isNaN() || rotation.pitch.isNaN() || rotation.pitch > 90 || rotation.pitch < -90) {
            return
        }

        if (!prioritizeRequest && rotationData?.prioritizeRequest == true) {
            return
        }

        if (applyClientSide) {
            currentRotation?.let {
                mc.player.yaw = it.yaw
                mc.player.pitch = it.pitch
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
            startOffSlow,
            slowDownOnDirChange,
            useStraightLinePath,
            minRotationDifference
        )

        this.resetTicks = if (applyClientSide) 1 else keepLength

        if (immediate) {
            update()
        }
    }

    private fun resetRotation() {
        resetTicks = 0
        currentRotation?.let { rotation ->
            mc.player?.let {
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
        reach: Float = mc.interactionManager.blockReachDistance,
    ): MovingObjectPosition? {
        val world = mc.world ?: return null
        val player = mc.player ?: return null

        val eyes = player.eyes

        return blockPos.getBlock()?.collisionRayTrace(
            world,
            blockPos,
            eyes,
            eyes + (getVectorForRotation(rotation) * reach.toDouble())
        )
    }

    fun performRayTrace(blockPos: BlockPos, vec: Vec3d, eyes: Vec3d = mc.player.eyes) =
        mc.world?.let { blockPos.getBlock()?.collisionRayTrace(it, blockPos, eyes, vec) }

    fun syncRotations() {
        val player = mc.player ?: return

        player.prevRotationYaw = player.yaw
        player.prevRotationPitch = player.pitch
        player.renderArmYaw = player.yaw
        player.renderArmPitch = player.pitch
        player.prevRenderArmYaw = player.yaw
        player.prevRotationPitch = player.pitch
    }

    private fun update() {
        val data = rotationData ?: return
        val player = mc.player ?: return

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

    /**
     * Handle rotation update
     */
    @EventTarget(priority = -1)
    fun onRotationUpdate(event: RotationUpdateEvent) {
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

            val yawDiff = getAngleDifference(packet.yaw, serverRotation.yaw)
            val pitchDiff = getAngleDifference(packet.pitch, serverRotation.pitch)

            if (Rotations.debugRotations) {
                ClientUtils.displayChatMessage("PREV YAW: $yawDiff, PREV PITCH: $pitchDiff")
            }
        }

        serverRotation = Rotation(packet.yaw, packet.pitch)
    }

    enum class SmootherMode(val modeName: String) { LINEAR("Linear"), RELATIVE("Relative") }

    data class RotationData(
        var hSpeed: ClosedFloatingPointRange<Float>, var vSpeed: ClosedFloatingPointRange<Float>,
        var smootherMode: SmootherMode, var strafe: Boolean, var strict: Boolean, var clientSide: Boolean,
        var immediate: Boolean, var resetThreshold: Float, val prioritizeRequest: Boolean,
        val simulateShortStop: Boolean, val startOffSlow: Boolean, val slowDownOnDirChange: Boolean,
        val useStraightLinePath: Boolean, val minRotationDifference: Float,
    )

    enum class BodyPoint(val rank: Int, val range: ClosedFloatingPointRange<Double>) {
        HEAD(1, 0.75..0.9),
        BODY(0, 0.5..0.75),
        FEET(-1, 0.1..0.4),
        UNKNOWN(-2, 0.0..0.0);

        companion object {
            fun fromString(point: String): BodyPoint {
                return values().find { it.name.equals(point, ignoreCase = true) } ?: UNKNOWN
            }
        }
    }

    fun coerceBodyPoint(point: BodyPoint, minPoint: BodyPoint, maxPoint: BodyPoint): BodyPoint {
        return when {
            point.rank < minPoint.rank -> minPoint
            point.rank > maxPoint.rank -> maxPoint
            else -> point
        }
    }

    /**
     * @return YESSSS!
     */
    override fun handleEvents() = true

}
