package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ElytraRotationProcessor.ignoreKillAura
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.aiming.features.processors.RotationProcessor
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val BASE_YAW_SPEED = 45.0f
private const val BASE_PITCH_SPEED = 35.0f
private const val IDEAL_DISTANCE = 10

@Suppress("MagicNumber")
internal object ElytraRotationProcessor : Configurable("Rotations"), RotationProcessor, EventListener {
    private val sharpRotations by boolean("Sharp", false)
    internal val ignoreKillAura by boolean("IgnoreKillAuraRotation", true)
    internal val look by boolean("Look", false)
    private val autoDistance by boolean("AutoDistance", true)
    private val prediction = tree(TargetEntityMovementPrediction)
    private val rotateAt by enumChoice("RotateAt", TargetRotatePosition.EYES)

    override val running: Boolean
        get() = super.running && ModuleElytraTarget.target != null

    override fun parent() = ModuleElytraTarget

    private inline val baseYawSpeed: Float get() = if (sharpRotations) {
        BASE_YAW_SPEED * 1.5f
    } else {
        BASE_YAW_SPEED
    }

    private inline val basePitchSpeed: Float get() = if (sharpRotations) {
        BASE_PITCH_SPEED * 1.5f
    } else {
        BASE_PITCH_SPEED
    }

    private inline val randomDirectionVector
        get() = with (System.currentTimeMillis() / 1000.0) {
            Vec3d(
                sin(this * 1.8) * 0.04 + (Math.random() - 0.5) * 0.02,
                sin(this * 2.2) * 0.03 + (Math.random() - 0.5) * 0.015,
                cos(this * 1.8) * 0.04 + (Math.random() - 0.5) * 0.02,
            )
        }

    /**
     * Adaptively smooths the angle, but at the same time
     * Allows it to work well with [ModuleKillAura] and [ignoreKillAura].
     *
     * Please do not use this ANYWHERE ELSE
     * This is only for [ModuleElytraTarget]
     */
    override fun process(
        rotationTarget: RotationTarget,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Rotation {
        val delta = currentRotation.rotationDeltaTo(targetRotation)

        val (deltaYaw, deltaPitch) = delta
        val difference = delta.length()

        val currentTime = System.currentTimeMillis()

        val shouldBoost = sin(currentTime / 300.0) > 0.8
        val isTargetBehind = abs(deltaYaw) > 90.0f

        val speedMultiplier = if (shouldBoost) {
            2.0f
        } else {
            1.2f
        }

        val smoothBoost = if (shouldBoost) {
            (sin((currentTime % 360) / 300.0f * Math.PI) * 0.8f + 1.2f).toFloat()
        } else {
            1.2f
        }

        val backTargetMultiplier = if (isTargetBehind) {
            (2.2f * sin(currentTime / 150.0) * 0.2 + 1.0).toFloat()
        } else {
            1.2f
        }

        val speed = speedMultiplier * smoothBoost

        val yawSpeed = baseYawSpeed * speed * backTargetMultiplier
        val pitchSpeed = basePitchSpeed * speed

        val microAdjustment = (sin(currentTime / 80.0) * 0.08 + cos(currentTime / 120.0) * 0.05).toFloat()

        var moveYaw = MathHelper.clamp(deltaYaw, -yawSpeed, yawSpeed)
        var movePitch = MathHelper.clamp(deltaPitch, -pitchSpeed, pitchSpeed)

        if (difference < 5.0f) {
            moveYaw += microAdjustment * 0.2f
            movePitch += microAdjustment * 0.8f
        }

        return Rotation(
            currentRotation.yaw + moveYaw,
            MathHelper.clamp(currentRotation.pitch + movePitch, -90.0f, 90.0f),
        )
    }

    @Suppress("unused")
    private val rotationsUpdate = handler<RotationUpdateEvent> {
        val target = ModuleElytraTarget.target ?: return@handler

        val correction = if (look) {
            MovementCorrection.CHANGE_LOOK
        } else {
            MovementCorrection.STRICT
        }

        calculateRotation(target).let {
            RotationManager.setRotationTarget(
                /*
                 * Don't use the RotationConfigurable because I need to superfast rotations.
                 * Without any setting and angle smoothing
                 */
                plan = RotationTarget(
                    rotation = it,
                    entity = target,
                    processors = listOfNotNull(ElytraRotationProcessor),
                    ticksUntilReset = 1,
                    resetThreshold = 1f,
                    considerInventory = true,
                    movementCorrection = correction
                ),
                priority = Priority.IMPORTANT_FOR_USAGE_3,
                provider = ModuleElytraTarget
            )
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun calculateRotation(target: LivingEntity): Rotation {
        var targetPos = prediction.predictPosition(target, rotateAt.position(target)) + randomDirectionVector * 4.0

        if (autoDistance) {
            val direction = (targetPos - player.pos).normalize()
            val distance = player.pos.squaredDistanceTo(direction)

            if (distance < IDEAL_DISTANCE * IDEAL_DISTANCE) {
                targetPos -= direction * (IDEAL_DISTANCE - distance)
            }
        }

        return Rotation.lookingAt(targetPos, player.pos)
    }
}
