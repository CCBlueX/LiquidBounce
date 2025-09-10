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
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.point.exempts.ExemptBestHitVector
import net.ccbluex.liquidbounce.utils.aiming.point.exempts.ExemptBoxPart
import net.ccbluex.liquidbounce.utils.aiming.point.exempts.ExemptContext
import net.ccbluex.liquidbounce.utils.aiming.point.features.PointProcessorDelay
import net.ccbluex.liquidbounce.utils.aiming.point.features.PointProcessorGaussian
import net.ccbluex.liquidbounce.utils.aiming.point.features.PointProcessorLazy
import net.ccbluex.liquidbounce.utils.aiming.utils.projectPointsOnBox
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.entity.getBoundingBoxAt
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.awt.Color

class PointTracker(val parent: EventListener) : Configurable("AimPoint"), EventListener {

    override fun parent() = parent

    private val predicateBoxParts by multiEnumChoice<ExemptBoxPart>("ExemptBoxParts")
    private val predicateBestHitVector = tree(ExemptBestHitVector(this))

    /**
     * This introduces a layer of randomness to the point tracker. A gaussian distribution is being used to
     * calculate the offset.
     */
    private val gaussian = tree(PointProcessorGaussian(this))

    /**
     * This will allow the point to stay at a certain position when the minimum threshold is not reached.
     */
    private val lazy = tree(PointProcessorLazy(this))

    /**
     * This will allow the point to be delayed until the ticks expire.
     */
    private val delay = tree(PointProcessorDelay(this))

    private val processors
        get() = listOf(delay, lazy, gaussian).filter { processor -> processor.enabled }

    /**
     * The point tracker is being used to track a certain point of an entity.
     *
     * @param entity The entity we want to track.
     */
    fun findPoint(eyes: Vec3d, entity: LivingEntity, ticks: Int = 0): PointInsideBox {
        // Predict target position
        val targetPos = PositionExtrapolation.getBestForEntity(entity)
            .getPositionInTicks(ticks.toDouble())

        // Project points onto box
        val box = entity.getBoundingBoxAt(targetPos)
            // Support [ModuleHitbox]
            .expand(entity.targetingMargin.toDouble())
        val points = box.getPoints(eyes)

        val bestHitVector = points.minByOrNull { it.squaredDistanceTo(eyes) }
            ?: box.getPseudoClosest(eyes)
        val worstHitVector = points.maxByOrNull { it.squaredDistanceTo(eyes) }
            ?: box.getPseudoFurthest(eyes)

        // Filter exempts
        val predicateContext = ExemptContext(box, bestHitVector, worstHitVector)
        val predicates = predicateBoxParts + predicateBestHitVector
        val pointsWithExempts = points.filter { point ->
            predicates.none { predicate -> predicate.predicate(predicateContext, point) }
        }

        parent.debugGeometry("Points") {
            ModuleDebug.DebugCollection(points.map { point ->
                val percentage = calculateDistancePercentage(point, eyes, bestHitVector, worstHitVector)
                val color = if (point !in pointsWithExempts) {
                    Color4b(Color.MAGENTA)
                } else {
                    Color4b(Color.GREEN).interpolateTo(Color4b.RED, percentage)
                }.fade(1.0f - percentage.toFloat())
                ModuleDebug.DebuggedPoint(point, color, 0.05)
            })
        }

        val pos = pointsWithExempts.minByOrNull { it.distanceTo(eyes) }
            ?: bestHitVector
        var point = PointInsideBox(pos, box)
        for (processor in processors) {
            point = processor.process(point)
        }
        return point
    }

    private fun Box.getPoints(eyes: Vec3d) = mutableListOf<Vec3d>().apply {
        projectPointsOnBox(eyes, this@getPoints) { point ->
            add(point)
        }
    }

    private fun Box.getPseudoClosest(eyes: Vec3d) = getNearestPoint(eyes, this)

    private fun Box.getPseudoFurthest(eyes: Vec3d) = Vec3d(
        eyes.x.coerceAtLeast(maxX).coerceAtMost(minX),
        eyes.y.coerceAtLeast(maxY).coerceAtMost(minY),
        eyes.z.coerceAtLeast(maxZ).coerceAtMost(minZ)
    )

    // For debug visuals
    private fun calculateDistancePercentage(point: Vec3d, eyes: Vec3d, bestHitVector: Vec3d,
                                            worstHitVector: Vec3d): Double {
        val pointDistance = point.distanceTo(eyes)
        val bestDistance = bestHitVector.distanceTo(eyes)
        val worstDistance = worstHitVector.distanceTo(eyes)
        return ((pointDistance - bestDistance) / (worstDistance - bestDistance)).coerceIn(0.0, 1.0)
    }

}
