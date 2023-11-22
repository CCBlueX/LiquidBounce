package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.absoluteValue

class LegitAimpointTracker(
    private val trackerConfigurable: LegitAimpointTrackerConfigurable,
) {
    private val random = Random()

    private var currentOffset: Vec3d = Vec3d.ZERO

    /**
     * Linear ease until bezier functionality is implemented
     */
    fun ease(time: Double): Double {
        return time
    }

    class AimSpot(
        val aimSpot: Vec3d,
    )

    fun nextPoint(
        box: Box,
        target: Vec3d,
        pointMovementInLastTick: Vec3d,
    ): AimSpot {
        this.updateCurrentOffset()

        val aimPointFac = 1.0 - ease((target.distanceTo(mc.player!!.eyes) / 6.5).coerceAtMost(1.0))

        val optimalTarget = box.center.add(0.0, (box.maxY - box.minY) * 0.25, 0.0)

        val off =
            this.currentOffset
                .multiply(
                    this.trackerConfigurable.horizontalStdPerVelocity.toDouble(),
                    this.trackerConfigurable.verticalStdPerVelocity.toDouble(),
                    this.trackerConfigurable.horizontalStdPerVelocity.toDouble(),
                )
                .multiply(
                    pointMovementInLastTick.horizontalLength(),
                    pointMovementInLastTick.y.absoluteValue,
                    pointMovementInLastTick.horizontalLength(),
                )
        var currentPoint = (target + (optimalTarget - target) * aimPointFac)
        currentPoint += off
        currentPoint -= pointMovementInLastTick * this.trackerConfigurable.aimBehindFactor.toDouble()

        return AimSpot(currentPoint)
    }

    private fun updateCurrentOffset() {
        val nextOffset =
            this.currentOffset.add(
                this.random.nextGaussian(),
                this.random.nextGaussian(),
                this.random.nextGaussian(),
            )

        this.currentOffset = nextOffset.multiply(0.9, 0.9, 0.9)
    }

    class LegitAimpointTrackerConfigurable(module: Module)
        : ToggleableConfigurable(module, "SimulateLegitAiming", true) {
        val horizontalStdPerVelocity by float("HorizontalDerive", 0.25F, 0.1F..10.0F)
        val verticalStdPerVelocity by float("VerticalDerive", 0.25F, 0.1F..10.0F)
        val aimBehindFactor by float("AimBehindFactor", 0.4F, 0.1F..1.0F)
    }
}
