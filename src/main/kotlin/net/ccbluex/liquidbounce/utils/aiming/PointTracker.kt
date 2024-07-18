/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.security.SecureRandom
import kotlin.math.abs


class PointTracker(
    highestPointDefault: PreferredBoxPart = PreferredBoxPart.HEAD,
    lowestPointDefault: PreferredBoxPart = PreferredBoxPart.BODY,
    gaussianOffsetDefault: Boolean = true,
    timeEnemyOffsetDefault: Float = 0.4f,
    timeEnemyOffsetScale: ClosedFloatingPointRange<Float> = -1f..1f
) : Configurable("PointTracker"), Listenable {

    companion object {
        /**
         * The base predict defines the amount of ticks we are going to predict the future movement of the client.
         * This adds on top of the amount of ticks the user has configured.
         * Why are we doing this? Because our server rotation lags behind, and we need to compensate for that.
         */
        const val BASE_PREDICT = 1

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
     * The value of predict future movement is the amount of ticks we are going to predict the future movement of the
     * client.
     */
    private val predictFutureMovement by int("PredictClientMovement", 1, 0..2,
        "ticks")

    /**
     * The time offset defines a prediction or rather a delay of the point tracker.
     * We can either try to predict the next location of the player and use this as our newest point, or
     * we pretend to be slow in the head and aim behind.
     */
    private val timeEnemyOffset by float("TimeEnemyOffset", timeEnemyOffsetDefault, timeEnemyOffsetScale)

    /**
     * This introduces a layer of randomness to the point tracker. A gaussian distribution is being used to
     * calculate the offset.
     */
    private val gaussianOffset by boolean("GaussianOffset", gaussianOffsetDefault)

    /**
     * OutOfBox will set the box offset to an unreachable position.
     */
    private val outOfBox by boolean("OutOfBox", false)

    /**
     * The shrink box value will shrink the cut-off box by the given amount.
     */
    private val shrinkBox by float("ShrinkBox", 0.05f, 0.0f..0.1f)

    /**
     * The shrink box value will shrink the cut-off box by the given amount.
     */
    private val intersectsBox by boolean("Intersects", true)

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
        STRAIGHT("Straight", { box, eyes ->
            Vec3d(
                box.center.x,
                eyes.y.coerceAtMost(box.maxY).coerceAtLeast(box.minY),
                box.center.z
            )
        }),
        CENTER("Center", { box, _ -> box.center });
    }

    /**
     * A predicted client position generated by the input handler based on the configured amount of ticks.
     */
    private var predictedPosition: Vec3d? = null
    private val predictedEyes: Vec3d?
        get() = predictedPosition?.let { Vec3d(it.x, it.y + mc.player!!.standingEyeHeight, it.z) }

    /**
     * The current offset of the point tracker.
     */
    private val random = SecureRandom()
    private var currentOffset = Vec3d.ZERO

    /**
     * The input handler tracks the movement of the player and calculates the predicted future position.
     */
    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent> {
        val input =
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(it.directionalInput)

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(input)

        repeat(BASE_PREDICT + predictFutureMovement) {
            simulatedPlayer.tick()
        }

        predictedPosition = simulatedPlayer.pos
    }

    /**
     * The point tracker is being used to track a certain point of an entity.
     *
     * @param entity The entity we want to track.
     */
    fun gatherPoint(entity: LivingEntity, situation: AimSituation): Point {
        val playerPosition = player.pos
        val positionDifference = playerPosition.y - entity.pos.y

        if (intersectsBox && player.box.intersects(entity.box)) {
            val eyes = if (situation == AimSituation.FOR_NOW) {
                null
            } else {
                predictedEyes
            } ?: player.eyes

            eyes.y -= abs(player.velocity.y) * 0.1

            return Point(eyes, entity.eyes, entity.box, entity.box)
        }

        // Predicted target position of the enemy
        val targetPrediction = entity.pos.subtract(entity.prevPos)
            .multiply(timeEnemyOffset.toDouble())
        var box = entity.box
            .contract(0.02, 0.05, 0.02)
            .offset(targetPrediction)
        if (!situation.isNear && outOfBox) {
            box = box.withMinY(box.maxY).withMaxY(box.maxY + 1.0)
        }

        val highest = (highestPoint.cutOff(box) + positionDifference)
            .coerceAtMost(box.maxY)
            .coerceAtLeast(box.minY + 0.2)
        val lowest = (lowestPoint.cutOff(box) + positionDifference)
            .coerceAtMost(box.maxY - 0.2)
            .coerceAtLeast(box.minY)

        val cutoffBox = box
            .withMaxY(highest)
            .withMinY(lowest)
            .contract(shrinkBox.toDouble(), 0.0, shrinkBox.toDouble())

        val offset = if (gaussianOffset) {
            updateGaussianOffset()
            currentOffset
        } else {
            Vec3d.ZERO
        }

        // The target point should be about the same height of
        val eyes = if (situation == AimSituation.FOR_NOW) {
            null
        } else {
            predictedEyes
        } ?: player.eyes

        val targetPoint = preferredBoxPoint.point(cutoffBox, eyes) + offset
        eyes.y -= abs(player.velocity.y) * 0.1

        return Point(eyes, targetPoint, box, cutoffBox)
    }

    private fun updateGaussianOffset() {
        val newX = random.nextGaussian(MEAN_X, STDDEV_X)
        val newY = random.nextGaussian(MEAN_Y, STDDEV_Y)
        val newZ = random.nextGaussian(MEAN_Z, STDDEV_Z)

        this.currentOffset = Vec3d(newX, newY, newZ)
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
