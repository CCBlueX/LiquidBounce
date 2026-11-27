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

import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationDelta
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.util.Mth
import java.util.EnumSet
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Turns straight at the goal, instantly by default. Each turn follows the In curve for its first half and the Out
 * curve for its second, as fast as the turn speeds allow, and Dynamic slows it down by the size of the turn, the
 * distance to the target and whether the view meets it already.
 */
class ConstantAim(parent: ModeValueGroup<*>) : Aim("Constant", parent, listOf("Linear")) {

    private val horizontalTurnSpeed by floatRange("HorizontalTurnSpeed", 180f..180f, 0f..180f, "°/tick")
    private val verticalTurnSpeed by floatRange("VerticalTurnSpeed", 180f..180f, 0f..180f, "°/tick")
    private val easeIn by enumChoice("In", Easing.LINEAR, CURVES)
    private val easeOut by enumChoice("Out", Easing.LINEAR, CURVES)
    private val dynamic = tree(Dynamic())

    private var turn: Turn? = null

    /**
     * Slows each turn down by what it is like. Every setting is the share of the turn speeds left at its far end.
     */
    private inner class Dynamic : ToggleableValueGroup(this, "Dynamic", false) {
        /** Left for the smallest turns; from 90 degrees on a turn goes at full speed. */
        private val smallTurns by float("Angle", 0.4f, 0.05f..1f, "x")

        /** Left for a target 6 or more blocks away; up to 3 blocks it goes at full speed. */
        private val farTargets by float("Distance", 0.6f, 0.05f..1f, "x")

        /** Left while the view already meets the target. */
        private val onTarget by float("OnTarget", 0.3f, 0.05f..1f, "x")

        /** The share of the turn speeds a turn of [degrees] onto [goal] may use. */
        fun share(degrees: Float, goal: AimGoal): Float {
            if (!enabled) {
                return 1f
            }

            val share = Mth.lerp(min(degrees / 90f, 1f), smallTurns, 1f) *
                Mth.lerp(((goal.distance - 3f) / 3f).coerceIn(0f, 1f), 1f, farTargets)
            return if (goal.isReached) share * onTarget else share
        }
    }

    /**
     * One movement from [start] to the goal in [ticks] ticks, with at most [yawSpeed] and [pitchSpeed] degrees a tick.
     * Its [pace] is the average step; [isReached] whether the view met the target when it was planned.
     */
    private inner class Turn(
        val start: Rotation,
        val ticks: Int,
        val yawSpeed: Float,
        val pitchSpeed: Float,
        val pace: Float,
        val isReached: Boolean,
    ) {
        var tick = 0
        var goal = start

        /** Where the last tick of this turn found the view, and where it sent it. */
        var from = start
        var view = start
        var step = 0f

        /** Whether the next tick from [view] towards [goal] still belongs to this turn rather than starting one. */
        fun continues(view: Rotation, goal: Rotation) = tick < ticks && follows(goal) &&
            // The view lands up to half a mouse step off where it was sent
            (view == this.view || isAt(view, this.view, RotationUtil.gcd.toFloat()))

        /** Whether [view] is where the last tick started, so that tick's step towards [goal] is still to arrive. */
        fun isUnderWay(view: Rotation, goal: Rotation) = tick > 0 && follows(goal) && view != this.view &&
            isAt(view, from, 1e-3f)

        // A goal that moved further than the view moves in a tick jumped, so the turn starts over
        private fun follows(goal: Rotation) = this.goal.rotationDeltaLengthTo(goal) <= max(step, pace)

        private fun isAt(view: Rotation, other: Rotation, tolerance: Float) =
            abs(angleDifference(view.yaw, other.yaw)) <= tolerance && abs(view.pitch - other.pitch) <= tolerance

        /** The step from [view] to where the [nth] tick of this turn puts it, before the speeds limit it. */
        fun toward(view: Rotation, goal: Rotation, nth: Int): RotationDelta {
            val progress = if (nth >= ticks) 1f else progress(nth.toFloat() / ticks)
            val path = start.rotationDeltaTo(goal)
            return view.rotationDeltaTo(start.add(path.deltaPitch * progress, path.deltaYaw * progress))
        }

        fun limit(step: RotationDelta) = RotationDelta(
            step.deltaYaw.coerceIn(-yawSpeed, yawSpeed),
            step.deltaPitch.coerceIn(-pitchSpeed, pitchSpeed),
        )

        /**
         * Ticks after the next one until [view] is on a [goal] that stays: the rest of this turn, and of another one
         * where the speeds cut its last step short.
         */
        fun ticksLeft(view: Rotation, goal: Rotation): Int {
            var at = view
            var cut = RotationDelta(0f, 0f)
            for (next in tick + 1..ticks) {
                val wanted = toward(at, goal, next)
                val step = limit(wanted)
                at = at.add(step.deltaPitch, step.deltaYaw)
                cut = RotationDelta(wanted.deltaYaw - step.deltaYaw, wanted.deltaPitch - step.deltaPitch)
            }

            // The last tick lands on the goal unless the speeds cut it short, as they do when the goal moved
            val rest = if (cut.deltaYaw == 0f && cut.deltaPitch == 0f) {
                0
            } else {
                ticks(abs(cut.deltaYaw), abs(cut.deltaPitch), yawSpeed, pitchSpeed)
            }
            return ticks - tick - 1 + rest
        }
    }

    override fun turn(view: Rotation, goal: AimGoal, isFresh: Boolean): RotationDelta {
        val target = goal.rotation
        // Reaching the target changes how fast a dynamic turn may go, so it starts over from there
        val turn = this.turn?.takeIf {
            !isFresh && it.continues(view, target) && (!dynamic.enabled || it.isReached == goal.isReached)
        } ?: plan(view, goal).also { this.turn = it }

        turn.tick++
        val delta = turn.limit(turn.toward(view, target, turn.tick))
        turn.goal = target
        turn.from = view
        turn.view = view.add(delta.deltaPitch, delta.deltaYaw)
        turn.step = delta.length()
        return delta
    }

    override fun ticksToReach(from: Rotation, goal: AimGoal): Int {
        val to = goal.rotation
        val delta = from.rotationDeltaTo(to)
        val yaw = abs(delta.deltaYaw)
        val pitch = abs(delta.deltaPitch)
        val share = dynamic.share(delta.length(), goal)
        val yawSpeed = horizontalTurnSpeed.start * share
        val pitchSpeed = verticalTurnSpeed.start * share
        if (yaw > 0f && yawSpeed == 0f || pitch > 0f && pitchSpeed == 0f) {
            // Never, as a count callers can still add to
            return Int.MAX_VALUE / 2
        }

        val turn = turn
        // Asked after this tick's turn, with the view the server still has from before it
        if (turn != null && turn.isUnderWay(from, to)) {
            return turn.ticksLeft(turn.view, to) + 1
        }
        if (turn != null && turn.continues(from, to)) {
            return turn.ticksLeft(from, to)
        }

        return ticks(yaw, pitch, yawSpeed, pitchSpeed) - 1
    }

    private fun plan(view: Rotation, goal: AimGoal): Turn {
        val delta = view.rotationDeltaTo(goal.rotation)
        val share = dynamic.share(delta.length(), goal)
        val yawSpeed = horizontalTurnSpeed.random() * share
        val pitchSpeed = verticalTurnSpeed.random() * share
        val ticks = ticks(abs(delta.deltaYaw), abs(delta.deltaPitch), yawSpeed, pitchSpeed)
        return Turn(view, ticks, yawSpeed, pitchSpeed, delta.length() / ticks, goal.isReached)
    }

    /**
     * The fewest ticks in which the curve turns [yawAngle] and [pitchAngle] degrees without a step above the speeds.
     * An axis that cannot turn does not count.
     */
    private fun ticks(yawAngle: Float, pitchAngle: Float, yawSpeed: Float, pitchSpeed: Float): Int {
        val yaw = if (yawSpeed > 0f) yawAngle else 0f
        val pitch = if (pitchSpeed > 0f) pitchAngle else 0f

        // Lets float noise in a step pass as the turn speed itself
        val slack = 1.0001f
        // The largest step is at least the average one, so no curve gets there in fewer ticks than a linear turn
        val linear = max(
            if (yaw > 0f) yaw / yawSpeed else 0f,
            if (pitch > 0f) pitch / pitchSpeed else 0f,
        )
        return fewest(max(1, ceil(linear / slack).toInt())) { ticks ->
            val share = largestShare(ticks) / slack
            share * yaw <= yawSpeed && share * pitch <= pitchSpeed
        }
    }

    /** The largest part of the turn one tick takes when the turn lasts [ticks] ticks. */
    private fun largestShare(ticks: Int): Float {
        var largest = 0f
        var before = 0f
        for (tick in 1..ticks) {
            val now = if (tick == ticks) 1f else progress(tick.toFloat() / ticks)
            largest = max(largest, abs(now - before))
            before = now
        }
        return largest
    }

    /** The smallest count from [least] on that [fits], taking a count that fits to mean that all above it do. */
    private inline fun fewest(least: Int, fits: (Int) -> Boolean): Int {
        if (fits(least)) {
            return least
        }

        var low = least
        var high = least * 2
        while (!fits(high)) {
            low = high
            high *= 2
        }
        while (high - low > 1) {
            val middle = (low + high) / 2
            if (fits(middle)) high = middle else low = middle
        }
        return high
    }

    private fun progress(time: Float) = if (time < 0.5f) {
        easeIn.transform(time * 2) / 2
    } else {
        0.5f + easeOut.transform(time * 2 - 1) / 2
    }

    private companion object {
        /** [Easing.NONE] would skip a half of the turn. */
        val CURVES: Set<Easing> = EnumSet.complementOf(EnumSet.of(Easing.NONE))
    }

}
