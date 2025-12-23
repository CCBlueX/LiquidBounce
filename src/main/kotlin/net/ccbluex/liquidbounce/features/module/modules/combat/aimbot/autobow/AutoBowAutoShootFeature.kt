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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.SimulatedArrow
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerCache
import net.ccbluex.liquidbounce.utils.inventory.interactItem
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.item.BowItem
import net.minecraft.item.CrossbowItem
import net.minecraft.item.TridentItem
import net.minecraft.util.Hand
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

object AutoBowAutoShootFeature : ToggleableConfigurable(ModuleAutoBow, "AutoShoot", true) {

    val charged by int("Charged", 15, 3..20, suffix = "ticks")

    val chargedRandom by floatRange(
        "ChargedRandom",
        0.0F..0.0F,
        -10.0F..10.0F,
        suffix = "ticks"
    )
    val delayBetweenShots by float("DelayBetweenShots", 0.0F, 0.0F..5.0F, suffix = "s")
    val aimThreshold by float("AimThreshold", 1.5F, 1.0F..4.0F, suffix = "°")
    val requiresHypotheticalHit by boolean("RequiresHypotheticalHit", true)

    var currentChargeRandom: Int? = null

    fun updateChargeRandom() {
        val lenHalf = (chargedRandom.endInclusive - chargedRandom.start) / 2.0F
        val mid = chargedRandom.start + lenHalf

        currentChargeRandom =
            (mid + ModuleAutoBow.random.nextGaussian() * lenHalf).toInt()
                .coerceIn(chargedRandom.start.toInt()..chargedRandom.endInclusive.toInt())
    }

    fun getChargedRandom(): Int {
        if (currentChargeRandom == null) {
            updateChargeRandom()
        }

        return currentChargeRandom!!
    }

    private var forceUncharged = false


    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        forceUncharged = false

        val currentItem = player.activeItem?.item
        val stack = player.mainHandStack

        when (currentItem) {
            is CrossbowItem -> {
                val pullTime = CrossbowItem.getPullTime(stack, player)
                val isChargedNow = CrossbowItem.isCharged(stack)
                if (!isChargedNow && player.itemUseTime < pullTime) {
                    return@handler
                }
            }

            is BowItem -> {
                if (player.itemUseTime < charged + getChargedRandom()) {
                    return@handler
                }
            }

            is TridentItem -> {
                if (player.itemUseTime <= TridentItem.MIN_DRAW_DURATION) {
                    return@handler
                }
            }

            else -> return@handler
        }

        if (!ModuleAutoBow.lastShotTimer.hasElapsed((delayBetweenShots * 1000.0F).toLong())) {
            return@handler
        }

        if (requiresHypotheticalHit) {
            val hypotheticalHit = getHypotheticalHit()
            if (hypotheticalHit == null || !hypotheticalHit.shouldBeAttacked()) {
                return@handler
            }
        } else if (AutoBowAimbotFeature.enabled) {
            if (AutoBowAimbotFeature.targetTracker.target == null) {
                return@handler
            }

            val targetRotation = RotationManager.activeRotationTarget ?: return@handler

            val aimDifference = RotationManager.serverRotation.angleTo(targetRotation.rotation)

            if (aimDifference > aimThreshold) {
                return@handler
            }
        }

        if (currentItem is CrossbowItem) {
            val isChargedNow = CrossbowItem.isCharged(stack)
            if (isChargedNow) {
                interactItem(Hand.MAIN_HAND)
                ModuleAutoBow.lastShotTimer.reset()
            } else {
                forceUncharged = true
            }
        } else {
            forceUncharged = true
            if (currentItem is BowItem) {
                updateChargeRandom()
            }
        }
    }


    @Suppress("unused")
    private val keybindHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding == mc.options.useKey && forceUncharged) {
            event.isPressed = false
        }
    }

    private fun getHypotheticalHit(): Entity? {
        val rotation = RotationManager.serverRotation
        val yaw = rotation.yaw
        val pitch = rotation.pitch

        val trajectoryInfo = when (player.activeItem?.item) {
            is BowItem -> TrajectoryInfo.bowWithUsageDuration()
            is CrossbowItem -> TrajectoryInfo.BOW_FULL_PULL
            is TridentItem -> TrajectoryInfo.TRIDENT
            else -> null
        } ?: return null

        val velocity = trajectoryInfo.initialVelocity

        val vX = -MathHelper.sin(yaw.toRadians()) * MathHelper.cos(pitch.toRadians()) * velocity
        val vY = -MathHelper.sin(pitch.toRadians()) * velocity
        val vZ = MathHelper.cos(yaw.toRadians()) * MathHelper.cos(pitch.toRadians()) * velocity

        val arrow = SimulatedArrow(
            world,
            player.eyePos,
            Vec3d(vX, vY, vZ),
            collideEntities = false
        )

        val entities = findAndBuildSimulatedEntities()

        for (i in 0 until 40) {
            val lastPos = arrow.pos
            arrow.tick()

            entities.forEach { (entity, simulatedPos) ->
                val predictedPos = if (entity is AbstractClientPlayerEntity && simulatedPos is SimulatedPlayerCache) {
                    simulatedPos.getSnapshotAt(i).pos
                } else {
                    entity.pos.add(entity.velocity.multiply(i.toDouble()))
                }

                val entityBox = entity.boundingBox
                    .expand(0.3)
                    .offset(predictedPos.subtract(entity.pos))

                val raycastResult = entityBox.raycast(lastPos, arrow.pos)
                raycastResult.orElse(null)?.let {
                    return entity
                }
            }
        }

        return null
    }

    private fun findAndBuildSimulatedEntities(): List<Pair<Entity, Any?>> {
        return world.entities.filter { entity ->
            entity != player &&
                entity.shouldBeAttacked() &&
                Line(player.pos, player.rotationVector).squaredDistanceTo(entity.pos) < 10.0 * 10.0
        }.map { entity ->
            val simulation = if (entity is AbstractClientPlayerEntity) {
                PlayerSimulationCache.getSimulationForOtherPlayers(entity)
            } else {
                null
            }
            Pair(entity, simulation)
        }
    }

    override fun onDisabled() {
        forceUncharged = false
        super.onDisabled()
    }

}
