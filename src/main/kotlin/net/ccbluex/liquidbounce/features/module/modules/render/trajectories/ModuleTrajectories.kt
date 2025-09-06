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
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.render.BlockHitRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.math.Vec3d
import java.awt.Color

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
        Show.ACTIVE_TRAJECTORY_ARROW,
        Show.BLOCK_HIT_ESP
    )
    val arrowColor by color("Arrow", Color4b(Color.RED).withAlpha(100))
    val potionColor by color("Potion", Color4b(Color.PINK).withAlpha(100))
    val enderPearlColor by color("EnderPearl", Color4b(Color.MAGENTA).withAlpha(100))
    val fishingBobberColor by color("FishBobber", Color4b(Color.DARK_GRAY).withAlpha(100))
    val tridentColor by color("Trident", Color4b(Color.CYAN).withAlpha(100))
    val snowballColor by color("Snowball", Color4b(Color.WHITE).withAlpha(100))
    val eggColor by color("Egg", Color4b(Color.WHITE).withAlpha(100))
    val expBottleColor by color("ExpBottle", Color4b(Color.GREEN).withAlpha(100))
    val fireballColor by color("Fireball", Color4b(Color.ORANGE).withAlpha(100))
    val windChargeColor by color("WindCharge", Color4b(Color.LIGHT_GRAY).withAlpha(100))
    val entityHitColor by color("EntityHit", Color4b(255, 0, 0, 100))

    val enableBlockHitESP get() = Show.BLOCK_HIT_ESP in show
    val enableEntityHitColor get() = Show.ENTITY_HIT_ESP in show
    val alwaysShowBow get() = Show.ALWAYS_SHOW_BOW in show
    private val otherPlayers get() = Show.OTHER_PLAYERS in show
    private val activeTrajectoryArrow get() = Show.ACTIVE_TRAJECTORY_ARROW in show
    private val activeTrajectoryOther get() = Show.ACTIVE_TRAJECTORY_OTHER in show
    private val enableEntityHitESP get() = Show.ENTITY_HIT_ESP in show

    private val blockHitRenderer = tree(BlockHitRenderer(this))
    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
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
                drawLandingPos(hitResult,
                    trajectoryInfo, event,
                    if (enableBlockHitESP)color else Color4b.TRANSPARENT,
                    if (enableEntityHitColor) color else Color4b.TRANSPARENT
                )
            }
        }

        if (otherPlayers) {
            for (otherPlayer in world.players) {
                if (otherPlayer != player) {
                    drawHypotheticalTrajectory(otherPlayer, event)
                }
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

        val rotation = if (otherPlayer == player) {
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

        val hitResult = renderer.drawTrajectoryForProjectile(maxSimulatedTicks, Color4b.WHITE, event.matrixStack)

        if (hitResult != null) {
            if (hitResult is EntityHitResult && enableEntityHitESP) {
                drawLandingPos(
                    hitResult,
                    trajectoryInfo,
                    event,
                    Color4b.TRANSPARENT, // No block hit rendering here
                    entityHitColor // Entity hit color
                )
            } else if (enableBlockHitESP) {
                blockHitRenderer.render(true, event, hitResult)
            }
        }
    }

    private enum class Show(
        override val choiceName: String
    ) : NamedChoice {
        BLOCK_HIT_ESP("BlockHitESP"),
        ENTITY_HIT_ESP("EntityHitESP"),
        ALWAYS_SHOW_BOW("AlwaysShowBow"),
        OTHER_PLAYERS("OtherPlayers"),
        ACTIVE_TRAJECTORY_ARROW("ActiveTrajectoryArrow"),
        ACTIVE_TRAJECTORY_OTHER("ActiveTrajectoryOther"),
    }
}
