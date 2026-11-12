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
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.math.Easing
import kotlin.math.abs
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
        return aim
    }

    /**
     * Turns to [goal] for as many ticks as [Aim.ticksToReach] predicts and returns the yaw steps, checking that the
     * prediction counts down each tick and the last tick gets there.
     */
    private fun ConstantAim.yawSteps(goal: Rotation = GOAL): List<Float> {
        var view = Rotation(0f, 0f)
        val ticks = ticksToReach(view, goal) + 1
        val steps = (0 until ticks).map { tick ->
            assertEquals(ticks - 1 - tick, ticksToReach(view, goal), "ticksToReach before tick $tick")
            val step = turn(view, AimGoal(goal, 0f, 0f, false), tick == 0)
            view = view.add(step.deltaPitch, step.deltaYaw)
            step.deltaYaw
        }
        assertTrue(view.rotationDeltaLengthTo(goal) < 1e-3f, "ended at $view")
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

}
