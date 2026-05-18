/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.handItems
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.render.trajectory.EntityTrajectoryResolver
import net.ccbluex.liquidbounce.utils.render.trajectory.HeldItemTrajectoryResolver
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryDisplayResolver
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryType
import net.minecraft.world.entity.TraceableEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.phys.Vec3

/**
 * Trajectories module
 *
 * Allows you to see where projectile items will land.
 */
@Suppress("MagicNumber")
object ModuleTrajectories : ClientModule("Trajectories", ModuleCategories.RENDER) {
    private val maxSimulatedTicks by int("MaxSimulatedTicks", 240, 1..1000, "ticks")
    private val maxRenderDistance by int("MaxRenderDistance", 96, 16..512, "m")
    private val cullBehindPlayer by boolean("CullBehindPlayer", false)
    private val showMultiShot by boolean("ShowMultiShot", true)

    private val trajectoryTypes by multiEnumChoice("TrajectoryTypes", TrajectoryType.entries, canBeNone = false)

    private val show by multiEnumChoice(
        "Show",
        Show.OTHER_PLAYERS,
        Show.ACTIVE_TRAJECTORY_ARROW
    )

    private val alwaysShowBow get() = Show.ALWAYS_SHOW_BOW in show
    private val otherPlayers get() = Show.OTHER_PLAYERS in show
    private val activeTrajectoryArrow get() = Show.ACTIVE_TRAJECTORY_ARROW in show
    private val activeTrajectoryOther get() = Show.ACTIVE_TRAJECTORY_OTHER in show

    init {
        tree(TrajectoryDetailedInfoRenderer)
    }

    internal val simulationResults =
        mutableListOf<Pair<TrajectoryInfoRenderer, TrajectoryInfoRenderer.SimulationResult>>()

    override fun onDisabled() {
        simulationResults.clear()
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        simulationResults.clear()
        renderEnvironmentForWorld(event.matrixStack) {
            val viewPos = player.eyePosition
            val viewDirection = player.rotation.directionVector
            val maxRenderDistanceSq = maxRenderDistance * maxRenderDistance.toDouble()

            for (entity in world.entitiesForRendering()) {
                val delta = entity.position().subtract(viewPos)
                if (delta.lengthSqr() > maxRenderDistanceSq ||
                    cullBehindPlayer && delta.dot(viewDirection) < 0.0 && delta.lengthSqr() > 9.0) {
                    continue
                }

                val (trajectoryInfo, trajectoryType) = EntityTrajectoryResolver.resolveEntityTrajectory(
                    entity,
                    activeTrajectoryArrow,
                    activeTrajectoryOther
                ) ?: continue

                if (trajectoryType !in trajectoryTypes) continue

                val trajectoryRenderer = TrajectoryInfoRenderer(
                    owner = (entity as? TraceableEntity)?.owner ?: entity,
                    icon = TrajectoryDisplayResolver.resolveEntityIcon(
                        entity, activeTrajectoryArrow, activeTrajectoryOther
                    ),
                    velocity = entity.deltaMovement,
                    pos = entity.position(),
                    trajectoryInfo = trajectoryInfo,
                    trajectoryType = trajectoryType,
                    type = TrajectoryInfoRenderer.Type.REAL,
                    renderOffset = Vec3.ZERO,
                )

                val color = TrajectoryDisplayResolver.resolveEntityColor(entity)

                simulationResults += trajectoryRenderer to trajectoryRenderer.drawTrajectoryForProjectile(
                    maxSimulatedTicks,
                    event.partialTicks,
                    trajectoryColor = color,
                    blockHitColor = color,
                    entityHitColor = color,
                )
            }

            if (otherPlayers) {
                for (otherPlayer in world.players()) {
                    if (otherPlayer !== player) {
                        val delta = otherPlayer.eyePosition.subtract(viewPos)
                        if (delta.lengthSqr() > maxRenderDistanceSq) {
                            continue
                        }
                        if (cullBehindPlayer && delta.dot(viewDirection) < 0.0 && delta.lengthSqr() > 9.0) {
                            continue
                        }
                    }

                    // Including the user
                    drawHypotheticalTrajectory(otherPlayer, event.partialTicks)
                }
            } else {
                drawHypotheticalTrajectory(player, event.partialTicks)
            }
        }

        debugParameter("TrajectoryCount") { simulationResults.size }
    }

    /**
     * Draws the trajectory for an item in the player's hand
     */
    private fun WorldRenderEnvironment.drawHypotheticalTrajectory(
        otherPlayer: Player,
        partialTicks: Float,
    ) {
        val shouldFilterHeldFishingRod = otherPlayer.fishing != null &&
            activeTrajectoryOther &&
            TrajectoryType.FishingBobber in trajectoryTypes

        val (trajectoryShotDescriptors, stack) = otherPlayer.handItems.firstNotNullOfOrNull { stack ->
            if (shouldFilterHeldFishingRod && stack.item is FishingRodItem) {
                return@firstNotNullOfOrNull null
            }

            HeldItemTrajectoryResolver.resolveHeldItemShots(
                otherPlayer,
                stack,
                alwaysShowBow,
                includeMultiShot = showMultiShot
            )?.let {
                it to stack
            }
        } ?: return

        val rotation = if (otherPlayer === player) {
            if (ModuleFreeCam.running) {
                RotationManager.serverRotation
            } else {
                RotationManager.activeRotationTarget?.rotation
                    ?: RotationManager.currentRotation ?: otherPlayer.rotation
            }
        } else {
            otherPlayer.rotation
        }

        trajectoryShotDescriptors.forEach { shotDescriptor ->
            if (shotDescriptor.trajectoryType !in trajectoryTypes) {
                return@forEach
            }

            val shotRotation = Rotation(
                yaw = rotation.yaw + shotDescriptor.yawOffsetDegrees,
                pitch = rotation.pitch,
                isNormalized = rotation.isNormalized
            )

            val renderer = TrajectoryInfoRenderer.getHypotheticalTrajectory(
                owner = otherPlayer,
                icon = if (shotDescriptor.icon.isEmpty) stack else shotDescriptor.icon,
                trajectoryInfo = shotDescriptor.trajectoryInfo,
                trajectoryType = shotDescriptor.trajectoryType,
                rotation = shotRotation,
                partialTicks = partialTicks
            )

            simulationResults += renderer to renderer.drawTrajectoryForProjectile(
                maxSimulatedTicks,
                partialTicks,
                trajectoryColor = Color4b.WHITE,
                blockHitColor = Color4b(0, 160, 255, 150),
                entityHitColor = Color4b.RED.alpha(100),
            )
        }
    }

    private enum class Show(
        override val tag: String
    ) : Tagged {
        ALWAYS_SHOW_BOW("AlwaysShowBow"),
        OTHER_PLAYERS("OtherPlayers"),
        ACTIVE_TRAJECTORY_ARROW("ActiveTrajectoryArrow"),
        ACTIVE_TRAJECTORY_OTHER("ActiveTrajectoryOther"),
    }
}
