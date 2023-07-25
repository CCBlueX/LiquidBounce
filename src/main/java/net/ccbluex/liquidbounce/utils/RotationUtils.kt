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
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextInt
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.*
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
        targetRotation?.let { rotation ->
            if (keepLength > 0) {
                keepLength--
            } else {
                if (getRotationDifference(rotation, mc.thePlayer.rotation) <= angleThresholdForReset) {
                    resetRotation()
                } else {
                    val speed = RandomUtils.nextFloat(speedForReset.first, speedForReset.second)
                    targetRotation = limitAngleChange(rotation, mc.thePlayer.rotation, speed).fixedSensitivity()
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

        targetRotation?.let {
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

        targetRotation?.let {
            packet.yaw = it.yaw
            packet.pitch = it.pitch
        }

        serverRotation = Rotation(packet.yaw, packet.pitch)
    }

    /**
     * @return YESSSS!
     */
    override fun handleEvents() = true

    private var keepLength = 0

    var strafe = false
    var strict = false

    var speedForReset = 0f to 0f
    var angleThresholdForReset = 0f

    var targetRotation: Rotation? = null
    var serverRotation = Rotation(0f, 0f)

    var keepCurrentRotation = false

    private val random = Random()
    private var x = random.nextDouble()
    private var y = random.nextDouble()
    private var z = random.nextDouble()

    /**
     * Face block
     *
     * @param blockPos target block
     */
    fun faceBlock(blockPos: BlockPos?): VecRotation? {
        if (blockPos == null) return null
        var vecRotation: VecRotation? = null
        val eyesPos = mc.thePlayer.eyes
        val startPos = Vec3(blockPos)



        for (x in 0.1..0.9) {
            for (y in 0.1..0.9) {
                for (z in 0.1..0.9) {
                    val posVec = startPos.addVector(x, y, z)
                    val dist = eyesPos.distanceTo(posVec)

                    val (diffX, diffY, diffZ) = posVec - eyesPos
                    val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)

                    val rotation = Rotation(
                        MathHelper.wrapAngleTo180_float(atan2(diffZ, diffX).toDegreesF() - 90f),
                        MathHelper.wrapAngleTo180_float(-atan2(diffY, diffXZ).toDegreesF())
                    )

                    val rotationVector = getVectorForRotation(rotation)
                    val vector = eyesPos + (rotationVector * dist)

                    mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true)?.let {
                        if (it.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                            val currentVec = VecRotation(posVec, rotation)
                            if (vecRotation == null || getRotationDifference(currentVec.rotation) < getRotationDifference(
                                    vecRotation!!.rotation
                                )
                            ) vecRotation = currentVec
                        }
                    }
                }
            }
        }

        return vecRotation
    }

    /**
     * Face target with bow
     *
     * @param target      your enemy
     * @param silent      client side rotations
     * @param predict     predict new enemy position
     * @param predictSize predict size of predict
     */
    fun faceBow(target: Entity, silent: Boolean, predict: Boolean, predictSize: Float) {
        val player = mc.thePlayer

        val posX =
            target.posX + (if (predict) (target.posX - target.prevPosX) * predictSize else .0) - (player.posX + if (predict) player.posX - player.prevPosX else .0)
        val posY =
            target.entityBoundingBox.minY + (if (predict) (target.entityBoundingBox.minY - target.prevPosY) * predictSize else .0) + target.eyeHeight - 0.15 - (player.entityBoundingBox.minY + (if (predict) player.posY - player.prevPosY else .0)) - player.getEyeHeight()
        val posZ =
            target.posZ + (if (predict) (target.posZ - target.prevPosZ) * predictSize else .0) - (player.posZ + if (predict) player.posZ - player.prevPosZ else .0)
        val posSqrt = sqrt(posX * posX + posZ * posZ)

        var velocity = if (FastBow.state) 1f else player.itemInUseDuration / 20f
        velocity = min((velocity * velocity + velocity * 2) / 3, 1f)

        val rotation = Rotation(
            atan2(posZ, posX).toDegreesF() - 90f,
            atan((velocity * velocity - sqrt(velocity * velocity * velocity * velocity - 0.006f * (0.006f * posSqrt * posSqrt + 2 * posY * velocity * velocity))) / (0.006f * posSqrt)).toDegreesF()
        )
        if (silent) setTargetRotation(rotation)
        else limitAngleChange(
            player.rotation, rotation, 10f + nextInt(endExclusive = 6)
        ).toPlayer(mc.thePlayer)
    }

    /**
     * Translate vec to rotation
     *
     * @param vec     target vec
     * @param predict predict new location of your body
     * @return rotation
     */
    fun toRotation(vec: Vec3, predict: Boolean, fromEntity: Entity = mc.thePlayer): Rotation {
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
     * @param bb           enemy box
     * @param outborder    outborder option
     * @param random       random option
     * @param predict      predict option
     * @param throughWalls throughWalls option
     * @return center
     */
    fun searchCenter(
        bb: AxisAlignedBB, outborder: Boolean, random: Boolean, predict: Boolean, throughWalls: Boolean, distance: Float
    ): VecRotation? {
        if (outborder) {
            val vec3 = Vec3(
                bb.minX + (bb.maxX - bb.minX) * (x * 0.3 + 1.0),
                bb.minY + (bb.maxY - bb.minY) * (y * 0.3 + 1.0),
                bb.minZ + (bb.maxZ - bb.minZ) * (z * 0.3 + 1.0)
            )
            return VecRotation(vec3, toRotation(vec3, predict))
        }

        val randomVec = Vec3(
            bb.minX + (bb.maxX - bb.minX) * x, bb.minY + (bb.maxY - bb.minY) * y, bb.minZ + (bb.maxZ - bb.minZ) * z
        )

        val randomRotation = toRotation(randomVec, predict)

        val eyes = mc.thePlayer.eyes
        var vecRotation: VecRotation? = null

        if (random) {
            val dist = eyes.distanceTo(randomVec)

            if (dist <= distance && (throughWalls || isVisible(randomVec))) {
                return VecRotation(randomVec, randomRotation)
            }
        }

        for (x in 0.1..0.9) {
            for (y in 0.1..0.9) {
                for (z in 0.1..0.9) {
                    val vec = Vec3(
                        bb.minX + (bb.maxX - bb.minX) * x,
                        bb.minY + (bb.maxY - bb.minY) * y,
                        bb.minZ + (bb.maxZ - bb.minZ) * z
                    )

                    val rotation = toRotation(vec, predict)
                    val vecDist = eyes.distanceTo(vec)

                    if (vecDist <= distance) {
                        if (throughWalls || isVisible(vec)) {
                            val currentVec = VecRotation(vec, rotation)
                            val rotationToCompare = if (random) randomRotation else targetRotation ?: serverRotation

                            if (vecRotation == null || getRotationDifference(rotation, rotationToCompare)
                                < getRotationDifference(vecRotation.rotation, rotationToCompare)
                            ) vecRotation = currentVec
                        }
                    }
                }
            }
        }

        if (vecRotation == null) {
            val vec = getNearestPointBB(eyes, bb)
            val dist = eyes.distanceTo(vec)

            if (dist <= distance && (throughWalls || isVisible(vec))) {
                return VecRotation(vec, toRotation(vec, predict))
            }
        }

        return vecRotation
    }

    /**
     * Calculate difference between the client rotation and your entity
     *
     * @param entity your entity
     * @return difference between rotation
     */
    fun getRotationDifference(entity: Entity) = getRotationDifference(
        toRotation(getCenter(entity.hitBox), true), Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
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
    fun getVectorForRotation(rotation: Rotation): Vec3 {
        val f = MathHelper.cos(-rotation.yaw.toRadians() - PI.toFloat())
        val f1 = MathHelper.sin(-rotation.yaw.toRadians() - PI.toFloat())
        val f2 = -MathHelper.cos(-rotation.pitch.toRadians())
        val f3 = MathHelper.sin(-rotation.pitch.toRadians())
        return Vec3((f1 * f2).toDouble(), f3.toDouble(), (f * f2).toDouble())
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
        blockReachDistance, rotation.yaw, rotation.pitch
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
        resetSpeed: Pair<Float, Float> = 180f to 180f,
        angleThresholdForReset: Float = 180f
    ) {
        if (rotation.yaw.isNaN() || rotation.pitch.isNaN() || rotation.pitch > 90 || rotation.pitch < -90) return

        targetRotation = rotation.fixedSensitivity()

        this.strafe = strafe
        this.strict = strict
        this.keepLength = keepLength
        this.speedForReset = resetSpeed
        this.angleThresholdForReset = angleThresholdForReset
    }

    fun resetRotation() {
        keepLength = 0
        targetRotation?.let { rotation ->
            mc.thePlayer?.let {
                it.rotationYaw = rotation.yaw + getAngleDifference(it.rotationYaw, rotation.yaw)
                it.renderArmYaw = it.rotationYaw
                it.prevRenderArmYaw = it.rotationYaw
            }
        }
        targetRotation = null
        strafe = false
        strict = false
    }

    /**
     * Returns smallest angle difference possible with a specific sensitivity ("gcd")
     */
    fun getFixedAngleDelta(sensitivity: Float = mc.gameSettings.mouseSensitivity) =
        (sensitivity * 0.6f + 0.2f).pow(3) * 1.2f

    /**
     * Returns angle that is legitimately accomplishable with player's current sensitivity
     */
    fun getFixedSensitivityAngle(targetAngle: Float, startAngle: Float = 0f, gcd: Float = getFixedAngleDelta()) =
        startAngle + ((targetAngle - startAngle) / gcd).roundToInt() * gcd
}
