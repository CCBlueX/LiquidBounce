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
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.entity.lastPos
import net.ccbluex.liquidbounce.utils.entity.ping
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
 * the glider's flight path; a direct hit additionally attempts 1.0 damage server-side. Spectators
 * are excluded client-side (wind charge explosions never damage anyone, and ability-driven flyers
 * get zero knockback — see SimpleExplosionDamageCalculator). No abilities check is needed: other
 * players' abilities are never synced to the client (see ClientboundPlayerAbilitiesPacket,
 * which carries no entity id), and ability-flyers cannot glide (see Player#canGlide). Teammates
 * (while Teams is enabled and matches) and friends (unless Friends is a global Combat target) are
 * also excluded. Blast Protection adds +0.15 explosion knockback resistance per level and piece
 * (see blast_protection.json); a full level-IV set takes zero knockback, and enchantments are not
 * synced to the client, so such targets cannot be distinguished beforehand.
 *
 * @see InterceptElytraSolver
 */
object ModuleInterceptElytra : ClientModule("InterceptElytra", ModuleCategories.COMBAT) {

    private const val MILLISECONDS_PER_TICK = 50
    // ~1.06 blocks: half the 1.5/t sampling step (0.75) plus the 0.3125 charge box; squared in use.
    private const val VERIFY_TOLERANCE_SQ = 1.2

    // Eye-to-hitbox distance (nearest box point), in blocks.
    private val range by float("Range", 32f, 10f..64f, "blocks")
    private val requireGliding by boolean("RequireGliding", true)
    // Horizontal (XZ) speed only; vertical dive speed is ignored.
    private val minimumTargetSpeed by float("MinimumTargetSpeed", 0.2f, 0f..5f, "blocks/tick")
    // INTERCEPT_POINT falls back to DIRECT when unsolvable (target outruns the projectile).
    private val aimMode by enumChoice("AimMode", AimMode.INTERCEPT_POINT)
    // Only applies to DIRECT aim (and the INTERCEPT_POINT fallback); ignored when closed-form solves.
    private val predictionMode by enumChoice("PredictionMode", PredictionMode.LINEAR)
    // Only used with LINEAR prediction (DIRECT mode, including the INTERCEPT_POINT fallback).
    private val predictionMultiplier by floatRange("PredictionMultiplier", 1.8f..2.0f, 0.5f..3f)
    // Caps the INTERCEPT_POINT solver and the VerifyHit sim; DIRECT aim is uncapped.
    private val maxFlightTicks by int("MaxFlightTicks", 30, 10..60, "ticks")
    // Wall-clock approx (ticks x 50 ms); diverges from game ticks under lag. Vanilla also enforces
    // a fixed 10-tick item cooldown (Items.useCooldown(0.5F)) — anything below it is inert.
    private val cooldown by intRange("Cooldown", 10..12, 10..50, "ticks")
    // 0 = restore immediately; raise if the server ignores the silent swap.
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..20, "ticks")
    private val aimOffThreshold by float("AimOffThreshold", 2f, 0.5f..10f, "°")
    private val considerInventory by boolean("ConsiderInventory", true)
    // Leads the target anchor by the tracking interval (2 ticks for players, EntityTypes.PLAYER)
    // plus half the round-trip time, so the solver sees where the target is, not its last report.
    private val lagCompensation by boolean("LagCompensation", true)
    private val requireLineOfSight by boolean("RequireLineOfSight", true)
    private val verifyHit by boolean("VerifyHit", false)
    private val rotations = tree(RotationsValueGroup(this))

    private val chronometer = Chronometer()

    // Rolled once per engagement so the aim point does not wobble between ticks.
    private var engagedTarget: LivingEntity? = null
    private var engagedMultiplier: Float = 1.0f

    private val cooldownReached: Boolean
        get() = chronometer.hasElapsed((cooldown.random() * MILLISECONDS_PER_TICK).toLong())

    @Suppress("unused")
    private val interceptHandler = handler<GameTickEvent> {
        if (player.isUsingItem || (considerInventory && InventoryManager.isInventoryOpen)) {
            return@handler
        }

        val target = selectBestGlider() ?: run {
            engagedTarget = null
            return@handler
        }

        if (target !== engagedTarget) {
            engagedTarget = target
            engagedMultiplier = predictionMultiplier.random()
        }

        val slot = Slots.OffhandWithHotbar.findSlot(Items.WIND_CHARGE) ?: return@handler

        val aim = calculateAim(target)

        if (verifyHit && !passesVerification(aim)) {
            return@handler
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

    override fun onEnabled() {
        // Otherwise the first eligible tick after enabling would shoot instantly.
        chronometer.reset()
    }

    override fun onDisabled() {
        engagedTarget = null
        engagedMultiplier = 1.0f
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
            // so far, to avoid line-of-sight checks for entities that can never win.
            if (distanceSq <= range.sq() && distanceSq < bestDistanceSq && hasLineOfSight(glider)) {
                bestDistanceSq = distanceSq
                bestTarget = glider
            }
        }

        return bestTarget
    }

    /** Casts to a player that can be knocked out of the air, or null when not targetable. */
    private fun asTargetableGlider(entity: Entity): LivingEntity? {
        val glider = entity as? Player ?: return null
        val hasSpeed = glider.deltaMovement.horizontalDistance() >= minimumTargetSpeed

        return glider.takeIf { candidate ->
            candidate !== player &&
                candidate.isAlive &&
                !candidate.isSpectator &&
                (!requireGliding || candidate.isFallFlying) &&
                candidate.shouldBeAttacked() && // honors the friend list and Teams tagging when those apply
                hasSpeed
        }
    }

    /** Eye-to-eye block clip with 128-block cutoff (see LivingEntity#hasLineOfSight). */
    private fun hasLineOfSight(glider: LivingEntity): Boolean =
        !requireLineOfSight || player.hasLineOfSight(glider)

    /** Rotation, flight time and the predicted impact point the aim was computed for. */
    private data class Aim(val rotation: Rotation, val flightTicks: Double, val predictedImpact: Vec3)

    private fun calculateAim(target: LivingEntity): Aim {
        val eye = player.eyePosition
        // Vanilla adds the thrower's getKnownMovement() (Projectile#shootFromRotation), which is
        // declared on Entity and fed by ServerGamePacketListenerImpl#handlePlayerKnownMovement
        // (zeroed by handleClientTickEnd when no movement arrived — never goes stale). Mirrored
        // here from recent displacement (position - lastPos) rather than the friction-scaled
        // deltaMovement. Y is zeroed when grounded.
        val knownMovement = player.position().subtract(player.lastPos)
        val ownVelocity = Vec3(
            knownMovement.x,
            if (player.onGround()) 0.0 else knownMovement.y,
            knownMovement.z,
        )
        // Lag compensation: lead the anchor so the solve runs on the target's live position.
        // No-op in singleplayer, where the integrated server shares the world state.
        val lagTicks = if (lagCompensation && !mc.hasSingleplayerServer()) {
            2.0 + player.ping / 100.0
        } else {
            0.0
        }
        // While gliding, the pose eye height (0.4) sits 0.1 above the center of the 0.6-block
        // hitbox (see Avatar#FALL_FLYING) — aiming at the eye ≈ aiming at center of mass.
        val targetPosition = target.getEyePosition().add(target.deltaMovement.scale(lagTicks))

        return when (aimMode) {
            AimMode.INTERCEPT_POINT -> {
                val solution = InterceptElytraSolver.solveWindChargeIntercept(
                    eye,
                    ownVelocity,
                    targetPosition,
                    target.deltaMovement,
                    maxFlightTicks.toDouble(),
                )

                if (solution != null) {
                    val predictedImpact = InterceptElytraSolver.predictGliderLinear(
                        targetPosition,
                        target.deltaMovement,
                        solution.flightTicks,
                    )
                    Aim(solution.rotation, solution.flightTicks, predictedImpact)
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
                engagedMultiplier.toDouble(),
            )

            PredictionMode.SIMULATED -> PositionExtrapolation.getBestForEntity(target).getPositionInTicks(flightTicks)
        }

        return Aim(Rotation.lookingAt(predicted, eye), flightTicks, predicted)
    }

    /**
     * Validates the shot by simulating the wind charge trajectory and checking it reaches the
     * predicted impact point the aim was computed for. Bounded by the maximum flight time, hence
     * bounded tick count (per-step cost still scales with nearby entities).
     *
     * The simulation world is static — entity hitboxes never move — while the aim leads the target,
     * so the trajectory is compared against the target's predicted position instead of an entity
     * reference.
     */
    private fun passesVerification(aim: Aim): Boolean {
        // Note: the shared trajectory renderer inherits the owner's deltaMovement while the
        // solver above uses the position-based known movement; the residual is absorbed by the
        // tolerance below and the whole check stays opt-in.
        val result = TrajectoryInfoRenderer.getHypotheticalTrajectory(
            player,
            TrajectoryInfo.WIND_CHARGE,
            TrajectoryType.WindCharge,
            aim.rotation,
        ).runSimulation(maxFlightTicks + 1)

        // The simulation advances in ~1.5-block steps per tick plus inherited thrower velocity;
        // the tolerance (~1.06 blocks) covers that granularity plus the 0.3125-block wind charge
        // (see EntityTypes.WIND_CHARGE). This is a coarse geometric filter, not a vanilla hit test:
        // vanilla resolves the charge against the target's box, not a point.
        // Best for short flights: the residual against the solver's known movement grows
        // with flight time.
        return result.positions.any { it.distanceToSqr(aim.predictedImpact) <= VERIFY_TOLERANCE_SQ }
    }

    /**
     * [INTERCEPT_POINT] solves the closed-form interception and falls back to [DIRECT] when no
     * solution exists — e.g. the target outruns the wind charge within [maxFlightTicks].
     */
    private enum class AimMode(override val tag: String) : Tagged {
        INTERCEPT_POINT("InterceptPoint"),
        DIRECT("Direct"),
    }

    private enum class PredictionMode(override val tag: String) : Tagged {
        LINEAR("Linear"),
        SIMULATED("Simulated"),
    }
}
