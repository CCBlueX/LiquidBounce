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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.math.toFixed
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.minecraft.entity.Ownable
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import java.text.DecimalFormat
import java.util.function.BiFunction
import java.util.function.IntFunction

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

    private object ShowDetailedInfo : ToggleableConfigurable(this, "ShowDetailedInfo", false) {
        private val showAt by enumChoice("ShowAt", ShowAt.ENTITY)

        private enum class ShowAt(
            override val choiceName: String,
        ) : NamedChoice, BiFunction<TrajectoryInfoRenderer, TrajectoryInfoRenderer.SimulationResult, Vec3d> {
            OWNER("Owner"),
            ENTITY("Entity"),
            LANDING("Landing");

            override fun apply(
                renderer: TrajectoryInfoRenderer,
                result: TrajectoryInfoRenderer.SimulationResult,
            ): Vec3d = when (this) {
                OWNER -> renderer.owner.pos
                ENTITY -> result.positions.firstOrNull()
                LANDING -> result.positions.lastOrNull()
            } ?: renderer.owner.pos
        }

        private val ownerName by boolean("OwnerName", true)
        private val distance by boolean("Distance", true)
        private val durationUnit by enumChoice("DurationUnit", DurationUnit.TICKS)

        private val TICK_FORMATTER = DecimalFormat("0.#s")

        private enum class DurationUnit(
            override val choiceName: String,
        ) : NamedChoice, IntFunction<String> {
            TICKS("Ticks"),
            SECONDS("Seconds");

            override fun apply(ticks: Int): String = when (this) {
                TICKS -> ticks.toString()
                SECONDS -> TICK_FORMATTER.format(ticks * 0.05)
            }
        }

        private val scale by float("Scale", 1F, 0.25F..4F)
        private val renderOffset by vec3d("RenderOffset", Vec3d.ZERO)

        val overlayRenderHandler = handler<OverlayRenderEvent> { event ->
            fun Vec3d.calcScreenPosWithOffset(): Vec3? {
                return WorldToScreen.calculateScreenPos(add(renderOffset))
            }

            val context = event.context

            simulationResults.forEachIndexed { index, (renderer, result) ->
                val screenPos =
                    when {
                        showAt === ShowAt.OWNER && renderer.owner === player -> when (renderer.type) {
                            // If this renderer is created by player holding items and showAt is OWNER,
                            // then show at the landing position
                            TrajectoryInfoRenderer.Type.HYPOTHETICAL ->
                                ShowAt.LANDING.apply(renderer, result).calcScreenPosWithOffset()
                            else -> {
                                val centerX = mc.window.scaledWidth * 0.5F
                                val centerY = mc.window.scaledHeight * 0.5F
                                Vec3(centerX + 50F, centerY + index * (mc.textRenderer.fontHeight + 1), 0F)
                            }
                        }

                        else -> showAt.apply(renderer, result).calcScreenPosWithOffset()
                    } ?: return@forEachIndexed

                context.matrices.push()
                context.matrices.translate(screenPos.x, screenPos.y, screenPos.z)
                context.matrices.scale(scale, scale, 1.0F)

                val text = durationUnit.apply(result.positions.size).asText()
                if (ownerName && renderer.owner !== player) {
                    text.append(" ").append(renderer.owner.name)
                }
                if (distance && result.positions.isNotEmpty()) {
                    text.append(" ${player.pos.distanceTo(result.positions.last()).toFixed(1)}m")
                }

                var y = 0

                context.drawCenteredTextWithShadow(
                    mc.textRenderer,
                    text,
                    0,
                    y,
                    Color4b.WHITE.toARGB(),
                )
                y += mc.textRenderer.fontHeight + 1

                context.matrices.pop()
            }
        }
    }

    init {
        tree(ShowDetailedInfo)
    }

    private val simulationResults =
        mutableListOf<Pair<TrajectoryInfoRenderer, TrajectoryInfoRenderer.SimulationResult>>()

    override fun onDisabled() {
        simulationResults.clear()
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
                type = TrajectoryInfoRenderer.Type.REAL,
                renderOffset = Vec3d.ZERO
            )

            val color = TrajectoryData.getColorForEntity(it)

            simulationResults += trajectoryRenderer to trajectoryRenderer.drawTrajectoryForProjectile(
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
                drawHypotheticalTrajectory(otherPlayer, event)
            }
        } else {
            drawHypotheticalTrajectory(player, event)
        }

        debugParameter("TrajectoryCount") { simulationResults.size }
    }

    /**
     * Draws the trajectory for an item in the player's hand
     */
    private fun drawHypotheticalTrajectory(
        otherPlayer: PlayerEntity,
        event: WorldRenderEvent
    ) {
        val trajectoryInfo = otherPlayer.handItems.firstNotNullOfOrNull {
            TrajectoryData.getRenderedTrajectoryInfo(otherPlayer, it.item, this.alwaysShowBow)
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

        val renderer = TrajectoryInfoRenderer.getHypotheticalTrajectory(
            entity = otherPlayer,
            trajectoryInfo = trajectoryInfo,
            rotation = rotation,
            partialTicks = event.partialTicks
        )

        simulationResults += renderer to renderer.drawTrajectoryForProjectile(
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
