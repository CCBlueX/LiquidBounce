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
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.entity.projectile.FireballEntity
import net.minecraft.entity.projectile.FishingBobberEntity
import net.minecraft.entity.projectile.TridentEntity
import net.minecraft.entity.projectile.WindChargeEntity
import net.minecraft.entity.projectile.thrown.*
import net.minecraft.item.EggItem
import net.minecraft.item.EnderPearlItem
import net.minecraft.item.SnowballItem
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
        Show.ALWAYS_SHOW_BOW,
        Show.ACTIVE_TRAJECTORY_ARROW,
        Show.ACTIVE_TRAJECTORY_OTHER
    )

    val alwaysShowBow get() = Show.ALWAYS_SHOW_BOW in show
    private val otherPlayers get() = Show.OTHER_PLAYERS in show
    private val activeTrajectoryArrow get() = Show.ACTIVE_TRAJECTORY_ARROW in show
    private val activeTrajectoryOther get() = Show.ACTIVE_TRAJECTORY_OTHER in show

    sealed class ProjectileType(name: String, defaultColor: Color4b) : ToggleableConfigurable(
        this, name, enabled = true) {
        val color by color("Color", defaultColor)
        val blockHitESP by boolean("BlockHitESP", true)
        val entityHitESP by boolean("EntityHitESP", true)

        object Arrow : ProjectileType("Arrow", Color4b(Color.RED).withAlpha(100))
        object Potion : ProjectileType("Potion", Color4b(Color.PINK).withAlpha(100))
        object EnderPearl : ProjectileType("EnderPearl", Color4b(Color.MAGENTA).withAlpha(100))
        object FishingBobber : ProjectileType("FishingBobber", Color4b(Color.DARK_GRAY).withAlpha(100))
        object Trident : ProjectileType("Trident", Color4b(Color.CYAN).withAlpha(100))
        object Snowball : ProjectileType("Snowball", Color4b(Color.WHITE).withAlpha(100))
        object Egg : ProjectileType("Egg", Color4b(Color.WHITE).withAlpha(100))
        object ExpBottle : ProjectileType("ExpBottle", Color4b(Color.GREEN).withAlpha(100))
        object Fireball : ProjectileType("Fireball", Color4b(Color.ORANGE).withAlpha(100))
        object WindCharge : ProjectileType("WindCharge", Color4b(Color.LIGHT_GRAY).withAlpha(100))
    }

    init {
        tree(ProjectileType.Arrow)
        tree(ProjectileType.Potion)
        tree(ProjectileType.EnderPearl)
        tree(ProjectileType.FishingBobber)
        tree(ProjectileType.Trident)
        tree(ProjectileType.Snowball)
        tree(ProjectileType.Egg)
        tree(ProjectileType.ExpBottle)
        tree(ProjectileType.Fireball)
        tree(ProjectileType.WindCharge)
    }

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

            val type = it.categorize() ?: return@forEach
            if (!type.enabled || type.color.a <= 0) return@forEach

            val trajectoryRenderer = TrajectoryInfoRenderer(
                owner = it,
                velocity = it.velocity,
                pos = it.pos,
                trajectoryInfo = trajectoryInfo,
                renderOffset = Vec3d.ZERO
            )

            val hitResult = trajectoryRenderer.drawTrajectoryForProjectile(maxSimulatedTicks, type.color, matrixStack)

            if (hitResult != null && !(hitResult is EntityHitResult && hitResult.entity == player)) {
                drawLandingPos(
                    hitResult,
                    trajectoryInfo,
                    event,
                    if (type.blockHitESP) type.color else Color4b.TRANSPARENT,
                    if (type.entityHitESP) type.color else Color4b.TRANSPARENT
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

        val type = trajectoryInfo.categorize() ?: return
        if (!type.enabled || type.color.a <= 0) return

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

        val hitResult = renderer.drawTrajectoryForProjectile(maxSimulatedTicks, type.color, event.matrixStack)

        if (hitResult != null) {
            if (hitResult is EntityHitResult && type.entityHitESP) {
                drawLandingPos(
                    hitResult,
                    trajectoryInfo,
                    event,
                    Color4b.TRANSPARENT,
                    type.color
                )
            } else if (type.blockHitESP && blockHitRenderer.enabled) {
                blockHitRenderer.render(true, event, hitResult, overrideColor = type.color)
            }
        }
    }

    private enum class Show(
        override val choiceName: String
    ) : NamedChoice {
        ALWAYS_SHOW_BOW("AlwaysShowBow"),
        OTHER_PLAYERS("OtherPlayers"),
        ACTIVE_TRAJECTORY_ARROW("ActiveTrajectoryArrow"),
        ACTIVE_TRAJECTORY_OTHER("ActiveTrajectoryOther"),
    }

    @JvmStatic
    fun TrajectoryInfo.categorize(): ProjectileType? {
        return when {
            this === TrajectoryInfo.POTION -> ProjectileType.Potion
            this === TrajectoryInfo.TRIDENT -> ProjectileType.Trident
            this === TrajectoryInfo.GENERIC -> when {
                player.handItems.any { it.item is EnderPearlItem } -> ProjectileType.EnderPearl
                player.handItems.any { it.item is SnowballItem } -> ProjectileType.Snowball
                player.handItems.any { it.item is EggItem } -> ProjectileType.Egg
                else -> null
            }
            this === TrajectoryInfo.EXP_BOTTLE -> ProjectileType.ExpBottle
            this === TrajectoryInfo.FIREBALL -> ProjectileType.Fireball
            this === TrajectoryInfo.FISHING_ROD -> ProjectileType.FishingBobber
            this === TrajectoryInfo.WIND_CHARGE -> ProjectileType.WindCharge
            this === TrajectoryInfo.BOW_FULL_PULL ||
                this === TrajectoryInfo.PERSISTENT ||
                (this.gravity == TrajectoryInfo.BOW_FULL_PULL.gravity &&
                    this.hitboxRadius == TrajectoryInfo.BOW_FULL_PULL.hitboxRadius) ->
                ProjectileType.Arrow
            else -> null
        }
    }


    @JvmStatic
    fun Entity.categorize(): ProjectileType? {
        return when (this) {
            is ArrowEntity -> ProjectileType.Arrow
            is PotionEntity -> ProjectileType.Potion
            is EnderPearlEntity -> ProjectileType.EnderPearl
            is FishingBobberEntity -> ProjectileType.FishingBobber
            is TridentEntity -> ProjectileType.Trident
            is SnowballEntity -> ProjectileType.Snowball
            is EggEntity -> ProjectileType.Egg
            is ExperienceBottleEntity -> ProjectileType.ExpBottle
            is FireballEntity -> ProjectileType.Fireball
            is WindChargeEntity -> ProjectileType.WindCharge
            else -> null
        }
    }
}
