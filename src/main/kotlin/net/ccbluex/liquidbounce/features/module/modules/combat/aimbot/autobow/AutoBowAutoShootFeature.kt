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

package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.SimulatedArrow
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerCache
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.useItem
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TridentItem
import net.minecraft.world.phys.Vec3

object AutoBowAutoShootFeature : ToggleableValueGroup(ModuleAutoBow, "AutoShoot", true) {

    private val charged by int("Charged", 15, 3..20, suffix = "ticks")

    private val chargedRandom by floatRange(
        "ChargedRandom",
        0.0F..0.0F,
        -10.0F..10.0F,
        suffix = "ticks"
    )
    private val delayBetweenShots by float("DelayBetweenShots", 0.0F, 0.0F..5.0F, suffix = "s")
    private val aimThreshold by float("AimThreshold", 1.5F, 1.0F..4.0F, suffix = "°")
    private val requiresHypotheticalHit by boolean("RequiresHypotheticalHit", false)
    private val usePrechargedCrossbow by boolean("UsePrechargedCrossbow", false)

    private var currentChargeRandom: Int? = null

    private fun updateChargeRandom() {
        val lenHalf = (chargedRandom.endInclusive - chargedRandom.start) / 2.0F
        val mid = chargedRandom.start + lenHalf

        currentChargeRandom =
            (mid + ModuleAutoBow.random.nextGaussian() * lenHalf).toInt()
                .coerceIn(chargedRandom.start.toInt()..chargedRandom.endInclusive.toInt())
    }

    private fun getChargedRandom(): Int {
        if (currentChargeRandom == null) {
            updateChargeRandom()
        }

        return currentChargeRandom!!
    }

    private var forceUncharged = false

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        forceUncharged = false

        val usingItemHand = player.usingItemHand
            ?: if (usePrechargedCrossbow) {
                InteractionHand.entries.find { player.getItemInHand(it).isChargedCrossbow } ?: return@handler
            } else {
                return@handler
            }

        val usingItemStack = if (player.isUsingItem) player.useItem else player.getItemInHand(usingItemHand)

        when (usingItemStack.item) {
            is CrossbowItem -> {
                val pullTime = CrossbowItem.getChargeDuration(usingItemStack, player)
                val isChargedNow = usingItemStack.isChargedCrossbow
                if (!isChargedNow && player.ticksUsingItem < pullTime) {
                    return@handler
                }
            }

            is BowItem -> {
                if (player.ticksUsingItem < charged + getChargedRandom()) {
                    return@handler
                }
            }

            is TridentItem -> {
                if (player.ticksUsingItem <= TridentItem.THROW_THRESHOLD_TIME) {
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

        if (usingItemStack.item is CrossbowItem) {
            val isChargedNow = usingItemStack.isChargedCrossbow
            if (isChargedNow) {
                useItem(usingItemHand)
                ModuleAutoBow.lastShotTimer.reset()
            } else {
                forceUncharged = true
            }
        } else {
            forceUncharged = true
            if (usingItemStack.item is BowItem) {
                updateChargeRandom()
            }
        }
    }

    @Suppress("unused")
    private val keybindHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding == mc.options.keyUse && forceUncharged) {
            event.isPressed = false
        }
    }

    private fun getHypotheticalHit(): Entity? {
        player.usingItemHand ?: return null
        val rotation = RotationManager.serverRotation
        val yaw = rotation.yaw
        val pitch = rotation.pitch

        val (trajectoryInfo, _) =
            TrajectoryData.getRenderedTrajectoryInfo(player, player.activeItem, false) ?: return null

        val velocity = trajectoryInfo.initialVelocity

        val vX = -yaw.toRadians().fastSin() * pitch.toRadians().fastCos() * velocity
        val vY = -pitch.toRadians().fastSin() * velocity
        val vZ = yaw.toRadians().fastCos() * pitch.toRadians().fastCos() * velocity

        val arrow = SimulatedArrow(
            world,
            player.eyePosition,
            Vec3(vX, vY, vZ),
            collideEntities = false
        )

        val entities = findAndBuildSimulatedEntities()

        for (i in 0 until 40) {
            val lastPos = arrow.pos

            arrow.tick()

            entities.forEach { (entity, simulatedPos) ->
                val predictedPos = if (entity is AbstractClientPlayer && simulatedPos != null) {
                    simulatedPos.getSnapshotAt(i).pos
                } else {
                    entity.position().add(entity.deltaMovement.scale(i.toDouble()))
                }

                val entityBox = entity.boundingBox
                    .inflate(0.3)
                    .move(predictedPos.subtract(entity.position()))

                if (entityBox.clip(lastPos, arrow.pos).isPresent) {
                    return entity
                }
            }
        }

        return null
    }

    private fun findAndBuildSimulatedEntities(): List<Pair<Entity, SimulatedPlayerCache?>> {
        return world.entitiesForRendering().filter { entity ->
            entity != player &&
                entity.shouldBeAttacked() &&
                Line(player.position(), player.rotation.directionVector)
                    .squaredDistanceTo(entity.position()) < 10.0 * 10.0
        }.map { entity ->
            val simulation = if (entity is AbstractClientPlayer) {
                PlayerSimulationCache.getSimulationForOtherPlayers(entity)
            } else {
                null
            }
            Pair(entity, simulation)
        }
    }

    private inline val ItemStack.isChargedCrossbow
        get() = CrossbowItem.isCharged(this)

    override fun onDisabled() {
        forceUncharged = false
        super.onDisabled()
    }

}
