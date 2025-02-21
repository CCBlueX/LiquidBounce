/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.clicking.Clicker.Companion.RNG
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.security.SecureRandom
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PointTracker(
    highestPointDefault: PreferredBoxPart = PreferredBoxPart.HEAD,
    lowestPointDefault: PreferredBoxPart = PreferredBoxPart.BODY,
    timeEnemyOffsetDefault: Float = 0.4f,
    timeEnemyOffsetScale: ClosedFloatingPointRange<Float> = -1f..1f
) : Configurable("AimPoint", aliases = arrayOf("PointTracker")), EventListener {

    companion object {

        /**
         * The gaussian distribution values for the offset.
         */
        private const val STDDEV_Z = 0.24453708645460387
        private const val MEAN_X = 0.00942273861037109
        private const val STDDEV_X = 0.23319837528201348
        private const val MEAN_Y = -0.30075078007595923
        private const val STDDEV_Y = 0.3492437109081718
        private const val MEAN_Z = 0.013282929419023442

    }

    /**
     * Define the highest and lowest point of the box we want to aim at.
     */
    private val highestPoint: PreferredBoxPart by enumChoice("HighestPoint", highestPointDefault)
        .onChange { new ->
            if (lowestPoint.isHigherThan(new)) {
                lowestPoint
            } else {
                new
            }
        }
    private val lowestPoint: PreferredBoxPart by enumChoice("LowestPoint", lowestPointDefault)
        .onChange { new ->
            if (new.isHigherThan(highestPoint)) {
                highestPoint
            } else {
                new
            }
        }

    private val preferredBoxPoint by enumChoice("BoxPoint", PreferredBoxPoint.STRAIGHT)

    /**
     * The time offset defines a prediction or rather a delay of the point tracker.
     * We can either try to predict the next location of the player and use this as our newest point, or
     * we pretend to be slow in the head and aim behind.
     */
    private val timeEnemyOffset by float("TimeEnemyOffset", timeEnemyOffsetDefault, timeEnemyOffsetScale)

    private inner class Gaussian : ToggleableConfigurable(this, "Gaussian", false) {

        var currentOffset: Vec3d = Vec3d.ZERO
        private var targetOffset: Vec3d = Vec3d.ZERO

        val yawFactor by floatRange("YawOffset", 0f..0f, 0.0f..1.0f)
        val pitchFactor by floatRange("PitchOffset", 0f..0f, 0.0f..1.0f)
        val chance by int("Chance", 100, 0..100, "%")
        val speed by floatRange("Speed", 0.1f..0.2f, 0.01f..1f)
        val tolerance by float("Tolerance", 0.05f, 0.01f..0.1f)

        private inner class Dynamic : ToggleableConfigurable(this, "Dynamic", false) {
            val hurtTime by int("HurtTime", 10, 0..10)
            val yawFactor by float("YawFactor", 0f, 0f..10f, "x")
            val pitchFactor by float("PitchFactor", 0f, 0f..10f, "x")
            val speed by floatRange("Speed", 0.5f..0.75f, 0.01f..1f)
            val tolerance by float("Tolerance", 0.1f, 0.01f..0.1f)
        }

        private val dynamic = tree(Dynamic())

        private val random = SecureRandom()

        private fun interpolate(start: Double, end: Double, f: Double) = start + (end - start) * f

        fun factorCheck(): Boolean {
            return yawFactor.random() > 0.0f && pitchFactor.random() > 0.0f && chance > 0
        }

        private fun gaussianHasReachedTarget(vec1: Vec3d, vec2: Vec3d, tolerance: Float): Boolean {
            return abs(vec1.x - vec2.x) < tolerance &&
                abs(vec1.y - vec2.y) < tolerance &&
                abs(vec1.z - vec2.z) < tolerance
        }

        @Suppress("CognitiveComplexMethod")
        fun updateGaussianOffset(entity: Any?) {
            val dynamicCheck = dynamic.enabled && entity is LivingEntity && entity.hurtTime >= dynamic.hurtTime

            val yawFactor =
                if (dynamicCheck && dynamic.yawFactor > 0f) {
                    (yawFactor.random() + player.sqrtSpeed * dynamic.yawFactor)
                } else {
                    yawFactor.random()
                }

            val pitchFactor =
                if (dynamicCheck && dynamic.pitchFactor > 0f) {
                    (pitchFactor.random() + player.sqrtSpeed * dynamic.pitchFactor)
                } else {
                    pitchFactor.random()
                }

            if (gaussianHasReachedTarget(
                    currentOffset,
                    targetOffset,
                    if (dynamicCheck) dynamic.tolerance else tolerance
                )
            ) {
                if (random.nextInt(100) <= chance) {
                    targetOffset = Vec3d(
                        random.nextGaussian(MEAN_X, STDDEV_X) * yawFactor,
                        random.nextGaussian(MEAN_Y, STDDEV_Y) * pitchFactor,
                        random.nextGaussian(MEAN_Z, STDDEV_Z) * yawFactor
                    )
                }
            } else {
                currentOffset = Vec3d(
                    interpolate(
                        currentOffset.x,
                        targetOffset.x,
                        if (dynamicCheck) dynamic.speed.random() else speed.random()
                    ),
                    interpolate(
                        currentOffset.y,
                        targetOffset.y,
                        if (dynamicCheck) dynamic.speed.random() else speed.random()
                    ),
                    interpolate(
                        currentOffset.z,
                        targetOffset.z,
                        if (dynamicCheck) dynamic.speed.random() else speed.random()
                    )
                )
            }
        }

    }

    /**
     * This introduces a layer of randomness to the point tracker. A gaussian distribution is being used to
     * calculate the offset.
     */
    private val gaussian = tree(Gaussian())

    /**
     * OutOfBox will set the box offset to an unreachable position.
     */
    private val outOfBox by boolean("OutOfBox", false)

    /**
     * The shrink box value will shrink the cut-off box by the given amount.
     */
    private val shrinkBox by float("ShrinkBox", 0.05f, 0.0f..0.3f)
    private val dynamicShrinkBox by boolean("DynamicShrinkBox", true)

    enum class PreferredBoxPart(override val choiceName: String, val cutOff: (Box) -> Double) : NamedChoice {
        HEAD("Head", { box -> box.maxY }),
        BODY("Body", { box -> box.center.y }),
        FEET("Feet", { box -> box.minY });

        /**
         * Check if this part of the box is higher than the other by the index of the enum.
         * So please DO NOT change the order of the enum.
         */
        fun isHigherThan(other: PreferredBoxPart) = entries.indexOf(this) < entries.indexOf(other)

    }

    @Suppress("unused")
    enum class PreferredBoxPoint(override val choiceName: String, val point: (Box, Vec3d) -> Vec3d) : NamedChoice {
        CLOSEST("Closest", { box, eyes ->
            Vec3d(
                eyes.x.coerceAtMost(box.maxX).coerceAtLeast(box.minX),
                eyes.y.coerceAtMost(box.maxY).coerceAtLeast(box.minY),
                eyes.z.coerceAtMost(box.maxZ).coerceAtLeast(box.minZ)
            )
        }),
        ASSIST("Assist", { box, eyes ->
            val vec3 = eyes + player.rotation.directionVector

            Vec3d(
                vec3.x.coerceAtMost(box.maxX).coerceAtLeast(box.minX),
                vec3.y.coerceAtMost(box.maxY).coerceAtLeast(box.minY),
                vec3.z.coerceAtMost(box.maxZ).coerceAtLeast(box.minZ)
            )
        }),
        STRAIGHT("Straight", { box, eyes ->
            Vec3d(
                box.center.x,
                eyes.y.coerceAtMost(box.maxY).coerceAtLeast(box.minY),
                box.center.z
            )
        }),
        CENTER("Center", { box, _ -> box.center }),
        RANDOM("Random", { box, _ ->
            Vec3d(
                RNG.nextDouble(box.minX, box.maxX),
                RNG.nextDouble(box.minY, box.maxY),
                RNG.nextDouble(box.minZ, box.maxZ)
            )
        }),
        RANDOM_CENTER("RandomCenter", { box, _ ->
            Vec3d(
                RNG.nextDouble(box.minX, box.maxX),
                box.center.y,
                RNG.nextDouble(box.minZ, box.maxZ)
            )
        });
    }

    /**
     * The point tracker is being used to track a certain point of an entity.
     *
     * @param entity The entity we want to track.
     */
    fun gatherPoint(entity: LivingEntity, situation: AimSituation): Point {
        val playerPosition = player.pos
        val playerEyes = player.eyePos
        val positionDifference = playerPosition.y - entity.pos.y

        // Predicted target position of the enemy
        val targetVelocity = entity.pos.subtract(entity.prevPos)
        var box = entity.box.offset(targetVelocity.multiply(timeEnemyOffset.toDouble()))
        if (!situation.isNear && outOfBox) {
            box = box.withMinY(box.maxY).withMaxY(box.maxY + 1.0)
        }

        val highest = (highestPoint.cutOff(box) + positionDifference)
            .coerceAtMost(box.maxY)
            .coerceAtLeast(box.minY + 1.0)
        val lowest = (lowestPoint.cutOff(box) + positionDifference)
            .coerceAtMost(box.maxY - 1.0)
            .coerceAtLeast(box.minY)

        val speedShrinkFactor = min(0.05, max(player.sqrtSpeed * 0.5, targetVelocity.sqrtSpeed * 0.5))

        val initialCutoffBox = box
            .withMaxY(highest)
            .withMinY(lowest)
            .contract(shrinkBox.toDouble(), 0.0, shrinkBox.toDouble())
            .contract(speedShrinkFactor, abs(player.velocity.y), speedShrinkFactor)

        val cutoffBox = if (dynamicShrinkBox) {
            initialCutoffBox.contract(speedShrinkFactor, abs(player.velocity.y), speedShrinkFactor)
        } else {
            initialCutoffBox
        }

        val offset = if (gaussian.enabled && gaussian.factorCheck()) {
            gaussian.updateGaussianOffset(entity)
            gaussian.currentOffset
        } else {
            Vec3d.ZERO
        }

        val targetPoint = preferredBoxPoint.point(cutoffBox, playerEyes) + offset

        val finalCutoffBox = Box(
            min(targetPoint.x, cutoffBox.minX),
            min(targetPoint.y, cutoffBox.minY),
            min(targetPoint.z, cutoffBox.minZ),
            max(targetPoint.x, cutoffBox.maxX),
            max(targetPoint.y, cutoffBox.maxY),
            max(targetPoint.z, cutoffBox.maxZ)
        )

        return Point(playerEyes, targetPoint, box, finalCutoffBox)
    }

    data class Point(val fromPoint: Vec3d, val toPoint: Vec3d, val box: Box, val cutOffBox: Box)

    enum class AimSituation {
        FOR_THE_FUTURE,
        FOR_NEXT_TICK,
        FOR_NOW;

        val isNear: Boolean
            get() = this == FOR_NEXT_TICK || this == FOR_NOW

    }

}
