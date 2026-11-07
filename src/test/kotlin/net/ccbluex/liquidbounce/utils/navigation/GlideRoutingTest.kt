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

package net.ccbluex.liquidbounce.utils.navigation

import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.navigation.TestNavigationWorld.Companion.feet
import net.ccbluex.liquidbounce.utils.navigation.flight.GlideAbilities
import net.ccbluex.liquidbounce.utils.navigation.flight.GlideControl
import net.ccbluex.liquidbounce.utils.navigation.flight.GlidePhysics
import net.ccbluex.liquidbounce.utils.navigation.flight.GlidePath
import net.ccbluex.liquidbounce.utils.navigation.flight.GlideRouteSearch
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GlideRoutingTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }

        private val GLIDER = GlideAbilities(rockets = 0, rocketTicks = 0, glideTicks = Int.MAX_VALUE)
        private val ROCKETEER = GlideAbilities(rockets = 8, rocketTicks = 20, glideTicks = Int.MAX_VALUE)
    }

    /**
     * Ground at y = 0 from x -10 to 100, so feet stand at y = 1, and a stone tower on x -1..1, z -1..1 with its top
     * at [towerTop].
     */
    private fun towerWorld(towerTop: Int) = TestNavigationWorld()
        .fill(-10, 0, -40, 100, 0, 40)
        .fill(-1, 1, -1, 1, towerTop, 1)

    private fun glide(world: TestNavigationWorld, start: Vec3, goal: Vec3, abilities: GlideAbilities):
        RouteResult<GlidePath> {
        val search = GlideRouteSearch(world.terrain(), start, goal, 1.0, abilities)
        var result = search.advance()
        repeat(1000) {
            if (result != RouteResult.Searching) {
                return result
            }
            result = search.advance()
        }
        return result
    }

    @Test
    fun `goals far below and ahead are glided to`() {
        val goal = feet(70, 1, 0)
        val result = glide(towerWorld(40), feet(0, 41, 0), goal, GLIDER)

        val path = assertIs<RouteResult.Found<GlidePath>>(result).value
        assertEquals(1.0, path.landing.y, 1.0E-6, "the glide lands at ${path.landing}")
        assertTrue(path.landing.distanceTo(goal) <= 3.0, "the glide lands at ${path.landing}, away from $goal")
        assertEquals(0, path.rockets)
    }

    @Test
    fun `walls in the way stop the glide`() {
        val world = towerWorld(40).fill(30, 1, -40, 30, 45, 40)

        assertIs<RouteResult.Failed>(glide(world, feet(0, 41, 0), feet(70, 1, 0), GLIDER))
    }

    @Test
    fun `goals above are glided to only with rockets`() {
        val world = towerWorld(10).fill(56, 1, -8, 72, 19, 8)
        val start = feet(0, 11, 0)
        val goal = feet(64, 20, 0)

        assertIs<RouteResult.Failed>(glide(world, start, goal, GLIDER))

        val path = assertIs<RouteResult.Found<GlidePath>>(glide(world, start, goal, ROCKETEER)).value
        assertTrue(path.rockets > 0, "the glide needs no rocket")
        assertTrue(path.landing.distanceTo(goal) <= 3.0, "the glide lands at ${path.landing}, away from $goal")
    }

    @Test
    fun `far too high above the goal it sinks without firing rockets`() {
        val goal = Vec3(0.5, 90.0, 0.5)
        val control = GlideControl(goal, rockets = 8, rocketTicks = 20)
        var position = Vec3(20.5, 250.0, 0.5)
        var velocity = Vec3(-1.5, 0.0, 0.0)

        repeat(200) {
            val input = control.steer(position, velocity)
            assertFalse(input.fireRocket, "fired a rocket at $position, ${position.y - goal.y} above the goal")
            velocity = GlidePhysics.step(velocity, input.yaw, input.pitch, input.boosted)
            position = position.add(velocity)
        }
        assertEquals(8, control.rockets)
        assertTrue(position.y < 250.0 - 40.0, "only sank to ${position.y}")
    }

}
