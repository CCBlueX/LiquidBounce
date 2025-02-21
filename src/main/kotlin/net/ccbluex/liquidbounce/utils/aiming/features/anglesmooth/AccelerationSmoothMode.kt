package net.ccbluex.liquidbounce.utils.aiming.features.anglesmooth

import it.unimi.dsi.fastutil.floats.FloatFloatPair
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationDelta
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.aiming.utils.facingEnemy
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.entity.Entity
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max

class AccelerationSmoothMode(override val parent: ChoiceConfigurable<*>) : AngleSmoothMode("Acceleration") {

    private val yawAcceleration by floatRange("YawAcceleration", 20f..25f, 1f..180f)
    private val pitchAcceleration by floatRange("PitchAccelelation", 20f..25f, 1f..180f)

    private inner class DynamicAccel : ToggleableConfigurable(this, "DynamicAccel", false) {
        val coefDistance by float("CoefDistance", -1.393f, -2f..2f)
        val yawCrosshairAccel by floatRange("YawCrosshairAccel", 17f..20f, 1f..180f)
        val pitchCrosshairAccel by floatRange("PitchCrosshairAccel", 17f..20f, 1f..180f)
    }

    private inner class AccelerationError : ToggleableConfigurable(this, "AccelerationError", true) {
        val yawAccelerationError by float("YawAccelError", 0.1f, 0.01f..1f)
        val pitchAccelerationError by float("PitchAccelError", 0.1f, 0.01f..1f)
    }

    private inner class ConstantError : ToggleableConfigurable(this, "ConstantError", true) {
        val yawConstantError by float("YawConstantError", 0.1f, 0.01f..1f)
        val pitchConstantError by float("PitchConstantError", 0.1f, 0.01f..1f)
    }


    // compute a sigmoid-like deceleration factor
    private inner class SigmoidDeceleration : ToggleableConfigurable(this, "SigmoidDeceleration", false) {
        val steepness by float("Steepness", 10f, 0.0f..20f)
        val midpoint by float("Midpoint", 0.3f, 0.0f..1.0f)

        fun computeDecelerationFactor(rotationDifference: Float): Float {
            val scaledDifference = rotationDifference / 120f
            val sigmoid = 1 / (1 + exp((-steepness * (scaledDifference - midpoint)).toDouble()))

            return sigmoid.toFloat()
                .coerceAtLeast(0f)
                .coerceAtMost(180f)
        }
    }

    private val dynamicAcceleration = tree(DynamicAccel())
    private val accelerationError = tree(AccelerationError())
    private val constantError = tree(ConstantError())
    private val sigmoidDeceleration = tree(SigmoidDeceleration())

    private val errorProviders: Pair<ErrorProvider, ErrorProvider>
        get() {
            val accelerationError = accelerationError.takeIf { accelerationError.enabled }
            val constantError = constantError.takeIf { constantError.enabled }

            val yawAccelerationError = accelerationError?.yawAccelerationError ?: 0.0F
            val pitchAccelerationError = accelerationError?.pitchAccelerationError ?: 0.0F
            val yawConstantError = constantError?.yawConstantError ?: 0.0F
            val pitchConstantError = constantError?.pitchConstantError ?: 0.0F

            val providerForYaw = ErrorProvider(
                accelerationErrorRange = -yawAccelerationError..yawAccelerationError,
                constantErrorRange = -yawConstantError..yawConstantError,
            )
            val providerForPitch = ErrorProvider(
                accelerationErrorRange = -pitchAccelerationError..pitchAccelerationError,
                constantErrorRange = -pitchConstantError..pitchConstantError,
            )

            return providerForYaw to providerForPitch
        }

    private class ErrorProvider(
        private val accelerationErrorRange: ClosedFloatingPointRange<Float>,
        private val constantErrorRange: ClosedFloatingPointRange<Float>,
    ) {
        fun getError(acceleration: Float): Float {
            val currentAccelerationError = this.accelerationErrorRange.random().toFloat()
            val currentConstantError = this.constantErrorRange.random().toFloat()

            return acceleration * currentAccelerationError + currentConstantError
        }
    }

    override fun limitAngleChange(
        factorModifier: Float,
        currentRotation: Rotation,
        targetRotation: Rotation,
        vec3d: Vec3d?,
        entity: Entity?
    ): Rotation {
        val prevRotation = RotationManager.previousRotation ?: player.lastRotation

        val prevDiff = prevRotation.rotationDeltaTo(currentRotation)
        val diff = currentRotation.rotationDeltaTo(targetRotation)

        val distance = vec3d?.distanceTo(player.pos) ?: 0.0
        val crosshair = entity?.let { facingEnemy(entity, max(3.0, distance), currentRotation) } ?: false

        val (newYawDiff, newPitchDiff) = computeTurnSpeed(
            prevDiff,
            diff,
            crosshair,
            distance
        )

        return Rotation(
            currentRotation.yaw + newYawDiff,
            currentRotation.pitch + newPitchDiff
        )
    }

    override fun howLongToReach(currentRotation: Rotation, targetRotation: Rotation): Int {
        val prevRotation = RotationManager.previousRotation ?: player.lastRotation
        val prevDiff = prevRotation.rotationDeltaTo(currentRotation)
        val diff = currentRotation.rotationDeltaTo(targetRotation)

        // Check if we are already on target
        if (MathHelper.approximatelyEquals(diff.deltaYaw, 0f) &&
            MathHelper.approximatelyEquals(diff.deltaPitch, 0f)) {
            return 0
        }

        val (newYawDiff, newPitchDiff) = computeTurnSpeed(
            prevDiff,
            diff,
            false,
            0.0
        )

        // Check if we are already on target
        if (MathHelper.approximatelyEquals(newYawDiff, 0f) &&
            MathHelper.approximatelyEquals(newPitchDiff, 0f) ||
            abs(diff.deltaYaw) < abs(newYawDiff) &&
            abs(diff.deltaPitch) < abs(newPitchDiff)) {
            return 0
        }

        val ticksH = floor(abs(diff.deltaYaw) / abs(newYawDiff))
        val ticksV = floor(abs(diff.deltaPitch) / abs(newPitchDiff))

        // Check if ticksH or ticksV are NaN
        if (ticksH.isNaN() || ticksV.isNaN()) {
            return 0
        }

        return max(ticksH, ticksV).toInt()
    }

    @Suppress("LongParameterList", "CognitiveComplexMethod")
    private fun computeTurnSpeed(
        prevDiff: RotationDelta,
        diff: RotationDelta,
        crosshair: Boolean,
        distance: Double
    ): FloatFloatPair {
        val decelerationFactor = sigmoidDeceleration.computeDecelerationFactor(diff.length())
            .takeIf { sigmoidDeceleration.enabled } ?: 1.0F

        val crosshairCheck = dynamicAcceleration.enabled && crosshair
        val distanceFactor = (dynamicAcceleration.coefDistance * distance).toFloat()

        val (yawErrorProvider, pitchErrorProvider) = this.errorProviders

        val (aYaw, aPitch) = if (crosshairCheck) {
            dynamicAcceleration.yawCrosshairAccel to dynamicAcceleration.pitchCrosshairAccel
        } else {
            yawAcceleration to pitchAcceleration
        }

        val (accRangeYaw, accRangePitch) = Pair(
            -aYaw.random().toFloat() + distanceFactor..aYaw.random().toFloat() + distanceFactor,
            -aPitch.random().toFloat() + distanceFactor..aPitch.random().toFloat() + distanceFactor
        )

        val yawAccel = calculateAcceleration(diff.deltaYaw, prevDiff.deltaYaw, accRangeYaw, decelerationFactor)
        val pitchAccel = calculateAcceleration(diff.deltaPitch, prevDiff.deltaPitch, accRangePitch, decelerationFactor)

        return FloatFloatPair.of(
            prevDiff.deltaYaw + yawAccel + yawErrorProvider.getError(yawAccel),
            prevDiff.deltaPitch + pitchAccel + pitchErrorProvider.getError(yawAccel)
        )
    }

    private fun calculateAcceleration(
        yawDiff: Float,
        prevYawDiff: Float,
        dynamicYawAccel: ClosedFloatingPointRange<Float>,
        yawDecelerationFactor: Float
    ) = RotationUtil.angleDifference(yawDiff, prevYawDiff)
        .coerceIn(dynamicYawAccel) *
        yawDecelerationFactor
}
