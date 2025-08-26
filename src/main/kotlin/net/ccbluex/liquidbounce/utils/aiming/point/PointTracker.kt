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
package net.ccbluex.liquidbounce.utils.aiming.point

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.aiming.point.features.Gaussian
import net.ccbluex.liquidbounce.utils.aiming.point.features.LazyPoint
import net.ccbluex.liquidbounce.utils.aiming.point.preference.PreferredBoxPart
import net.ccbluex.liquidbounce.utils.aiming.point.preference.PreferredBoxPoint
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PointTracker(
    highestPointDefault: PreferredBoxPart = PreferredBoxPart.HEAD,
    lowestPointDefault: PreferredBoxPart = PreferredBoxPart.BODY,
    timeEnemyOffsetDefault: Float = 0.4f,
    timeEnemyOffsetScale: ClosedFloatingPointRange<Float> = -1f..1f
) : Configurable("AimPoint", aliases = arrayOf("PointTracker")), EventListener {

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

    /**
     * This introduces a layer of randomness to the point tracker. A gaussian distribution is being used to
     * calculate the offset.
     */
    private val gaussian = tree(Gaussian(this))

    /**
     * This will allow the point to stay at a certain position when the minimum threshold is not reached.
     */
    private val lazyPoint = tree(LazyPoint(this))

    /**
     * OutOfBox will set the box offset to an unreachable position.
     */
    private val outOfBox by boolean("OutOfBox", false)

    /**
     * The shrink box value will shrink the cut-off box by the given amount.
     */
    private val shrinkBox by float("ShrinkBox", 0.05f, 0.0f..0.3f)
    private val dynamicShrinkBox by boolean("DynamicShrinkBox", true)

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

        val targetPoint = lazyPoint.update(preferredBoxPoint.point(cutoffBox, playerEyes) + offset)

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
