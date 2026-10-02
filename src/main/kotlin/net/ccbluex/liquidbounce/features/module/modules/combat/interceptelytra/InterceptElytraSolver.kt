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

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Closed-form interception math for wind charges.
 *
 * A wind charge flies in a straight line at constant speed — no gravity, no drag, even in water —
 * and inherits the thrower's velocity at spawn. See
 * [net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge] and
 * [net.minecraft.world.entity.projectile.Projectile.shootFromRotation].
 *
 * Requiring the projectile to hit a linearly moving target at time t gives the condition
 * `|Δ + w·t| = 1.5·t`, which squares to the quadratic
 * `(|w|² − 2.25)·t² + 2·(Δ·w)·t + |Δ|² = 0` with `Δ = target − eye` and
 * `w = targetVelocity − ownVelocity`. All functions here are O(1) closed-form operations.
 */
object InterceptElytraSolver {

    /**
     * Launch speed of a wind charge in blocks per tick.
     *
     * @see net.minecraft.world.item.WindChargeItem#PROJECTILE_SHOOT_POWER
     */
    const val WIND_CHARGE_SPEED: Double = 1.5

    /**
     * Explosion knockback multiplier of the player wind charge.
     *
     * @see net.minecraft.world.level.SimpleExplosionDamageCalculator
     */
    const val KNOCKBACK_MULTIPLIER: Double = 1.22

    /** Explosion radius of the wind charge in blocks. */
    const val EXPLOSION_RADIUS: Double = 1.2

    /** Normalizes the knockback falloff distance (radius * 2). */
    const val KNOCKBACK_DISTANCE_DIVISOR: Double = EXPLOSION_RADIUS * 2.0

    /** Shortest flight time considered solvable, in ticks. */
    const val MIN_FLIGHT_TICKS: Double = 0.5

    /** Maximum deviation of the solved aim direction from unit length. */
    const val MIN_SOLUTION_NORM: Double = 1e-3

    private const val EPSILON = 1e-9
    private const val DEG_TO_RAD = Math.PI / 180.0

    /** Aim direction and flight time of an interception shot. */
    data class WindChargeSolution(
        val rotation: Rotation,
        val flightTicks: Double,
    )

    /**
     * Computes the aim direction that hits a linearly moving target with a wind charge.
     *
     * @param eye spawn position of the projectile (the thrower's eye position).
     * @param ownVelocity the thrower's velocity at spawn; the Y component is ignored when grounded.
     * @param targetPos the target position at time zero.
     * @param targetVelocity the target's linear velocity, in blocks per tick.
     * @param maxFlightTicks upper bound for the predicted flight time; beyond it prediction is unreliable.
     * @param verticalOffsetDegrees shifts the aim point vertically before solving. A positive offset
     * aims above the target, placing the explosion above its eyes so the radial knockback pushes it down.
     * @return the shot solution, or null when no interception exists — e.g. the target escapes faster
     * than the projectile, the target sits at the eye, or any input is non-finite.
     */
    fun solveWindChargeIntercept(
        eye: Vec3,
        ownVelocity: Vec3,
        targetPos: Vec3,
        targetVelocity: Vec3,
        maxFlightTicks: Double,
        verticalOffsetDegrees: Float = 0f,
    ): WindChargeSolution? {
        if (!eye.isFinite() || !ownVelocity.isFinite() || !targetPos.isFinite() || !targetVelocity.isFinite()) {
            return null
        }

        val aimPoint = applyVerticalOffset(eye, targetPos, verticalOffsetDegrees)
        val delta = aimPoint.subtract(eye)
        val constantTerm = delta.lengthSqr()

        // Nothing to aim at from zero distance.
        if (constantTerm < EPSILON) {
            return null
        }

        val relativeVelocity = targetVelocity.subtract(ownVelocity)
        val quadraticCoefficient = relativeVelocity.lengthSqr() - WIND_CHARGE_SPEED * WIND_CHARGE_SPEED
        val linearCoefficient = 2.0 * delta.dot(relativeVelocity)

        val flightTicks = solveFlightTime(quadraticCoefficient, linearCoefficient, constantTerm)
            ?: return null

        // Aim direction, unit by construction since |Δ + w·t| = 1.5·t at the root.
        val direction = delta.add(relativeVelocity.scale(flightTicks)).scale(1.0 / (WIND_CHARGE_SPEED * flightTicks))

        // Numerical safety: discard degenerate solutions and predictions outside the flight window.
        val solvable = flightTicks in MIN_FLIGHT_TICKS..maxFlightTicks &&
            abs(direction.length() - 1.0) <= MIN_SOLUTION_NORM

        return if (solvable) {
            WindChargeSolution(Rotation.fromRotationVec(direction), flightTicks)
        } else {
            null
        }
    }

    /**
     * Smallest positive root of A·t² + B·t + C = 0 for the flight-time problem.
     *
     * When |A| ≈ 0 the target closes at exactly the projectile speed and the equation degenerates
     * to the linear case B·t + C = 0.
     */
    private fun solveFlightTime(a: Double, b: Double, c: Double): Double? {
        if (abs(a) <= EPSILON) {
            if (b >= 0.0) {
                return null // t = −C/B would be non-positive
            }

            return -c / b
        }

        val discriminant = b * b - 4.0 * a * c
        if (discriminant < 0.0) {
            return null
        }

        val root = sqrt(discriminant)
        val first = (-b - root) / (2.0 * a)
        val second = (-b + root) / (2.0 * a)

        return when {
            first > 0.0 && second > 0.0 -> minOf(first, second)
            first > 0.0 -> first
            second > 0.0 -> second
            else -> null
        }
    }

    /**
     * Shifts an aim point vertically by an angle around an origin, keeping the distance constant.
     *
     * Positive offsets raise the point; a wind charge exploding there pushes the target downward.
     */
    fun applyVerticalOffset(origin: Vec3, target: Vec3, offsetDegrees: Float): Vec3 {
        if (offsetDegrees == 0f) {
            return target
        }

        val distance = target.subtract(origin).length()
        return target.add(0.0, distance * tan(offsetDegrees.toDouble() * DEG_TO_RAD), 0.0)
    }

    /**
     * Linear extrapolation of a glider's position.
     *
     * The elytra velocity re-aligns toward the look direction by ~10% per tick, so the linear
     * approximation stays accurate for short flight horizons.
     */
    fun predictGliderLinear(targetPos: Vec3, targetVelocity: Vec3, ticks: Double, multiplier: Double = 1.0): Vec3 =
        targetPos.add(targetVelocity.scale(ticks * multiplier))

    /**
     * Predicts the knockback a wind charge explosion applies, mirroring the server-side formula.
     *
     * @param explosionCenter the center of the wind burst.
     * @param entityEyePosition the affected entity's eye position.
     * @param exposure line-of-sight exposure factor in 0..1 (1.0 in open air).
     * @param knockbackResistance the entity's explosion knockback resistance attribute (0..1).
     * @param knockbackMultiplier the explosion's knockback multiplier; pass 0 for creative flyers.
     * @return the added velocity in blocks per tick, or [Vec3.ZERO] when out of range or degenerate.
     *
     * @see net.minecraft.world.level.ServerExplosion
     */
    fun predictKnockback(
        explosionCenter: Vec3,
        entityEyePosition: Vec3,
        exposure: Double,
        knockbackResistance: Double = 0.0,
        knockbackMultiplier: Double = KNOCKBACK_MULTIPLIER,
    ): Vec3 {
        val offset = entityEyePosition.subtract(explosionCenter)
        val distance = offset.length()

        if (distance < EPSILON) {
            return Vec3.ZERO
        }

        val normalizedDistance = distance / KNOCKBACK_DISTANCE_DIVISOR
        if (normalizedDistance >= 1.0) {
            return Vec3.ZERO
        }

        val power = (1.0 - normalizedDistance) * exposure * knockbackMultiplier * (1.0 - knockbackResistance)
        return offset.scale(power / distance)
    }

    /** Estimated flight time in ticks for a straight-line distance in blocks. */
    fun estimateFlightTicks(distance: Double): Double = distance / WIND_CHARGE_SPEED

    private fun Vec3.isFinite(): Boolean = x.isFinite() && y.isFinite() && z.isFinite()
}
