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

/**
 * Closed-form interception math for wind charges.
 *
 * A wind charge flies in a straight line at constant speed — `accelerationPower` is 0.0 and
 * `getInertia()`/`getLiquidInertia()` are 1.0, so `applyInertia()` is the identity on land and in
 * water, and `tick()` applies no gravity — and adds the thrower's `getKnownMovement()` at spawn,
 * with the Y component forced to 0 when `onGround()` (`Projectile.shootFromRotation`).
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
     * Hard accuracy ceiling: the server adds `random.triangle(0, 0.0172275 * uncertainty)`
     * per axis to the normalized aim direction (`Projectile.getMovementToShoot`) with uncertainty
     * 1.0 for hand-thrown charges (`WindChargeItem.use`; thrown power equals
     * `PROJECTILE_SHOOT_POWER` = 1.5). Worst case ~1.7° combined (~0.6 blocks at 20 blocks);
     * transverse RMS ~0.57° (~0.20 blocks at 20, from Var = spread²/6 per axis — see
     * `RandomSource#triangle`). No solver precision can beat that spread.
     *
     * @see net.minecraft.world.item.WindChargeItem#PROJECTILE_SHOOT_POWER
     */
    const val WIND_CHARGE_SPEED: Double = 1.5

    /** Shortest flight time considered solvable, in ticks. */
    const val MIN_FLIGHT_TICKS: Double = 0.5

    /** Maximum deviation of the solved aim direction from unit length. */
    const val MIN_SOLUTION_NORM: Double = 1e-3

    private const val EPSILON = 1e-9

    /** Aim direction and flight time of an interception shot. */
    data class WindChargeSolution(
        val rotation: Rotation,
        val flightTicks: Double,
    )

    /**
     * Computes the aim direction that hits a linearly moving target with a wind charge.
     *
     * @param eye spawn position of the projectile (the thrower's eye position).
     * @param ownVelocity the thrower's velocity at spawn, inherited in full by the projectile
     * (the caller must zero the Y component when grounded, mirroring `Projectile.shootFromRotation`).
     * @param targetPos the target position at time zero.
     * @param targetVelocity the target's linear velocity, in blocks per tick.
     * @param maxFlightTicks upper bound for the predicted flight time; beyond it prediction is unreliable.
     * @return the shot solution, or null when no interception exists within the window — e.g. the
     * target outruns the projectile (no positive root; a firework-boosted glider converges to
     * ~1.7 b/t against the charge's 1.5 b/t, see FireworkRocketEntity), the target is at
     * (within ~3e-5 blocks of) the eye, the root falls outside `MIN_FLIGHT_TICKS..maxFlightTicks`,
     * the degenerate linear case does not close (`B >= 0`), or any of the four vector inputs is
     * non-finite.
     */
    fun solveWindChargeIntercept(
        eye: Vec3,
        ownVelocity: Vec3,
        targetPos: Vec3,
        targetVelocity: Vec3,
        maxFlightTicks: Double,
    ): WindChargeSolution? {
        if (!eye.isFinite() || !ownVelocity.isFinite() || !targetPos.isFinite() || !targetVelocity.isFinite()) {
            return null
        }

        val delta = targetPos.subtract(eye)
        val constantTerm = delta.lengthSqr()

        // Treat near-zero distance (below ~3e-5 blocks) as no solution.
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

        // Validity window: discard sub-minimum times, predictions past maxFlightTicks,
        // and non-unit directions.
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
     * When |A| is near zero the relative speed equals the projectile speed and the equation
     * degenerates to the linear case B·t + C = 0, solvable iff the target is closing (B < 0,
     * since C = |Δ|² > 0).
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
     * Linear extrapolation of a glider's position.
     *
     * This is an approximation: gliders can steer away from their current velocity (vanilla blends
     * horizontal velocity toward the look direction by 0.1 per tick — see
     * LivingEntity#updateFallFlyingMovement), so the linear model only holds for short horizons.
     */
    fun predictGliderLinear(targetPos: Vec3, targetVelocity: Vec3, ticks: Double, multiplier: Double = 1.0): Vec3 =
        targetPos.add(targetVelocity.scale(ticks * multiplier))

    /** Estimated flight time in ticks for a straight-line distance in blocks. */
    fun estimateFlightTicks(distance: Double): Double = distance / WIND_CHARGE_SPEED

    private fun Vec3.isFinite(): Boolean = x.isFinite() && y.isFinite() && z.isFinite()
}
