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
package net.ccbluex.liquidbounce.features.module.modules.render.trajectories

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

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

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        world.entities.forEach {
            val trajectoryInfo = TrajectoryData.getRenderTrajectoryInfoForOtherEntity(
                it,
                this.activeTrajectoryArrow,
                this.activeTrajectoryOther
            ) ?: return@forEach

            val trajectoryRenderer = TrajectoryInfoRenderer(
                owner = it,
                velocity = it.velocity,
                pos = it.pos,
                trajectoryInfo = trajectoryInfo,
                renderOffset = Vec3d.ZERO
            )

            val color = TrajectoryData.getColorForEntity(it)

            val hitResult = trajectoryRenderer.drawTrajectoryForProjectile(maxSimulatedTicks, color, matrixStack)

            if (hitResult != null && !(hitResult is EntityHitResult && hitResult.entity == player)) {
                drawLandingPos(hitResult, trajectoryInfo, event, color, color)
            }
        }

        if (otherPlayers) {
            for (otherPlayer in world.players) {
                drawHypotheticalTrajectory(otherPlayer, event)
            }
        }

        drawHypotheticalTrajectory(player, event)
    }

    /**
     * Draws the trajectory for an item in the player's hand
     */
    private fun drawHypotheticalTrajectory(otherPlayer: PlayerEntity, event: WorldRenderEvent) {
        val trajectoryInfo = otherPlayer.handItems.firstNotNullOfOrNull {
            TrajectoryData.getRenderedTrajectoryInfo(otherPlayer, it.item, this.alwaysShowBow)
        } ?: return

        val rotation = RotationManager.storedAimPlan?.rotation ?: otherPlayer.rotation

        val yawRadians = rotation.yaw / 180f * Math.PI.toFloat()
        val pitchRadians = rotation.pitch / 180f * Math.PI.toFloat()

        val interpolatedOffset = otherPlayer.interpolateCurrentPosition(event.partialTicks) - otherPlayer.pos

        // Positions
        val pos = Vec3d(
            otherPlayer.x,
            otherPlayer.eyeY - 0.10000000149011612,
            otherPlayer.z
        )

        var velocity = Vec3d(
            -sin(yawRadians) * cos(pitchRadians).toDouble(),
            -sin((rotation.pitch + trajectoryInfo.roll).toRadians()).toDouble(),
            cos(yawRadians) * cos(pitchRadians).toDouble()
        ).normalize() * trajectoryInfo.initialVelocity

        if (trajectoryInfo.copiesPlayerVelocity) {
            velocity += Vec3d(
                otherPlayer.velocity.x,
                if (otherPlayer.isOnGround) 0.0 else otherPlayer.velocity.y,
                otherPlayer.velocity.z
            )
        }

        val renderer = TrajectoryInfoRenderer(
            owner = otherPlayer,
            velocity = velocity,
            pos = pos,
            trajectoryInfo = trajectoryInfo,
            renderOffset = interpolatedOffset + Vec3d(-cos(yawRadians) * 0.16, 0.0, -sin(yawRadians) * 0.16)
        )

        val hitResult = renderer.drawTrajectoryForProjectile(maxSimulatedTicks, Color4b.WHITE, event.matrixStack)

        drawLandingPos(
            hitResult,
            trajectoryInfo,
            event,
            Color4b(0, 160, 255, 150),
            Color4b(255, 0, 0, 100)
        )
    }

}
