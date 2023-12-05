/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.combat.FastBow
import net.ccbluex.liquidbounce.utils.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.*
import org.apache.commons.lang3.tuple.MutableTriple
import java.util.*
import kotlin.math.*

object RotationUtils : MinecraftInstance(), Listenable {

    /**
     * Handle minecraft tick
     *
     * @param event Update event
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        currentRotation?.let { rotation ->
            setbackRotation?.let {
                if (it.middle) {
                    currentRotation = it.left
                    it.setMiddle(false)
                    return
                }

                val sign = if (random.nextBoolean()) 1 else -1

                rotation.yaw = (it.right.yaw + 0.000001f * sign) % 360f
                rotation.pitch = (it.right.pitch + 0.000001f * sign) % 360f
                setbackRotation = null
                return
            }

            if (keepLength > 0) {
                keepLength--
            } else {
                if (getRotationDifference(rotation, mc.thePlayer.rotation) <= angleThresholdForReset) {
                    resetRotation()
                } else {
                    val speed = RandomUtils.nextFloat(speedForReset.first, speedForReset.second)
                    currentRotation = limitAngleChange(rotation, mc.thePlayer.rotation, speed).fixedSensitivity()
                }
            }
        }

        if (random.nextGaussian() > 0.8) x = Math.random()
        if (random.nextGaussian() > 0.8) y = Math.random()
        if (random.nextGaussian() > 0.8) z = Math.random()
    }

    /**
     * Handle strafing
     */
    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (!strafe) {
            return
        }

        currentRotation?.let {
            it.applyStrafeToPlayer(event, strict)
            event.cancelEvent()
        }
    }

    /**
     * Handle packet
     *
     * @param event Packet Event
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
        }

        serverRotation = Rotation(packet.yaw, packet.pitch)
    }

    /**
     * @return YESSSS!
     */
    override fun handleEvents() = true

    var keepLength = 0

    var strafe = false
    var strict = false

    var speedForReset = 0f to 0f
    var angleThresholdForReset = 0f

    var currentRotation: Rotation? = null
    var serverRotation = Rotation(0f, 0f)

    // (The priority, the active check and the original rotation)
    var setbackRotation: MutableTriple<Rotation, Boolean, Rotation>? = null

    private val random = Random()
    private var x = random.nextDouble()
    private var y = random.nextDouble()
    private var z = random.nextDouble()

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

                    val posVec = startPos.addVector(
                        block.blockBoundsMinX + (block.blockBoundsMaxX - block.blockBoundsMinX) * x,
                        block.blockBoundsMinY + (block.blockBoundsMaxY - block.blockBoundsMinY) * x,
                        block.blockBoundsMinZ + (block.blockBoundsMaxZ - block.blockBoundsMinZ) * x
                    )

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
    fun faceTrajectory(target: Entity, predict: Boolean, predictSize: Float, gravity: Float = 0.05f, velocity: Float? = null): Rotation {
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
            -atan((velocity * velocity - sqrt(velocity * velocity * velocity * velocity - gravityModifier * (gravityModifier * posSqrt * posSqrt + 2 * posY * velocity * velocity))) / (gravityModifier * posSqrt)).toDegreesF()
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
     * Get the center of a box
     *
     * @param bb your box
     * @return center of box
     */
    fun getCenter(bb: AxisAlignedBB) = Vec3(
        (bb.minX + bb.maxX) * 0.5, (bb.minY + bb.maxY) * 0.5, (bb.minZ + bb.maxZ) * 0.5
    )

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
        bb: AxisAlignedBB, outborder: Boolean, random: Boolean, predict: Boolean,
        lookRange: Float, attackRange: Float, throughWallsRange: Float = 0f
    ): Rotation? {
        val lookRange = lookRange.coerceAtLeast(attackRange)

        if (outborder) {
            val vec3 = Vec3(
                bb.minX + (bb.maxX - bb.minX) * (x * 0.3 + 1.0),
                bb.minY + (bb.maxY - bb.minY) * (y * 0.3 + 1.0),
                bb.minZ + (bb.maxZ - bb.minZ) * (z * 0.3 + 1.0)
            )

            return toRotation(vec3, predict).fixedSensitivity()
        }

        val randomVec = Vec3(
            bb.minX + (bb.maxX - bb.minX) * x,
            bb.minY + (bb.maxY - bb.minY) * y,
            bb.minZ + (bb.maxZ - bb.minZ) * z
        )

        val randomRotation = toRotation(randomVec, predict).fixedSensitivity()

        val eyes = mc.thePlayer.eyes

        // TODO: Doesn't have GCD treatment as rotations below, but it is random rotation, what do you expect?
        if (random) {
            val dist = eyes.distanceTo(randomVec)

            if (dist <= attackRange && (dist <= throughWallsRange || isVisible(randomVec)))
                return randomRotation
        }

        var attackRotation: Pair<Rotation, Float>? = null
        var lookRotation: Pair<Rotation, Float>? = null

        val rotationToCompareTo = if (random) randomRotation else currentRotation ?: serverRotation

        for (x in 0.0..1.0) {
            for (y in 0.0..1.0) {
                for (z in 0.0..1.0) {
                    val vec = Vec3(
                        bb.minX + (bb.maxX - bb.minX) * x,
                        bb.minY + (bb.maxY - bb.minY) * y,
                        bb.minZ + (bb.maxZ - bb.minZ) * z
                    )

                    val rotation = toRotation(vec, predict).fixedSensitivity()

                    // Calculate actual hit vec after applying fixed sensitivity to rotation
                    val gcdVec =
                        bb.calculateIntercept(eyes, eyes + getVectorForRotation(rotation) * lookRange.toDouble())?.hitVec
                            ?: continue

                    val distance = eyes.distanceTo(gcdVec)

                    // Check if vec is in range
                    // Skip if a rotation that is in attack range was already found and the vec is out of attack range
                    if (distance > lookRange || (attackRotation != null && distance > attackRange))
                        continue

                    // Check if vec is reachable through walls
                    if (!isVisible(gcdVec) && distance > throughWallsRange)
                        continue

                    val rotationWithDiff = rotation to getRotationDifference(rotation, rotationToCompareTo)

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
        getRotationDifference(
            toRotation(getCenter(entity.hitBox), true),
            mc.thePlayer.rotation
        )

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
     * @return limited rotation
     */
    fun limitAngleChange(currentRotation: Rotation, targetRotation: Rotation, turnSpeed: Float) = Rotation(
        currentRotation.yaw + getAngleDifference(targetRotation.yaw, currentRotation.yaw).coerceIn(
            -turnSpeed, turnSpeed
        ), currentRotation.pitch + getAngleDifference(targetRotation.pitch, currentRotation.pitch).coerceIn(
            -turnSpeed, turnSpeed
        )
    )

    /**
     * Calculate difference between two angle points
     *
     * @param a angle point
     * @param b angle point
     * @return difference between angle points
     */
    fun getAngleDifference(a: Float, b: Float) = ((a - b) % 360f + 540f) % 360f - 180f

    /**
     * Calculate rotation to vector
     *
     * @param rotation your rotation
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
    fun isRotationFaced(targetEntity: Entity, blockReachDistance: Double, rotation: Rotation) =
        raycastEntity(blockReachDistance, rotation.yaw, rotation.pitch)
            { entity: Entity -> targetEntity == entity } != null

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
        resetSpeed: Pair<Float, Float> = 180f to 180f,
        angleThresholdForReset: Float = 180f
    ) {
        if (rotation.yaw.isNaN() || rotation.pitch.isNaN() || rotation.pitch > 90 || rotation.pitch < -90) return

        currentRotation = rotation.fixedSensitivity()

        this.strafe = strafe
        this.strict = strict
        this.keepLength = keepLength
        this.speedForReset = resetSpeed
        this.angleThresholdForReset = angleThresholdForReset
    }

    fun resetRotation() {
        keepLength = 0
        currentRotation?.let { rotation ->
            mc.thePlayer?.let {
                it.rotationYaw = rotation.yaw + getAngleDifference(it.rotationYaw, rotation.yaw)
                syncRotations()
            }
        }
        currentRotation = null
        strafe = false
        strict = false
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
        reach: Float = mc.playerController.blockReachDistance
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
        mc.theWorld?.let {
            blockPos.getBlock()?.collisionRayTrace(it, blockPos, eyes, vec)
        }

    fun syncRotations() {
        val player = mc.thePlayer ?: return

        player.prevRotationYaw = player.rotationYaw
        player.prevRotationPitch = player.rotationPitch
        player.renderArmYaw = player.rotationYaw
        player.renderArmPitch = player.rotationPitch
        player.prevRenderArmYaw = player.rotationYaw
        player.prevRotationPitch = player.rotationPitch
    }
}
