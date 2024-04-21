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
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.render.trajectories

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.block.ShapeContext
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

fun drawLandingPos(
    landingPosition: HitResult?,
    trajectoryInfo: TrajectoryInfo,
    event: WorldRenderEvent,
    blockHitColor: Color4b,
    entityHitColor: Color4b
) {
    if (landingPosition == null) {
        return
    }

    if (landingPosition is BlockHitResult) {
        renderHitBlockFace(event.matrixStack, landingPosition, blockHitColor)
    } else if (landingPosition is EntityHitResult) {
        val entities = listOf(landingPosition.entity)

        drawHitEntities(event.matrixStack, entityHitColor, entities, event.partialTicks)
    }

    if (trajectoryInfo != TrajectoryInfo.POTION) {
        return
    }

    val box: Box = Box.of(
        landingPosition.pos,
        trajectoryInfo.hitboxRadius * 2.0,
        trajectoryInfo.hitboxRadius * 2.0,
        trajectoryInfo.hitboxRadius * 2.0
    ).expand(4.0, 2.0, 4.0)

    val hitTargets =
        world.getNonSpectatingEntities(LivingEntity::class.java, box)
            .takeWhile { it.squaredDistanceTo(landingPosition.pos) <= 16.0 }
            .filter { it.isAffectedBySplashPotions }

    drawHitEntities(event.matrixStack, entityHitColor, hitTargets, event.partialTicks)
}

private fun drawHitEntities(
    matrixStack: MatrixStack,
    entityHitColor: Color4b,
    entities: List<Entity>,
    partialTicks: Float
) {
    renderEnvironmentForWorld(matrixStack) {
        withColor(entityHitColor) {
            for (entity in entities) {
                if (entity == player) {
                    continue
                }

                val pos = entity.interpolateCurrentPosition(partialTicks)

                withPositionRelativeToCamera(pos) {
                    drawSolidBox(
                        entity
                            .getDimensions(entity.pose)!!
                            .getBoxAt(Vec3d.ZERO)
                    )
                }
            }
        }

    }
}

fun renderHitBlockFace(matrixStack: MatrixStack, blockHitResult: BlockHitResult, color: Color4b) {
    val currPos = blockHitResult.blockPos
    val currState = currPos.getState()!!

    val bestBox = currState.getOutlineShape(world, currPos, ShapeContext.of(player)).boundingBoxes
        .filter { blockHitResult.pos in it.expand(0.01, 0.01, 0.01).offset(currPos) }
        .minByOrNull { it.center.squaredDistanceTo(blockHitResult.pos) }

    if (bestBox != null) {
        renderEnvironmentForWorld(matrixStack) {
            withPositionRelativeToCamera(Vec3d.of(currPos)) {
                withColor(color) {
                    drawSideBox(bestBox, blockHitResult.side)
                }
            }
        }
    }
}
