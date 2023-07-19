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
import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.entity.SimulatedArrow
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.extensions.toDegrees
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.PI
import kotlin.math.atan2

object ModuleAutoDodge : Module("AutoDodge", Category.COMBAT) {

    private val positions = mutableMapOf<SimulatedArrow, MutableList<Vec3>>()

    val tickRep = handler<MovementInputEvent> { event ->
        val world = world

        val arrows = world.entities.filter { it is ArrowEntity && !it.inGround }

        val simulatedPlayer = SimulatedPlayer.fromPlayer(player, SimulatedPlayer.SimulatedPlayerInput(event.forwards, event.backwards, event.left, event.right, player.input.jumping, player.isSprinting))

        val inflictedHit = getInflictedHits(simulatedPlayer, arrows) {} ?: return@handler

        val directionYaw = player.yaw.toRadians()

        val arrowYaw = atan2(inflictedHit.arrowVelocity.z, inflictedHit.arrowVelocity.x).toFloat() - PI.toFloat() / 2.0F

        val dgs = MathHelper.wrapDegrees((arrowYaw - directionYaw + PI.toFloat() / 2.0F).toDegrees())

        var forwards = false
        var backwards = false
        var left = false
        var right = false

        if (dgs in -90.0F..90.0F) {
            forwards = true
        } else {
            backwards = true
        }

        if (dgs > 0.0F) {
            right = true
        } else {
            left = true
        }

        event.forwards = forwards
        event.backwards = backwards
        event.left = left
        event.right = right
//
//        println(event.left)
//
//        println(dgs)

//        var simulatedArrows = world.entities.filter { it is ArrowEntity && !it.inGround }.map { SimulatedArrow(world, it.pos, it.velocity, false) }
//
//        synchronized(positions) {
//            positions.clear()
//
//            var hit = false
//
//            for (i in 0 until 40) {
//                simulatedPlayer.tick()
//
//                simulatedArrows.forEach { arrow ->
//                    val lastPos = arrow.pos
//
//                    arrow.tick()
//
//                    val playerHitBox = Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).offset(simulatedPlayer.pos)
//
//                    val raycastResult = playerHitBox.raycast(lastPos, arrow.pos)
//
//                    if (raycastResult.isPresent)
//                        hit = true
//                }
//
//                positions.addAll(listOf(simulatedPlayer.pos.x, simulatedPlayer.pos.y, simulatedPlayer.pos.z))
//            }
//
//            if (hit) {
//                player.setVelocity(0.0, 0.0, 0.0)
//            }
//        }

    }

    fun getInflictedHits(simulatedPlayer: SimulatedPlayer, arrows: List<Entity>, behaviour: (SimulatedPlayer) -> Unit): HitInfo? {
        val simulatedArrows = arrows.map { SimulatedArrow(world, it.pos, it.velocity, false) }

        positions.clear()

        for (i in 0 until 80) {
            behaviour(simulatedPlayer)

            simulatedPlayer.tick()

            simulatedArrows.forEach { arrow ->
                if (arrow.inGround) {
                    return@forEach
                }

                val lastPos = arrow.pos
                val hitResult = arrow.tick()

                positions.getOrPut(arrow) { mutableListOf() }.add(Vec3(arrow.pos))

                val playerHitBox = Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).expand(0.3).offset(simulatedPlayer.pos)
                val raycastResult = playerHitBox.raycast(lastPos, arrow.pos)

                raycastResult.orElse(null)?.let { hitPos ->
                    return HitInfo(hitPos, lastPos, arrow.velocity)
                }
            }
        }

        return null
    }

    data class HitInfo(val hitPos: Vec3d, val prevArrowPos: Vec3d, val arrowVelocity: Vec3d)

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
