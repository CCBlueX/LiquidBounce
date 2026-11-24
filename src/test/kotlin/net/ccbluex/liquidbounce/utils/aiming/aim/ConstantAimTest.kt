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
package net.ccbluex.liquidbounce.utils.aiming.aim

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.math.Easing
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConstantAimTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }

        private val GOAL = Rotation(90f, 0f)
    }

    @Suppress("UNCHECKED_CAST")
    private fun aim(
        speed: Float? = null,
        easeIn: Easing = Easing.LINEAR,
        easeOut: Easing = Easing.LINEAR,
        dynamic: Boolean = false,
    ): ConstantAim {
        lateinit var aim: ConstantAim
        ModeValueGroup<Aim>(null, "Aim", { 0 }, { arrayOf(ConstantAim(it).also { constant -> aim = constant }) })
        val values = aim.containedValues.associateBy { it.name }
        if (speed != null) {
            (values.getValue("HorizontalTurnSpeed") as Value<ClosedFloatingPointRange<Float>>).set(speed..speed)
            (values.getValue("VerticalTurnSpeed") as Value<ClosedFloatingPointRange<Float>>).set(speed..speed)
        }
        values.getValue("In").setByString(easeIn.tag)
        values.getValue("Out").setByString(easeOut.tag)
        (values.getValue("Dynamic") as ToggleableValueGroup).enabled = dynamic
        return aim
    }

    /** Ticks [this] takes to turn onto [goal] from straight ahead, and the largest yaw step on the way. */
    private fun ConstantAim.turnOnto(goal: AimGoal): Pair<Int, Float> {
        var view = Rotation(0f, 0f)
        var largest = 0f
        repeat(50) { tick ->
            if (view.rotationDeltaLengthTo(goal.rotation) < 1e-3f) {
                return tick to largest
            }
            val step = turn(view, goal, tick == 0)
            largest = max(largest, abs(step.deltaYaw))
            view = view.add(step.deltaPitch, step.deltaYaw)
        }
        error("never got onto ${goal.rotation}")
    }

    /**
     * Turns to [goal] for as many ticks as [Aim.ticksToReach] predicts and returns the yaw steps, checking that the
     * prediction counts down each tick and the last tick gets there.
     */
    private fun ConstantAim.yawSteps(rotation: Rotation = GOAL): List<Float> {
        var view = Rotation(0f, 0f)
        val goal = AimGoal(rotation, 0f, 0f, false)
        val ticks = ticksToReach(view, goal) + 1
        val steps = (0 until ticks).map { tick ->
            assertEquals(ticks - 1 - tick, ticksToReach(view, goal), "ticksToReach before tick $tick")
            val step = turn(view, goal, tick == 0)
            // A caller that runs after the turn still sees the view from before it, as the server does. A tick that
            // did not move the view cannot be told from the one before it
            if (step.deltaYaw != 0f || step.deltaPitch != 0f) {
                assertEquals(ticks - 1 - tick, ticksToReach(view, goal), "ticksToReach after tick $tick")
            }
            view = view.add(step.deltaPitch, step.deltaYaw)
            step.deltaYaw
        }
        assertTrue(view.rotationDeltaLengthTo(rotation) < 1e-3f, "ended at $view")
        return steps
    }

    @Test
    fun `the defaults turn at once`() {
        assertEquals(listOf(90f), aim().yawSteps())
    }

    @Test
    fun `a linear turn takes even steps at the turn speed`() {
        assertEquals(listOf(30f, 30f, 30f), aim(30f).yawSteps())
    }

    @Test
    fun `an eased in turn starts slow and takes longer`() {
        val steps = aim(30f, easeIn = Easing.QUAD_IN).yawSteps()
        assertTrue(steps.size > 3, "$steps")
        assertTrue(steps[0] < steps[1] && steps[1] < steps[2], "$steps")
        assertTrue(steps.all { abs(it) <= 30f }, "$steps")
    }

    @Test
    fun `ticksToReach agrees with every curve`() {
        val curves = Easing.entries - Easing.NONE
        for (easeIn in curves) {
            for (easeOut in curves) {
                val steps = aim(30f, easeIn, easeOut).yawSteps(Rotation(90f, 40f))
                assertTrue(steps.all { abs(it) <= 30f }, "$easeIn/$easeOut: $steps")
            }
        }
    }

    @Test
    fun `a dynamic turn goes slower the smaller it is`() {
        val aim = aim(30f, dynamic = true)
        assertEquals(3 to 30f, aim.turnOnto(AimGoal(Rotation(90f, 0f), 0f, 0f, false)))
        // Angle 0.4: halfway to 90 degrees the turn keeps 0.7 of its speed, 21 degrees a tick
        val (ticks, largest) = aim.turnOnto(AimGoal(Rotation(45f, 0f), 0f, 0f, false))
        assertEquals(3, ticks)
        assertTrue(largest <= 21f + 1e-3f, "$largest")
        aim.yawSteps(Rotation(45f, 0f))
    }

    @Test
    fun `a dynamic turn slows down for far targets and once on target`() {
        val aim = aim(30f, dynamic = true)
        val goal = Rotation(90f, 0f)
        fun ticks(reached: Boolean, distance: Float) =
            aim.turnOnto(AimGoal(goal, 4f, 4f, reached, AimGoal.Kind.ENTITY, distance)).first
        assertEquals(3, ticks(false, 3f))
        // Distance 0.6: 24 degrees a tick halfway between 3 and 6 blocks, 18 from 6 on
        assertEquals(4, ticks(false, 4.5f))
        assertEquals(5, ticks(false, 9f))
        // OnTarget 0.3: 9 degrees a tick
        assertEquals(10, ticks(true, 3f))
    }

    @Test
    fun `dynamic is off by default`() {
        assertEquals(3 to 30f, aim(30f).turnOnto(AimGoal(Rotation(90f, 0f), 4f, 4f, true, AimGoal.Kind.ENTITY, 9f)))
    }

}
