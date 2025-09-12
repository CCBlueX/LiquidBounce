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
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.minecraft.entity.Entity
import net.minecraft.entity.Ownable
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.*
import net.minecraft.entity.projectile.thrown.*
import net.minecraft.item.EggItem
import net.minecraft.item.EnderPearlItem
import net.minecraft.item.SnowballItem
import net.minecraft.util.math.Vec3d

@Suppress("MagicNumber")
object ModuleTrajectories : ClientModule("Trajectories", Category.RENDER) {

    private val maxSimulatedTicks by int("MaxSimulatedTicks", 240, 1..1000, "ticks")
    private val show by multiEnumChoice(
        "Show",
        Show.OTHER_PLAYERS,
        Show.ACTIVE_TRAJECTORY_ARROW
    )

    val alwaysShowBow get() = Show.ALWAYS_SHOW_BOW in show
    private val otherPlayers get() = Show.OTHER_PLAYERS in show
    private val activeTrajectoryArrow get() = Show.ACTIVE_TRAJECTORY_ARROW in show
    private val activeTrajectoryOther get() = Show.ACTIVE_TRAJECTORY_OTHER in show

    // ---------------- Projectile Types ----------------
    sealed class ProjectileType(name: String, defaultColor: Color4b) : ToggleableConfigurable(
        this, name, enabled = true) {
        val color by color("Color", defaultColor)
        val blockHitESP by boolean("BlockHitESP", true)
        val entityHitESP by boolean("EntityHitESP", true)

        object Arrow : ProjectileType("Arrow", Color4b(255, 0, 0, 100))
        object Potion : ProjectileType("Potion", Color4b(255, 192, 203, 100))
        object EnderPearl : ProjectileType("EnderPearl", Color4b(255, 0, 255, 100))
        object FishingBobber : ProjectileType("FishingBobber", Color4b(64, 64, 64, 100))
        object Trident : ProjectileType("Trident", Color4b(0, 255, 255, 100))
        object Snowball : ProjectileType("Snowball", Color4b(255, 255, 255, 100))
        object Egg : ProjectileType("Egg", Color4b(255, 255, 255, 100))
        object ExpBottle : ProjectileType("ExpBottle", Color4b(0, 255, 0, 100))
        object Fireball : ProjectileType("Fireball", Color4b(255, 165, 0, 100))
        object WindCharge : ProjectileType("WindCharge", Color4b(192, 192, 192, 100))
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

    private val simulationResults = mutableListOf<Pair
    <TrajectoryInfoRenderer, TrajectoryInfoRenderer.SimulationResult>>()

    override fun onDisabled() {
        simulationResults.clear()
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        simulationResults.clear()

        world.entities.forEach { entity ->
            val trajectoryInfo = TrajectoryData.getRenderTrajectoryInfoForOtherEntity(
                entity,
                activeTrajectoryArrow,
                activeTrajectoryOther
            ) ?: return@forEach

            val type = entity.categorize() ?: return@forEach
            if (!type.enabled || type.color.a <= 0) return@forEach

            val renderer = TrajectoryInfoRenderer(
                owner = (entity as? Ownable)?.owner ?: entity,
                velocity = entity.velocity,
                pos = entity.pos,
                trajectoryInfo = trajectoryInfo,
                type = TrajectoryInfoRenderer.Type.REAL,
                renderOffset = Vec3d.ZERO
            )

            simulationResults += renderer to renderer.drawTrajectoryForProjectile(
                maxSimulatedTicks,
                event,
                trajectoryColor = type.color,
                blockHitColor = if (type.blockHitESP) type.color else Color4b.TRANSPARENT,
                entityHitColor = if (type.entityHitESP) type.color else Color4b.TRANSPARENT
            )
        }

        // ---------------- Player & Others ----------------
        if (otherPlayers) {
            world.players.forEach { drawHypotheticalTrajectory(it, event) }
        } else {
            drawHypotheticalTrajectory(player, event)
        }
    }

    private fun drawHypotheticalTrajectory(playerEntity: PlayerEntity, event: WorldRenderEvent) {
        val trajectoryInfo = playerEntity.handItems.firstNotNullOfOrNull {
            TrajectoryData.getRenderedTrajectoryInfo(playerEntity, it.item, alwaysShowBow)
        } ?: return

        val type = trajectoryInfo.categorize() ?: return
        if (!type.enabled || type.color.a <= 0) return

        val rotation = if (playerEntity === player) {
            if (ModuleFreeCam.running){
                RotationManager.serverRotation
            }
            else{
                RotationManager.activeRotationTarget?.rotation
                    ?: RotationManager.currentRotation
                    ?: playerEntity.rotation
            }
        } else {
            playerEntity.rotation
        }

        val renderer = TrajectoryInfoRenderer.getHypotheticalTrajectory(
            entity = playerEntity,
            trajectoryInfo = trajectoryInfo,
            rotation = rotation,
            partialTicks = event.partialTicks
        )

        simulationResults += renderer to renderer.drawTrajectoryForProjectile(
            maxSimulatedTicks,
            event,
            trajectoryColor = type.color,
            blockHitColor = if (type.blockHitESP) type.color else Color4b.TRANSPARENT,
            entityHitColor = if (type.entityHitESP) type.color else Color4b.TRANSPARENT
        )
    }


    private enum class Show(override val choiceName: String) : NamedChoice {
        ALWAYS_SHOW_BOW("AlwaysShowBow"),
        OTHER_PLAYERS("OtherPlayers"),
        ACTIVE_TRAJECTORY_ARROW("ActiveTrajectoryArrow"),
        ACTIVE_TRAJECTORY_OTHER("ActiveTrajectoryOther"),
    }

    // ---------------- Categorize ----------------
    @JvmStatic
    fun Entity.categorize(): ProjectileType? = when (this) {
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

}
