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

import net.ccbluex.liquidbounce.features.module.modules.combat.interceptelytra.InterceptElytraSolver.WIND_CHARGE_SPEED
import net.ccbluex.liquidbounce.features.module.modules.combat.interceptelytra.InterceptElytraSolver.solveWindChargeIntercept
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InterceptElytraSolverTest {

    private val eye = Vec3(0.0, 1.0, 0.0)
    private val ownVelocity = Vec3.ZERO

    @Test
    fun `static target straight ahead solves to straight aim`() {
        val solution = solve(eye, Vec3(20.0, 1.0, 0.0), Vec3.ZERO)

        assertNotNull(solution)
        assertEquals(20.0 / WIND_CHARGE_SPEED, solution.flightTicks, 1e-9)
        assertVecEquals(Vec3(1.0, 0.0, 0.0), solution.rotation.directionVector, 1e-4)
    }

    @Test
    fun `static target above solves correct pitch`() {
        val solution = solve(eye, Vec3(0.0, 11.0, 0.0), Vec3.ZERO)

        assertNotNull(solution)
        assertVecEquals(Vec3(0.0, 1.0, 0.0), solution.rotation.directionVector, 1e-4)
        assertImpactMatchesTarget(solution, Vec3(0.0, 11.0, 0.0), Vec3.ZERO)
    }

    @Test
    fun `moving target head-on produces leading aim`() {
        val targetPos = Vec3(20.0, 1.0, 0.0)
        val targetVelocity = Vec3(-1.0, 0.0, 0.0)
        val solution = solve(eye, targetPos, targetVelocity)

        assertNotNull(solution)
        assertTrue(solution.flightTicks < 20.0 / WIND_CHARGE_SPEED)
        assertImpactMatchesTarget(solution, targetPos, targetVelocity)
    }

    @Test
    fun `target moving sideways faster than projectile returns null`() {
        assertNull(solve(eye, Vec3(20.0, 1.0, 0.0), Vec3(0.0, 2.0, 0.0)))
    }

    @Test
    fun `target closing at exactly projectile speed uses linear branch`() {
        val targetPos = Vec3(20.0, 1.0, 0.0)
        val targetVelocity = Vec3(-WIND_CHARGE_SPEED, 0.0, 0.0)
        val solution = solve(eye, targetPos, targetVelocity)

        assertNotNull(solution)
        assertEquals(20.0 / 3.0, solution.flightTicks, 1e-9)
        assertImpactMatchesTarget(solution, targetPos, targetVelocity)
    }

    @Test
    fun `target at eye position returns null`() {
        assertNull(solve(eye, eye, Vec3.ZERO))
    }

    @Test
    fun `target escaping away faster than projectile returns null`() {
        assertNull(solve(eye, Vec3(20.0, 1.0, 0.0), Vec3(2.0, 0.0, 0.0)))
    }

    @Test
    fun `flight time beyond max returns null`() {
        assertNull(solve(eye, Vec3(200.0, 1.0, 0.0), Vec3.ZERO, maxFlightTicks = 30.0))
    }

    @Test
    fun `fast head-on target returns the earliest root value`() {
        // w = -2 head-on: A = 1.75, B = -80, C = 400 -> roots 5.714 and 40, the first hit wins.
        val targetPos = Vec3(20.0, 1.0, 0.0)
        val targetVelocity = Vec3(-2.0, 0.0, 0.0)
        val solution = solve(eye, targetPos, targetVelocity)

        assertNotNull(solution)
        assertEquals(5.714, solution.flightTicks, 1e-3)
        assertImpactMatchesTarget(solution, targetPos, targetVelocity)
    }

    @Test
    fun `slow sideways target solves with the positive root`() {
        // w = 0.5 lateral: A = -2, B = 0, C = 400 -> only t = 14.142 is positive.
        val targetPos = Vec3(20.0, 1.0, 0.0)
        val targetVelocity = Vec3(0.0, 0.0, 0.5)
        val solution = solve(eye, targetPos, targetVelocity)

        assertNotNull(solution)
        assertEquals(14.142, solution.flightTicks, 1e-3)
        assertImpactMatchesTarget(solution, targetPos, targetVelocity)
    }

    @Test
    fun `flight time below minimum returns null`() {
        // 0.3 blocks away would take 0.2 ticks, below the 0.5 tick minimum.
        assertNull(solve(eye, Vec3(0.3, 1.0, 0.0), Vec3.ZERO))
    }

    @Test
    fun `flight time exactly at minimum returns solution`() {
        // 0.75 blocks away takes exactly 0.5 ticks; the range bound is closed.
        val solution = solve(eye, Vec3(0.75, 1.0, 0.0), Vec3.ZERO)

        assertNotNull(solution)
        assertEquals(0.5, solution.flightTicks, 1e-9)
    }

    @Test
    fun `flight time exactly at maximum returns solution`() {
        // 45 blocks away takes exactly 30 ticks; the range bound is closed.
        val solution = solve(eye, Vec3(45.0, 1.0, 0.0), Vec3.ZERO, maxFlightTicks = 30.0)

        assertNotNull(solution)
        assertEquals(30.0, solution.flightTicks, 1e-9)
    }

    @Test
    fun `own vertical velocity is inherited fully by the solver`() {
        // Ground-zeroing happens in the caller, not here: own = (0, 1, 0) gives
        // w = (0, -1, 0), A = -1.25, B = 0, C = 400 -> t = sqrt(2000) / 2.5.
        val targetPos = Vec3(20.0, 1.0, 0.0)
        val own = Vec3(0.0, 1.0, 0.0)
        val solution = solve(eye, targetPos, Vec3.ZERO, ownVelocity = own)

        assertNotNull(solution)
        assertEquals(17.889, solution.flightTicks, 1e-3)
        assertImpactMatchesTarget(solution, targetPos, Vec3.ZERO, own)
    }

    @Test
    fun `NaN input returns null`() {
        assertNull(solve(eye, Vec3(Double.NaN, 1.0, 0.0), Vec3.ZERO))
        assertNull(solve(Vec3(Double.POSITIVE_INFINITY, 0.0, 0.0), Vec3(20.0, 1.0, 0.0), Vec3.ZERO))
    }

    @Test
    fun `round trip matches continuous vanilla flight`() {
        val targetPos = Vec3(30.0, 3.0, -12.0)
        val targetVelocity = Vec3(-0.7, 0.1, 0.4)
        val own = Vec3(0.3, 0.0, -0.2)
        val solution = assertNotNull(solve(eye, targetPos, targetVelocity, ownVelocity = own))

        // Constant velocity: no gravity, no drag (drag 1.0), exactly like AbstractWindCharge.
        val velocity = solution.rotation.directionVector
            .scale(WIND_CHARGE_SPEED)
            .add(own)

        // Closest approach of the two straight lines, solved in closed form over continuous time.
        // |(eye − P0) + (v0 − vt)·t| is minimized at t = (P0 − eye)·(v0 − vt) / |v0 − vt|².
        val relative = velocity.subtract(targetVelocity)
        val approachTime = targetPos.subtract(eye).dot(relative) / relative.lengthSqr()
        val closestDistance = eye.add(velocity.scale(approachTime))
            .distanceTo(targetPos.add(targetVelocity.scale(approachTime)))

        // Float yaw/pitch quantization is ~1e-7 rad; the dominant noise is vanilla's 64k-entry
        // sin/cos table (steps 2π/65536 ≈ 9.6e-5 rad, ~6.8e-5 rad combined over both axes);
        // at ~33 blocks that is ~2.2e-3 blocks, far below the 0.3125-block wind charge size
        // (see EntityTypes.WIND_CHARGE).
        assertTrue(closestDistance < 1e-2, "closest approach was $closestDistance blocks")
        assertEquals(solution.flightTicks, approachTime, 0.5)
    }

    @Test
    fun `own velocity is inherited by the projectile`() {
        val targetPos = Vec3(20.0, 1.0, 0.0)
        val own = Vec3(1.0, 0.0, 0.0)
        val solution = solve(eye, targetPos, Vec3.ZERO, ownVelocity = own)

        assertNotNull(solution)
        assertImpactMatchesTarget(solution, targetPos, Vec3.ZERO, own)
    }

    private fun solve(
        eye: Vec3,
        targetPos: Vec3,
        targetVelocity: Vec3,
        ownVelocity: Vec3 = Vec3.ZERO,
        maxFlightTicks: Double = 60.0,
    ) = solveWindChargeIntercept(eye, ownVelocity, targetPos, targetVelocity, maxFlightTicks)

    private fun assertImpactMatchesTarget(
        solution: InterceptElytraSolver.WindChargeSolution,
        targetPos: Vec3,
        targetVelocity: Vec3,
        ownVelocity: Vec3 = Vec3.ZERO,
    ) {
        // Direction is reconstructed from float yaw/pitch through vanilla's 64k-entry sin/cos
        // table (~9.6e-5 rad steps), so the round-trip error grows with flight time and can reach
        // ~1.4e-3 blocks at 20 blocks — over 200x smaller than the 0.3125-block wind charge
        // (see EntityTypes.WIND_CHARGE; assert tolerance 5e-3 is 62.5x smaller).
        val projectileVelocity = solution.rotation.directionVector.scale(WIND_CHARGE_SPEED).add(ownVelocity)
        val impact = eye.add(projectileVelocity.scale(solution.flightTicks))
        val expected = targetPos.add(targetVelocity.scale(solution.flightTicks))

        assertVecEquals(expected, impact, 5e-3)
    }

    private fun assertVecEquals(expected: Vec3, actual: Vec3, delta: Double) {
        assertEquals(expected.x, actual.x, delta)
        assertEquals(expected.y, actual.y, delta)
        assertEquals(expected.z, actual.z, delta)
    }
}
