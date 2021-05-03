/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.*
import net.ccbluex.liquidbounce.render.utils.drawBox
import net.ccbluex.liquidbounce.render.utils.drawBoxSide
import net.ccbluex.liquidbounce.render.utils.rect
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.minecraft.block.ShapeContext
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.item.*
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import org.lwjgl.opengl.GL11.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ModuleTrajectories : Module("Trajectories", Category.RENDER) {
    const val MAX_SIMULATED_TICKS = 240

    fun shouldDrawTrajectory(player: PlayerEntity, item: Item): Boolean {
        return item is BowItem && player.isUsingItem || item is FishingRodItem || item is ThrowablePotionItem || item is SnowballItem || item is EnderPearlItem || item is EggItem
    }

    val renderHandler = handler<EngineRenderEvent> { event ->
        val player = mc.player ?: return@handler
        val theWorld = mc.world ?: return@handler

        theWorld.entities.filter { it is ArrowEntity && !it.inGround }.forEach {
            val landingPosition = drawTrajectoryForProjectile(it.velocity, TrajectoryInfo(0.05F, 0.3F), it.pos, world, player, Vec3(0.0, 0.0, 0.0))

            if (landingPosition is EntityHitResult) {
                if (landingPosition.entity != player)
                    return@forEach

                val task = ColoredPrimitiveRenderTask(2, PrimitiveType.Triangles)

                task.rect(Vec3(-2.0, -1.0, 0.0), Vec3(2.0, 1.0, 0.0), Color4b(255, 0, 0, 100))

                RenderEngine.enqueueForRendering(RenderEngine.SCREEN_SPACE_LAYER, task)
            }
        }

        for (otherPlayer in theWorld.players) {
            val landingPosition = drawTrajectory(otherPlayer, event)

            if (landingPosition is EntityHitResult) {
                if (landingPosition.entity != player)
                    continue

                val task = ColoredPrimitiveRenderTask(2, PrimitiveType.Triangles)

                task.rect(Vec3(-2.0, -1.0, 0.0), Vec3(2.0, 1.0, 0.0), Color4b(255, 0, 0, 50))

                RenderEngine.enqueueForRendering(RenderEngine.SCREEN_SPACE_LAYER, task)
            }
        }

        val landingPosition = drawTrajectory(player, event)

        if (landingPosition != null) {
            if (landingPosition is BlockHitResult) {
                val currPos = landingPosition.blockPos
                val currState = currPos.getState()!!

                val bestBB = currState.getOutlineShape(mc.world, currPos, ShapeContext.of(player)).boundingBoxes
                    .map { it.offset(currPos) }
                    .filter { landingPosition.pos in it.expand(0.01, 0.01, 0.01) }
                    .minByOrNull { it.center.squaredDistanceTo(landingPosition.pos) }

                if (bestBB != null) {
                    RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER, drawBoxSide(bestBB.offset(0.0, 0.0, -1.0), landingPosition.side, Color4b(0, 160, 255, 150)))
                }
            } else if (landingPosition is EntityHitResult) {
                RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER, drawBox(landingPosition.entity.boundingBox.offset(0.0, 0.0, -1.0), Color4b(255, 0, 0, 150)))
            }
        }
    }

    private fun drawTrajectory(otherPlayer: PlayerEntity, event: EngineRenderEvent): HitResult? {
        val heldItem = otherPlayer.itemsHand.find { shouldDrawTrajectory(otherPlayer, it.item) } ?: return null

        val item = heldItem.item

        val trajectoryInfo = getTrajectoryInfo(otherPlayer, item) ?: return null

        val yaw: Float
        val pitch: Float

        val targetRotation = RotationManager.targetRotation

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
            otherPlayer.lastRenderX + (otherPlayer.x - otherPlayer.lastRenderX) * event.tickDelta - otherPlayer.x,
            otherPlayer.lastRenderY + (otherPlayer.y - otherPlayer.lastRenderY) * event.tickDelta - otherPlayer.y,
            otherPlayer.lastRenderZ + (otherPlayer.z - otherPlayer.lastRenderZ) * event.tickDelta - otherPlayer.z,
        ).add(Vec3(0.0, 0.0, -1.0))

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
            Vec3d(motionX, motionY, motionZ), trajectoryInfo, Vec3d(posX, posY, posZ), world, otherPlayer, interpolatedOffset
        )
    }

    private fun drawTrajectoryForProjectile(
        motion: Vec3d,
        trajectoryInfo: TrajectoryInfo,
        pos: Vec3d,
        theWorld: ClientWorld,
        player: PlayerEntity,
        interpolatedOffset: Vec3
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

        val color = Color4b.WHITE // TODO: Get this color in a nice way

        val renderTask = ColoredPrimitiveRenderTask(MAX_SIMULATED_TICKS + 1, PrimitiveType.LineStrip)

        var currTicks = 0

        while (!hasLanded && posY > 0.0 && currTicks < MAX_SIMULATED_TICKS) { // Set pos before and after
            val posBefore = Vec3d(posX, posY, posZ)
            var posAfter = Vec3d(posX + motionX, posY + motionY, posZ + motionZ)

            // Get landing position
            val blockHitResult = theWorld.raycast(
                RaycastContext(
                    posBefore, posAfter, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
                )
            )

            val entityHitResult = ProjectileUtil.getEntityCollision(
                world, player, posBefore, posAfter, Box(
                    -trajectoryInfo.size.toDouble(),
                    -trajectoryInfo.size.toDouble(),
                    -trajectoryInfo.size.toDouble(),
                    +trajectoryInfo.size.toDouble(),
                    +trajectoryInfo.size.toDouble(),
                    +trajectoryInfo.size.toDouble()
                ).offset(posX, posY, posZ).stretch(Vec3d(motionX, motionY, motionZ)).expand(1.0)
            ) {
                if (!it.isSpectator && it.isAlive && (it.collides() || player != mc.player && it == mc.player)) {
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
                posAfter = entityHitResult.pos
            } else if (blockHitResult != null && blockHitResult.type != HitResult.Type.MISS) {
                landingPosition = blockHitResult
                hasLanded = true
                posAfter = blockHitResult.pos
            }

            // Affect motions of arrow
            posX += motionX
            posY += motionY
            posZ += motionZ

            val blockState = theWorld.getBlockState(BlockPos(posX, posY, posZ))

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

            renderTask.index(renderTask.vertex(Vec3(posAfter) + interpolatedOffset, color))

            currTicks++
        }

        RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER, renderTask)
        return landingPosition
    }

    private fun getTrajectoryInfo(player: PlayerEntity, item: Item): TrajectoryInfo? {
        when (item) {
            is BowItem -> {
                // Calculate power of bow
                var power = player.itemUseTime / 20f
                power = (power * power + power * 2F) / 3F

                if (power < 0.1F)
                    return null

                return TrajectoryInfo(
                    0.05F,
                    0.3F,
                    motionFactor = power.coerceAtMost(1.0F) * 3.0F
                )
            }
            is FishingRodItem -> {
                return TrajectoryInfo(
                    0.04F,
                    0.25F,
                    motionSlowdown = 0.92F
                )
            }
            is PotionItem -> {
                return TrajectoryInfo(
                    0.05F,
                    0.25F,
                    motionSlowdown = 0.5F,
                    pitchSubtrahend = 20.0F
                )
            }
            else -> return TrajectoryInfo(0.03F, 0.25F)
        }
    }

    data class TrajectoryInfo(
        var gravity: Float,
        var size: Float,
        var motionFactor: Float = 1.5F * 0.4F,
        var motionSlowdown: Float = 0.99F,
        var pitchSubtrahend: Float = 0.0F,
        var angle: Float = 0.99F,
        val isBow: Boolean = false,
    )
}
