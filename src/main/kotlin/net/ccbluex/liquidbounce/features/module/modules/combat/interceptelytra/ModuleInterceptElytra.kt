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
package net.ccbluex.liquidbounce.features.module.modules.combat.interceptelytra

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3

/**
 * Shoots wind charges at elytra gliders to intercept them and knock them out of the air.
 *
 * Detects gliding players, predicts their trajectory and throws a wind charge at the computed
 * interception point. The explosion's radial knockback (multiplier 1.22, ~2.4 block falloff) breaks
 * the glider's flight path; a direct hit additionally deals 1.0 damage. Creative/spectator players are
 * ignored — they are immune to the knockback. Teammates and friends (as configured in Teams/FriendManager)
 * are also excluded.
 *
 * @see InterceptElytraSolver
 */
object ModuleInterceptElytra : ClientModule("InterceptElytra", ModuleCategories.COMBAT) {

    private const val MILLISECONDS_PER_TICK = 50
    private const val MAX_VERIFICATION_TICKS = 300
    private const val VERIFY_TOLERANCE_SQ = 4.0 // 2 blocks, covers sampling step + hitbox radius

    private val range by float("Range", 32f, 10f..64f)
    private val requireGliding by boolean("RequireGliding", true)
    private val minimumTargetSpeed by float("MinimumTargetSpeed", 0.2f, 0f..5f)
    private val aimMode by enumChoice("AimMode", AimMode.INTERCEPT_POINT)
    private val predictionMode by enumChoice("PredictionMode", PredictionMode.LINEAR)
    private val predictionMultiplier by floatRange("PredictionMultiplier", 1.8f..2.0f, 0.5f..3f)
    private val aimVerticalOffset by float("AimVerticalOffset", 0f, -10f..10f)
    private val maxFlightTicks by int("MaxFlightTicks", 30, 10..60)
    private val cooldown by intRange("Cooldown", 8..12, 1..50, "ticks")
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..20, "ticks")
    private val aimOffThreshold by float("AimOffThreshold", 2f, 0.5f..10f)
    private val considerInventory by boolean("ConsiderInventory", true)
    private val requireLineOfSight by boolean("RequireLineOfSight", true)
    private val verifyHit by boolean("VerifyHit", false)
    private val rotations = tree(RotationsValueGroup(this))

    private val chronometer = Chronometer()

    private val cooldownReached: Boolean
        get() = chronometer.hasElapsed((cooldown.random() * MILLISECONDS_PER_TICK).toLong())

    @Suppress("unused")
    private val interceptHandler = tickHandler {
        if (player.isUsingItem || (considerInventory && InventoryManager.isInventoryOpen)) {
            return@tickHandler
        }

        val target = selectBestGlider() ?: return@tickHandler
        val slot = Slots.OffhandWithHotbar.findSlot(Items.WIND_CHARGE) ?: return@tickHandler

        val aim = calculateAim(target)

        if (verifyHit && !passesVerification(target, aim.rotation, aim.flightTicks)) {
            return@tickHandler
        }

        RotationManager.setRotationTarget(
            rotations.toRotationTarget(aim.rotation, considerInventory = considerInventory),
            Priority.IMPORTANT_FOR_USAGE_2,
            this@ModuleInterceptElytra,
        )

        if (RotationManager.serverRotation.directionAngleTo(aim.rotation) <= aimOffThreshold && cooldownReached) {
            useHotbarSlotOrOffhand(slot, slotResetDelay.random(), aim.rotation.yaw, aim.rotation.pitch)
            chronometer.reset()
        }
    }

    /**
     * Selects the closest valid glider in a single O(k) pass without sorting or temporary
     * collections (O(1) auxiliary space).
     */
    private fun selectBestGlider(): LivingEntity? {
        var bestTarget: LivingEntity? = null
        var bestDistanceSq = Double.POSITIVE_INFINITY

        for (entity in world.entitiesForRendering()) {
            val glider = asTargetableGlider(entity) ?: continue

            val distanceSq = glider.squaredBoxedDistanceTo(player)
            // Line of sight is checked last, only for candidates inside range that beat the best
            // so far, to avoid raycasts for entities that can never win.
            if (distanceSq <= range.sq() && distanceSq < bestDistanceSq && hasLineOfSight(glider)) {
                bestDistanceSq = distanceSq
                bestTarget = glider
            }
        }

        return bestTarget
    }

    /** Casts to a gliding player that can be knocked out of the air, or null when not targetable. */
    private fun asTargetableGlider(entity: Entity): LivingEntity? {
        val glider = entity as? Player ?: return null
        val hasSpeed = glider.deltaMovement.horizontalDistance() >= minimumTargetSpeed

        return glider.takeIf { candidate ->
            candidate !== player &&
                candidate.isAlive &&
                (!requireGliding || candidate.isFallFlying) &&
                !candidate.abilities.instabuild && // creative/spectator players are immune to knockback
                candidate.shouldBeAttacked() && // respect friend list and team settings
                hasSpeed
        }
    }

    private fun hasLineOfSight(glider: LivingEntity): Boolean =
        !requireLineOfSight || player.hasLineOfSight(glider)

    /** Rotation plus the flight time used for prediction. */
    private data class Aim(val rotation: Rotation, val flightTicks: Double)

    private fun calculateAim(target: LivingEntity): Aim {
        val eye = player.eyePosition
        // Vanilla zeroes the thrower's vertical velocity when grounded (shootFromRotation).
        val ownVelocity = Vec3(
            player.deltaMovement.x,
            if (player.onGround()) 0.0 else player.deltaMovement.y,
            player.deltaMovement.z,
        )
        val targetPosition = target.getEyePosition()

        return when (aimMode) {
            AimMode.INTERCEPT_POINT -> {
                val solution = InterceptElytraSolver.solveWindChargeIntercept(
                    eye,
                    ownVelocity,
                    targetPosition,
                    target.deltaMovement,
                    maxFlightTicks.toDouble(),
                    aimVerticalOffset,
                )

                if (solution != null) {
                    Aim(solution.rotation, solution.flightTicks)
                } else {
                    directAim(eye, target, targetPosition)
                }
            }

            AimMode.DIRECT -> directAim(eye, target, targetPosition)
        }
    }

    /** Aims at the predicted target position without the closed-form interception. */
    private fun directAim(eye: Vec3, target: LivingEntity, targetPosition: Vec3): Aim {
        val flightTicks = InterceptElytraSolver.estimateFlightTicks(targetPosition.distanceTo(eye))
        val predicted = when (predictionMode) {
            PredictionMode.LINEAR -> InterceptElytraSolver.predictGliderLinear(
                targetPosition,
                target.deltaMovement,
                flightTicks,
                predictionMultiplier.random().toDouble(),
            )

            PredictionMode.SIMULATED -> PositionExtrapolation.getBestForEntity(target).getPositionInTicks(flightTicks)
        }

        val aimPoint = InterceptElytraSolver.applyVerticalOffset(eye, predicted, aimVerticalOffset)
        return Aim(Rotation.lookingAt(aimPoint, eye), flightTicks)
    }

    /**
     * Validates the shot by simulating the wind charge trajectory and checking it reaches the
     * predicted impact point. Bounded to [MAX_VERIFICATION_TICKS] ticks, hence constant cost.
     *
     * The simulation world is static — entity hitboxes never move — while the aim leads the target,
     * so the trajectory is compared against the target's predicted position instead of an entity
     * reference.
     */
    private fun passesVerification(target: LivingEntity, rotation: Rotation, flightTicks: Double): Boolean {
        val predictedImpact = InterceptElytraSolver.predictGliderLinear(
            target.getEyePosition(),
            target.deltaMovement,
            flightTicks,
        )

        val result = TrajectoryInfoRenderer.getHypotheticalTrajectory(
            player,
            TrajectoryInfo.WIND_CHARGE,
            TrajectoryType.WindCharge,
            rotation,
        ).runSimulation(MAX_VERIFICATION_TICKS)

        // The projectile samples one point per tick (1.5 blocks apart); tolerance covers the
        // sampling step and the 1.0-block projectile hitbox.
        return result.positions.any { it.distanceToSqr(predictedImpact) <= VERIFY_TOLERANCE_SQ }
    }

    private enum class AimMode(override val tag: String) : Tagged {
        INTERCEPT_POINT("InterceptPoint"),
        DIRECT("Direct"),
    }

    private enum class PredictionMode(override val tag: String) : Tagged {
        LINEAR("Linear"),
        SIMULATED("Simulated"),
    }
}
