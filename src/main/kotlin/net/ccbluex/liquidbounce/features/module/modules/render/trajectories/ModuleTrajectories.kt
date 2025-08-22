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
package net.ccbluex.liquidbounce.features.module.modules.render.trajectories

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.minecraft.entity.Ownable
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d

/**
 * Trajectories module
 *
 * Allows you to see where projectile items will land.
 */
@Suppress("MagicNumber")
object ModuleTrajectories : ClientModule("Trajectories", Category.RENDER) {
    private val maxSimulatedTicks by int("MaxSimulatedTicks", 240, 1..1000, "ticks")
    private val show by multiEnumChoice(
        "Show",
        Show.OTHER_PLAYERS,
        Show.ACTIVE_TRAJECTORY_ARROW
    )

    private val alwaysShowBow get() = Show.ALWAYS_SHOW_BOW in show
    private val otherPlayers get() = Show.OTHER_PLAYERS in show
    private val activeTrajectoryArrow get() = Show.ACTIVE_TRAJECTORY_ARROW in show
    private val activeTrajectoryOther get() = Show.ACTIVE_TRAJECTORY_OTHER in show

    private val simulationResults: MutableList<TrajectoryInfoRenderer.SimulationResult> = ObjectArrayList()

    override fun onDisabled() {
        simulationResults.clear()
    }

    val overlayRenderHandler = handler<OverlayRenderEvent> { event ->
        debugParameter("TrajectoryCount") { simulationResults.size }
        /**
         * TODO:
         * draw owner name (bool)
         * draw landing duration (none ticks seconds)
         * draw at (owner, entity, landing pos)
         * draw offset
         */
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        simulationResults.clear()
        world.entities.forEach {
            val trajectoryInfo = TrajectoryData.getRenderTrajectoryInfoForOtherEntity(
                it,
                this.activeTrajectoryArrow,
                this.activeTrajectoryOther
            ) ?: return@forEach

            val trajectoryRenderer = TrajectoryInfoRenderer(
                owner = (it as? Ownable)?.owner ?: it,
                velocity = it.velocity,
                pos = it.pos,
                trajectoryInfo = trajectoryInfo,
                renderOffset = Vec3d.ZERO
            )

            val color = TrajectoryData.getColorForEntity(it)

            simulationResults += trajectoryRenderer.drawTrajectoryForProjectile(
                maxSimulatedTicks,
                event,
                trajectoryColor = color,
                blockHitColor = color,
                entityHitColor = color,
            )
        }

        if (otherPlayers) {
            for (otherPlayer in world.players) {
                // Including the user
                drawHypotheticalTrajectory(otherPlayer, event)?.let { simulationResults += it }
            }
        } else {
            drawHypotheticalTrajectory(player, event)?.let { simulationResults += it }
        }
    }

    /**
     * Draws the trajectory for an item in the player's hand
     */
    private fun drawHypotheticalTrajectory(
        otherPlayer: PlayerEntity,
        event: WorldRenderEvent
    ): TrajectoryInfoRenderer.SimulationResult? {
        val trajectoryInfo = otherPlayer.handItems.firstNotNullOfOrNull {
            TrajectoryData.getRenderedTrajectoryInfo(otherPlayer, it.item, this.alwaysShowBow)
        } ?: return null

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

        val renderer = TrajectoryInfoRenderer.getHypotheticalTrajectory(
            entity = otherPlayer,
            trajectoryInfo = trajectoryInfo,
            rotation = rotation,
            partialTicks = event.partialTicks
        )

        return renderer.drawTrajectoryForProjectile(
            maxSimulatedTicks,
            event,
            trajectoryColor = Color4b.WHITE,
            blockHitColor = Color4b(0, 160, 255, 150),
            entityHitColor = Color4b(255, 0, 0, 100),
        )
    }

    private enum class Show(
        override val choiceName: String
    ) : NamedChoice {
        ALWAYS_SHOW_BOW("AlwaysShowBow"),
        OTHER_PLAYERS("OtherPlayers"),
        ACTIVE_TRAJECTORY_ARROW("ActiveTrajectoryArrow"),
        ACTIVE_TRAJECTORY_OTHER("ActiveTrajectoryOther"),
    }
}
