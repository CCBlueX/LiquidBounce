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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.block.ShapeContext
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.entity.projectile.thrown.EggEntity
import net.minecraft.entity.projectile.thrown.EnderPearlEntity
import net.minecraft.entity.projectile.thrown.PotionEntity
import net.minecraft.entity.projectile.thrown.SnowballEntity
import net.minecraft.item.*
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Trajectories module
 *
 * Allows you to see where projectile items will land.
 */

object ModuleTrajectories : Module("Trajectories", Category.RENDER) {
    private val maxSimulatedTicks by int("MaxSimulatedTicks", 240, 1..1000, "ticks")
    private val alwaysShowBow by boolean("AlwaysShowBow", false)
    private val otherPlayers by boolean("OtherPlayers", true)
    private val activeTrajectoryArrow by boolean("ActiveTrajectoryArrow", true)
    private val activeTrajectoryOther by boolean("ActiveTrajectoryOther", false)


    fun shouldDrawTrajectory(player: PlayerEntity, item: Item): Boolean {
        return item is BowItem && (player.isUsingItem || alwaysShowBow) || item is FishingRodItem ||
            item is ThrowablePotionItem || item is SnowballItem || item is EnderPearlItem || item is EggItem ||
            item is CrossbowItem
    }

    private fun isValidArrowEntity(entity: Entity): Boolean {
        return activeTrajectoryArrow && entity is ArrowEntity && !entity.inGround
    }

    private fun isOtherEntity(entity: Entity): Boolean {
        return activeTrajectoryOther && (entity is EnderPearlEntity || entity is SnowballEntity ||
            entity is PotionEntity || entity is EggEntity)
    }

    private fun renderHitBlockFace(matrixStack: MatrixStack, blockHitResult: BlockHitResult, color: Color4b) {
        val currPos = blockHitResult.blockPos
        val currState = currPos.getState()!!

        val bestBox = currState.getOutlineShape(world, currPos, ShapeContext.of(player)).boundingBoxes
            .map { it.offset(currPos) }
            .filter { blockHitResult.pos in it.expand(0.01, 0.01, 0.01) }
            .minByOrNull { it.center.squaredDistanceTo(blockHitResult.pos) }

        if (bestBox != null) {
            renderEnvironmentForWorld(matrixStack) {
                withColor(color) {
                    drawSideBox(bestBox, blockHitResult.side)
                }
            }
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        world.entities.filter { isValidArrowEntity(it) || isOtherEntity(it) }.forEach {
            val landingPosition = drawTrajectoryForProjectile(
                it.velocity,
                if (it is ArrowEntity) TrajectoryInfo(0.05F, 0.3F) else TrajectoryInfo(0.03F, 0.25F),
                it.pos,
                it,
                Vec3(0.0, 0.0, 0.0),
                when (it) {
                    is ArrowEntity -> Color4b(255, 0, 0, 200)
                    is EnderPearlEntity -> Color4b(128, 0, 128, 200)
                    else -> Color4b(200, 200, 200, 200)
                },
                matrixStack
            )
            if (landingPosition is BlockHitResult) {
                renderHitBlockFace(matrixStack, landingPosition, when (it) {
                    is ArrowEntity -> Color4b(255, 0, 0, 200)
                    is EnderPearlEntity -> Color4b(128, 0, 128, 200)
                    else -> Color4b(200, 200, 200, 200)
                })
            }

            if (landingPosition is EntityHitResult) {
                if (landingPosition.entity != player) {
                    return@forEach
                }

                // todo: add rect support
//                val vertexFormat = PositionColorVertexFormat()
//
//                vertexFormat.initBuffer(4)
//
//                val indexBuffer = IndexBuffer(8, VertexFormatComponentDataType.GlUnsignedShort)
//
//                vertexFormat.rect(indexBuffer, Vec3(-10.0, -10.0, 0.0), Vec3(10.0, 10.0, 0.0), Color4b(255, 0, 0, 120))
//
//                RenderEngine.enqueueForRendering(
//                    RenderEngine.SCREEN_SPACE_LAYER,
//                    VertexFormatRenderTask(vertexFormat, PrimitiveType.Triangles, ColoredPrimitiveShader)
//                )
            }
        }


        if (otherPlayers) {
            for (otherPlayer in world.players) {
                val landingPosition = drawTrajectory(otherPlayer, matrixStack, event.partialTicks)

                if (landingPosition is EntityHitResult) {
                    if (landingPosition.entity != player) {
                        continue
                    }

                    // todo: add rect support
//                val vertexFormat = PositionColorVertexFormat()
//
//                vertexFormat.initBuffer(4)
//
//                val indexBuffer = IndexBuffer(8, VertexFormatComponentDataType.GlUnsignedShort)
//
//                vertexFormat.rect(indexBuffer, Vec3(-2.0, -1.0, 0.0), Vec3(2.0, 1.0, 0.0), Color4b(255, 0, 0, 50))
//
//                RenderEngine.enqueueForRendering(
//                    RenderEngine.SCREEN_SPACE_LAYER,
//                    VertexFormatRenderTask(vertexFormat, PrimitiveType.Triangles, ColoredPrimitiveShader)
//                )
                }
            }
        }

        val landingPosition = drawTrajectory(player, matrixStack, event.partialTicks)

        if (landingPosition != null) {
            if (landingPosition is BlockHitResult) {
                renderHitBlockFace(matrixStack, landingPosition, Color4b(0, 160, 255, 150))
            } else if (landingPosition is EntityHitResult) {
                renderEnvironmentForWorld(matrixStack) {
                    val pos = landingPosition.entity
                        .interpolateCurrentPosition(event.partialTicks)

                    withColor(Color4b(255, 0, 0, 100)) {
                        drawSolidBox(landingPosition.entity.getDimensions(landingPosition.entity.pose)!!.getBoxAt(pos))
                    }

                }
            }
        }
    }


    private fun drawTrajectory(otherPlayer: PlayerEntity, matrixStack: MatrixStack, partialTicks: Float): HitResult? {
        val heldItem = otherPlayer.handItems.find { shouldDrawTrajectory(otherPlayer, it.item) } ?: return null

        val item = heldItem.item

        val trajectoryInfo = getTrajectoryInfo(otherPlayer, item) ?: return null

        val yaw: Float
        val pitch: Float

        val targetRotation = RotationManager.storedAimPlan?.rotation

        if (targetRotation == null) {
            yaw = otherPlayer.yaw
            pitch = otherPlayer.pitch
        } else {
            yaw = targetRotation.yaw
            pitch = targetRotation.pitch
        }

        val yawRadians = yaw / 180f * Math.PI.toFloat()
        val pitchRadians = pitch / 180f * Math.PI.toFloat()

        val interpolatedOffset = Vec3(
            otherPlayer.lastRenderX + (otherPlayer.x - otherPlayer.lastRenderX) * partialTicks - otherPlayer.x,
            otherPlayer.lastRenderY + (otherPlayer.y - otherPlayer.lastRenderY) * partialTicks - otherPlayer.y,
            otherPlayer.lastRenderZ + (otherPlayer.z - otherPlayer.lastRenderZ) * partialTicks - otherPlayer.z
        )

        // Positions
        val posX = otherPlayer.x - cos(yawRadians) * 0.16
        val posY = otherPlayer.eyeY - 0.10000000149011612
        val posZ = otherPlayer.z - sin(yawRadians) * 0.16

        // Motions
        var motionX = -sin(yawRadians) * cos(pitchRadians).toDouble()
        var motionY = -sin((pitch - trajectoryInfo.pitchSubtrahend).toRadians()).toDouble()
        var motionZ = cos(yawRadians) * cos(pitchRadians).toDouble()

        val length = sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ)

        motionX /= length
        motionY /= length
        motionZ /= length

        motionX *= trajectoryInfo.motionFactor
        motionY *= trajectoryInfo.motionFactor
        motionZ *= trajectoryInfo.motionFactor

        return drawTrajectoryForProjectile(
            Vec3d(motionX, motionY, motionZ),
            trajectoryInfo,
            Vec3d(posX, posY, posZ),
            otherPlayer,
            interpolatedOffset,
            Color4b.WHITE,
            matrixStack
        )
    }

    private fun drawTrajectoryForProjectile(
        motion: Vec3d,
        trajectoryInfo: TrajectoryInfo,
        pos: Vec3d,
        player: Entity,
        interpolatedOffset: Vec3,
        color: Color4b,
        matrixStack: MatrixStack
    ): HitResult? { // Normalize the motion vector
        var motionX = motion.x
        var motionY = motion.y
        var motionZ = motion.z
        var posX = pos.x
        var posY = pos.y
        var posZ = pos.z

        // Landing
        var landingPosition: HitResult? = null
        var hasLanded = false

        // Start drawing of path
        val lines = mutableListOf<Vec3>()

        var currTicks = 0

        while (!hasLanded && posY > world.bottomY && currTicks < maxSimulatedTicks) { // Set pos before and after
            val posBefore = Vec3d(posX, posY, posZ)
            var posAfter = Vec3d(posX + motionX, posY + motionY, posZ + motionZ)

            // Get landing position
            val blockHitResult = world.raycast(
                RaycastContext(
                    posBefore,
                    posAfter,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    player
                )
            )


            val entityHitResult = ProjectileUtil.getEntityCollision(
                world,
                player,
                posBefore,
                posAfter,
                Box(
                    -trajectoryInfo.size.toDouble(),
                    -trajectoryInfo.size.toDouble(),
                    -trajectoryInfo.size.toDouble(),
                    +trajectoryInfo.size.toDouble(),
                    +trajectoryInfo.size.toDouble(),
                    +trajectoryInfo.size.toDouble()
                ).offset(posX, posY, posZ).stretch(Vec3d(motionX, motionY, motionZ)).expand(1.0)
            ) {
                if (!it.isSpectator && it.isAlive && (it.canHit() || player != mc.player && it == mc.player)) {
                    if (player.isConnectedThroughVehicle(it)) return@getEntityCollision false
                } else {
                    return@getEntityCollision false
                }

                return@getEntityCollision true
            }

            // Check if arrow is landing
            if (entityHitResult != null && entityHitResult.type != HitResult.Type.MISS) {
                landingPosition = entityHitResult
                hasLanded = true
            } else if (blockHitResult != null && blockHitResult.type != HitResult.Type.MISS) {
                landingPosition = blockHitResult
                hasLanded = true
                posAfter = blockHitResult.pos
            }

            // Affect motions of arrow
            posX += motionX
            posY += motionY
            posZ += motionZ

            val blockState = world.getBlockState(BlockPos.ofFloored(posX, posY, posZ))

            // Check is next position water
            if (!blockState.fluidState.isEmpty) { // Update motion
                motionX *= 0.6F
                motionY *= 0.6F
                motionZ *= 0.6F
            } else { // Update motion
                motionX *= trajectoryInfo.motionSlowdown.toDouble()
                motionY *= trajectoryInfo.motionSlowdown.toDouble()
                motionZ *= trajectoryInfo.motionSlowdown.toDouble()
            }

            motionY -= trajectoryInfo.gravity.toDouble()

            // Draw path
            lines += Vec3(posAfter) + interpolatedOffset

            currTicks++
        }

        renderEnvironmentForWorld(matrixStack) {
            withColor(color) {
                drawLineStrip(*lines.toTypedArray())
            }
        }

        return landingPosition
    }

    private fun getTrajectoryInfo(player: PlayerEntity, item: Item): TrajectoryInfo? {
        val trajectoryInfo: TrajectoryInfo?

        when (item) {
            is BowItem -> {
                // Calculate power of bow
                var power = player.itemUseTime / 20f
                power = (power * power + power * 2F) / 3F
                power = if (alwaysShowBow && power == 0.0F) 1.0F else power
                if (power < 0.1F) return null

                trajectoryInfo = TrajectoryInfo(
                    0.05F,
                    0.3F,
                    motionFactor = power.coerceAtMost(1.0F) * 3.0F
                )
            }

            is CrossbowItem -> {
                trajectoryInfo = TrajectoryInfo(
                    0.05F,
                    0.3F,
                    motionFactor = 3.0F
                )
            }

            is FishingRodItem -> {
                trajectoryInfo = TrajectoryInfo(
                    0.04F,
                    0.25F,
                    motionSlowdown = 0.92F
                )
            }

            is PotionItem -> {
                trajectoryInfo = TrajectoryInfo(
                    0.05F,
                    0.25F,
                    motionFactor = 0.5F,
                    pitchSubtrahend = 20.0F
                )
            }

            else -> trajectoryInfo = TrajectoryInfo(0.03F, 0.25F)
        }

        return trajectoryInfo
    }

    data class TrajectoryInfo(
        var gravity: Float,
        var size: Float,
        var motionFactor: Float = 1.5F,
        var motionSlowdown: Float = 0.99F,
        var pitchSubtrahend: Float = 0.0F,
        var angle: Float = 0.99F,
        val isBow: Boolean = false
    )
}
