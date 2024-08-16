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
package net.ccbluex.liquidbounce.features.module.modules.movement.autododge

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.render.murdermystery.ModuleMurderMystery
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.EventScheduler
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulation
import net.ccbluex.liquidbounce.utils.entity.SimulatedArrow
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object ModuleAutoDodge : Module("AutoDodge", Category.COMBAT) {
    private object AllowRotationChange : ToggleableConfigurable(this, "AllowRotationChange", false) {
        val allowJump by boolean("AllowJump", true)
    }

    private object AllowTimer : ToggleableConfigurable(this, "AllowTimer", false) {
        val timerSpeed by float("TimerSpeed", 2.0F, 1.0F..10.0F, suffix = "x")
    }

    init {
        tree(AllowRotationChange)
        tree(AllowTimer)
    }

    @Suppress("unused")
    val tickRep = handler<MovementInputEvent> { event ->
        // We aren't where we are because of blink. So this module shall not cause any disturbance in that case.
        if (ModuleBlink.enabled) {
            return@handler
        }
        if (ModuleMurderMystery.disallowsArrowDodge()) {
            return@handler
        }

        val world = world

        val arrows = findFlyingArrows(world)

        val input = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(event.directionalInput)

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(input)

        val inflictedHit =
            getInflictedHits(simulatedPlayer, arrows, hitboxExpansion = DodgePlanner.SAFE_DISTANCE_WITH_PADDING) {}
                ?: return@handler

        val dodgePlan =
            planEvasion(DodgePlannerConfig(allowRotations = AllowRotationChange.enabled), inflictedHit)
                ?: return@handler

        event.directionalInput = dodgePlan.directionalInput

        dodgePlan.yawChange?.let { yawChange ->
            player.yaw = yawChange
        }

        if (dodgePlan.shouldJump && AllowRotationChange.allowJump && player.isOnGround) {
            EventScheduler.schedule<MovementInputEvent>(ModuleScaffold) {
                it.jumping = true
            }
        }

        if (AllowTimer.enabled && dodgePlan.useTimer) {
            Timer.requestTimerSpeed(AllowTimer.timerSpeed, Priority.IMPORTANT_FOR_PLAYER_LIFE, this@ModuleAutoDodge)
        }
    }

    fun findFlyingArrows(world: ClientWorld): List<ArrowEntity> {
        return world.entities.mapNotNull {
            if (it !is ArrowEntity) {
                return@mapNotNull null
            }
            if (it.inGround) {
                return@mapNotNull null
            }

            return@mapNotNull it
        }
    }

    fun <T : PlayerSimulation> getInflictedHits(
        simulatedPlayer: T,
        arrows: List<ArrowEntity>,
        maxTicks: Int = 80,
        hitboxExpansion: Double = 0.7,
        behaviour: (T) -> Unit,
    ): HitInfo? {
        val simulatedArrows = arrows.map { SimulatedArrow(world, it.pos, it.velocity, false) }

        for (i in 0 until maxTicks) {
            behaviour(simulatedPlayer)

            simulatedPlayer.tick()

            simulatedArrows.forEachIndexed { arrowIndex, arrow ->
                if (arrow.inGround) {
                    return@forEachIndexed
                }

                val lastPos = arrow.pos

                arrow.tick()

                val playerHitBox =
                    Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3)
                        .expand(hitboxExpansion)
                        .offset(simulatedPlayer.pos)
                val raycastResult = playerHitBox.raycast(lastPos, arrow.pos)

                raycastResult.orElse(null)?.let { hitPos ->
                    return HitInfo(i, arrows[arrowIndex], hitPos, lastPos, arrow.velocity)
                }
            }
        }

        return null
    }

    data class HitInfo(
        val tickDelta: Int,
        val arrowEntity: ArrowEntity,
        val hitPos: Vec3d,
        val prevArrowPos: Vec3d,
        val arrowVelocity: Vec3d,
    )
}
