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
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.combat.ClickScheduler.Companion.RNG
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.security.SecureRandom
import kotlin.math.*


class PointTracker(
    highestPointDefault: PreferredBoxPart = PreferredBoxPart.HEAD,
    lowestPointDefault: PreferredBoxPart = PreferredBoxPart.BODY,
    gaussianOffsetDefault: Boolean = true,
    timeEnemyOffsetDefault: Float = 0.4f,
    timeEnemyOffsetScale: ClosedFloatingPointRange<Float> = -1f..1f
) : Configurable("PointTracker"), Listenable {

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
     * The time offset defines a prediction or rather a delay of the point tracker.
     * We can either try to predict the next location of the player and use this as our newest point, or
     * we pretend to be slow in the head and aim behind.
     */
    private val timeEnemyOffset by float("TimeEnemyOffset", timeEnemyOffsetDefault, timeEnemyOffsetScale)

    /**
     * This introduces a layer of randomness to the point tracker. A gaussian distribution is being used to
     * calculate the offset.
     */
    private val gaussianFactor by float("GaussianOffset", 0.0f, 0.0f..1.0f)
    private val gaussianChance by int("GaussianChance", 100, 0..100, "%")

    /**
     * OutOfBox will set the box offset to an unreachable position.
     */
    private val outOfBox by boolean("OutOfBox", false)

    /**
     * The shrink box value will shrink the cut-off box by the given amount.
     */
    private val shrinkBox by float("ShrinkBox", 0.05f, 0.0f..0.3f)

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
        ASSIST("Assist", { box, eyes ->
            val vec3 = eyes + player.rotation.rotationVec

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
     * The current offset of the point tracker.
     */
    private val random = SecureRandom()
    private var currentOffset = Vec3d.ZERO

    /**
     * The point tracker is being used to track a certain point of an entity.
     *
     * @param entity The entity we want to track.
     */
    fun gatherPoint(entity: LivingEntity, situation: AimSituation): Point {
        val playerPosition = player.pos
        val playerEyes = player.eyes
        val currentRotation = RotationManager.currentRotation ?: player.rotation
        val positionDifference = playerPosition.y - entity.pos.y

        if (intersectsBox && player.box.intersects(entity.box)) {
            return Point(playerEyes, playerEyes + currentRotation.rotationVec, entity.box, entity.box)
        }

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

        val cutoffBox = box
            .withMaxY(highest)
            .withMinY(lowest)
            .contract(shrinkBox.toDouble(), 0.0, shrinkBox.toDouble())
            .contract(speedShrinkFactor, abs(player.velocity.y), speedShrinkFactor)

        val offset = if (gaussianFactor > 0.0) {
            updateGaussianOffset()
            currentOffset
        } else {
            Vec3d.ZERO
        }

        val targetPoint = preferredBoxPoint.point(cutoffBox, playerEyes) + offset
        return Point(playerEyes, targetPoint, box, cutoffBox)
    }

    private fun updateGaussianOffset() {
        if (random.nextInt(100) > gaussianChance) {
            return
        }

        val newX = random.nextGaussian(MEAN_X, STDDEV_X) * gaussianFactor
        val newY = random.nextGaussian(MEAN_Y, STDDEV_Y) * gaussianFactor
        val newZ = random.nextGaussian(MEAN_Z, STDDEV_Z) * gaussianFactor

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
