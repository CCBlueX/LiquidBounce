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

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
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
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.TridentItem
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

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
    val requiresHypotheticalHit by boolean("RequiresHypotheticalHit", false)

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

        val currentItem = player.useItem?.item

        // Should check if player is using bow
        if (currentItem !is BowItem && currentItem !is TridentItem) {
            return@handler
        }

        if (player.ticksUsingItem < charged + getChargedRandom()) { // Wait until the bow is fully charged
            return@handler
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

        forceUncharged = true
        updateChargeRandom()
    }

    @Suppress("unused")
    private val keybindHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding == mc.options.keyUse && forceUncharged) {
            event.isPressed = false
        }
    }

    private val playerHitboxBase = AABB(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3)

    fun getHypotheticalHit(): AbstractClientPlayer? {
        val rotation = RotationManager.serverRotation
        val yaw = rotation.yaw
        val pitch = rotation.pitch

        val velocity = (TrajectoryInfo.bowWithUsageDuration() ?: return null).initialVelocity

        val vX = -yaw.toRadians().fastSin() * pitch.toRadians().fastCos() * velocity
        val vY = -pitch.toRadians().fastSin() * velocity
        val vZ = yaw.toRadians().fastCos() * pitch.toRadians().fastCos() * velocity

        val arrow = SimulatedArrow(
            world,
            player.eyePosition,
            Vec3(vX, vY, vZ),
            collideEntities = false
        )

        val players = findAndBuildSimulatedPlayers()

        for (i in 0 until 40) {
            val lastPos = arrow.pos

            arrow.tick()

            players.forEach { (entity, player) ->
                val playerSnapshot = player.getSnapshotAt(i)

                val playerHitBox =
                    playerHitboxBase.inflate(0.3).move(playerSnapshot.pos)

                val raycastResult = playerHitBox.clip(lastPos, arrow.pos)

                raycastResult.orElse(null)?.let {
                    return entity
                }
            }
        }

        return null
    }

    private fun findAndBuildSimulatedPlayers(): List<Pair<AbstractClientPlayer, SimulatedPlayerCache>> {
        return world.players().filter {
            it != player &&
                Line(player.position(), player.lookAngle).squaredDistanceTo(it.position()) < 10.0 * 10.0
        }.map {
            Pair(it, PlayerSimulationCache.getSimulationForOtherPlayers(it))
        }
    }

    override fun onDisabled() {
        forceUncharged = false
        super.onDisabled()
    }

}
