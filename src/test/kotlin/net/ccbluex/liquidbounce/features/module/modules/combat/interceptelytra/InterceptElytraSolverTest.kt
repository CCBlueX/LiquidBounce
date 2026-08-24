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
import net.ccbluex.liquidbounce.features.module.modules.combat.interceptelytra.InterceptElytraSolver.applyVerticalOffset
import net.ccbluex.liquidbounce.features.module.modules.combat.interceptelytra.InterceptElytraSolver.solveWindChargeIntercept
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.math.tan

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
    fun `NaN input returns null`() {
        assertNull(solve(eye, Vec3(Double.NaN, 1.0, 0.0), Vec3.ZERO))
        assertNull(solve(Vec3(Double.POSITIVE_INFINITY, 0.0, 0.0), Vec3(20.0, 1.0, 0.0), Vec3.ZERO))
    }

    @Test
    fun `positive vertical offset raises the aim above the target`() {
        val solution = solve(eye, Vec3(20.0, 1.0, 0.0), Vec3.ZERO, verticalOffset = 5f)

        assertNotNull(solution)
        assertTrue(solution.rotation.directionVector.y > 0.0)
    }

    @Test
    fun `negative vertical offset lowers the aim below the target`() {
        val solution = solve(eye, Vec3(20.0, 1.0, 0.0), Vec3.ZERO, verticalOffset = -5f)

        assertNotNull(solution)
        assertTrue(solution.rotation.directionVector.y < 0.0)
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

        // Float yaw/pitch round-trip adds ~7e-5 rad of noise; at 30+ blocks that is ~2e-3 blocks,
        // still 500x below the 1.0-block projectile hitbox.
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

    @Test
    fun `vertical offset of zero returns the target unchanged`() {
        val origin = Vec3(0.0, 1.0, 0.0)
        val target = Vec3(20.0, 2.0, 5.0)

        assertEquals(target, applyVerticalOffset(origin, target, 0f))
    }

    @Test
    fun `positive vertical offset raises the aim point keeping distance`() {
        val origin = Vec3(0.0, 1.0, 0.0)
        val target = Vec3(20.0, 1.0, 0.0)

        val raised = applyVerticalOffset(origin, target, 5f)

        assertEquals(20.0 * tan(5.0 * Math.PI / 180.0), raised.y - target.y, 1e-9)
    }

    private fun solve(
        eye: Vec3,
        targetPos: Vec3,
        targetVelocity: Vec3,
        ownVelocity: Vec3 = Vec3.ZERO,
        maxFlightTicks: Double = 60.0,
        verticalOffset: Float = 0f,
    ) = solveWindChargeIntercept(eye, ownVelocity, targetPos, targetVelocity, maxFlightTicks, verticalOffset)

    private fun assertImpactMatchesTarget(
        solution: InterceptElytraSolver.WindChargeSolution,
        targetPos: Vec3,
        targetVelocity: Vec3,
        ownVelocity: Vec3 = Vec3.ZERO,
    ) {
        // Direction is reconstructed from float yaw/pitch, hence the relaxed tolerance: the round-trip
        // error stays below 1e-3 blocks, far smaller than the 1.0-block projectile hitbox.
        val projectileVelocity = solution.rotation.directionVector.scale(WIND_CHARGE_SPEED).add(ownVelocity)
        val impact = eye.add(projectileVelocity.scale(solution.flightTicks))
        val expected = targetPos.add(targetVelocity.scale(solution.flightTicks))

        assertVecEquals(expected, impact, 1e-3)
    }

    private fun assertVecEquals(expected: Vec3, actual: Vec3, delta: Double) {
        assertEquals(expected.x, actual.x, delta)
        assertEquals(expected.y, actual.y, delta)
        assertEquals(expected.z, actual.z, delta)
    }
}
