/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.MovementInputEvent
import net.ccbluex.liquidbounce.event.NotificationEvent
import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulation
import net.ccbluex.liquidbounce.utils.entity.SimulatedArrow
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.extensions.toDegrees
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.math.Line
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.PI
import kotlin.math.atan2

object ModuleAutoDodge : Module("AutoDodge", Category.COMBAT) {

    private val positions = mutableMapOf<SimulatedArrow, MutableList<Vec3>>()

    val tickRep = handler<MovementInputEvent> { event ->
        // We aren't actually where we are because of blink. So this module shall not cause any disturbance in that case.
        if (ModuleBlink.enabled) {
            return@handler
        }

        val world = world

        val arrows = findFlyingArrows(world)

        val simulatedPlayer = SimulatedPlayer.fromPlayer(player, SimulatedPlayer.SimulatedPlayerInput(event.forwards, event.backwards, event.left, event.right, player.input.jumping, player.isSprinting))

        val inflictedHit = getInflictedHits(simulatedPlayer, arrows) {} ?: return@handler

        val optimalDodgePosition = findOptimalDodgePosition(inflictedHit.prevArrowPos, inflictedHit.arrowVelocity) ?: return@handler

        val playerToOptimalDodgeVector = optimalDodgePosition.subtract(player.pos.x, 0.0, player.pos.z)

        val optimalYaw = atan2(playerToOptimalDodgeVector.z, playerToOptimalDodgeVector.x).toFloat() + PI.toFloat() / 2.0F
        val currentYaw = player.yaw.toRadians()

//        println(inflictedHit.prevArrowPos)
//        println(optimalYaw.toDegrees())
        println(inflictedHit.tickDelta)

        if (inflictedHit.tickDelta == 0) {
            notification("Blink", "Evasion failed!", NotificationEvent.Severity.INFO)
        }

        val dgs = MathHelper.wrapDegrees((optimalYaw - currentYaw).toDegrees())

        var forwards = false
        var backwards = false
        var left = false
        var right = false

//        println(dgs)

        if (dgs in -70.0F..70.0F) {
            forwards = true
        } else if (dgs < -120.0 || dgs > 120.0) {
            backwards = true
        }

        if (dgs in 20.0F..160.0F) {
            right = true
        } else if (dgs in -160.0F..-20.0F) {
            left = true
        }

        event.forwards = forwards
        event.backwards = backwards
        event.left = left
        event.right = right
        event.jumping = false
    }

    fun findOptimalDodgePosition(arrowPos: Vec3d, arrowVelocity: Vec3d): Vec3d? {
        val baseLine = Line(
            Vec3d(arrowPos.x, 0.0, arrowPos.z),
            Vec3d(arrowVelocity.x, 0.0, arrowVelocity.z)
        )

        val playerPos2d = Vec3d(player.pos.x, 0.0, player.pos.z)

        // Check if we are in the danger zone. If we are not in the danger zone there is no need to dodge (yet).
        if (baseLine.squaredDistanceTo(playerPos2d) > SAFE_DISTANCE_PLUS_PLAYER_HITBOX * SAFE_DISTANCE_PLUS_PLAYER_HITBOX) {
            return null
        }

        val dangerZone = getDangerZoneBorders(baseLine, SAFE_DISTANCE_PLUS_PLAYER_HITBOX)

        val nearestPointsToDangerZoneBorders = dangerZone.map { it.getNearestPointTo(playerPos2d) }
        val nearestPointDistancesToPlayer = nearestPointsToDangerZoneBorders.map { it.distanceTo(playerPos2d) }

//        println(nearestPointsToDangerZoneBorders.map { it.subtract(player.pos) })

        // Find the nearest point that is outside the danger zone
        return if (nearestPointDistancesToPlayer[0] < nearestPointDistancesToPlayer[1] - 0.1) {
            nearestPointsToDangerZoneBorders[0]
        } else {
            nearestPointsToDangerZoneBorders[1]
        }
    }

    const val SAFE_DISTANCE: Double = 0.5 * 1.4 + 0.5
    const val SAFE_DISTANCE_PLUS_PLAYER_HITBOX: Double = SAFE_DISTANCE + 0.9 * 1.4

    /**
     * Returns the two lines at the border of the danger zone (in 2D)
     */
    private fun getDangerZoneBorders(baseLine: Line, distanceFromBaseLine: Double): Array<Line> {
        val orthoVecToBaseLine = baseLine.direction.crossProduct(Vec3d(0.0, 1.0, 0.0)).normalize()

        val orthoOffsetVec = baseLine.direction.multiply(distanceFromBaseLine)

        val lineLeft = Line(baseLine.position.subtract(orthoOffsetVec), baseLine.direction)
        val lineRight = Line(baseLine.position.add(orthoOffsetVec), baseLine.direction)

        return arrayOf(lineLeft, lineRight)
    }

    fun findFlyingArrows(world: ClientWorld): List<ArrowEntity> {
        return world.entities.mapNotNull {
            if (it !is ArrowEntity) {
                return@mapNotNull null
            }
            if (it.inGround)
                return@mapNotNull null

            return@mapNotNull it
        }
    }



    fun <T: PlayerSimulation> getInflictedHits(simulatedPlayer: T, arrows: List<ArrowEntity>, maxTicks: Int = 80, behaviour: (T) -> Unit): HitInfo? {
        val simulatedArrows = arrows.map { SimulatedArrow(world, it.pos, it.velocity, false) }

        positions.clear()

        for (i in 0 until maxTicks) {
            behaviour(simulatedPlayer)

            simulatedPlayer.tick()

            simulatedArrows.forEachIndexed { arrowIndex, arrow ->
                if (arrow.inGround) {
                    return@forEachIndexed
                }

                val lastPos = arrow.pos
                val hitResult = arrow.tick()

                positions.getOrPut(arrow) { mutableListOf() }.add(Vec3(arrow.pos))

                val playerHitBox = Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).expand(0.7).offset(simulatedPlayer.pos)
                val raycastResult = playerHitBox.raycast(lastPos, arrow.pos)

                raycastResult.orElse(null)?.let { hitPos ->
                    return HitInfo(i, arrows[arrowIndex], hitPos, lastPos, arrow.velocity)
                }
            }
        }

        return null
    }

    data class HitInfo(val tickDelta: Int, val arrowEntity: ArrowEntity, val hitPos: Vec3d, val prevArrowPos: Vec3d, val arrowVelocity: Vec3d)

    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        synchronized(positions) {
            // Get all positions for each arrow

            renderEnvironment(matrixStack) {
                withColor(Color4b.WHITE) {
                    for ((_, positions) in positions) {
                        drawLineStrip(*positions.toTypedArray())
                    }
                }
            }
        }
    }

}
