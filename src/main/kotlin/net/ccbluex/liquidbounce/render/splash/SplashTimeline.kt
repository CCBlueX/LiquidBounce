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
package net.ccbluex.liquidbounce.render.splash

/**
 * Plays the marked segments of a splash animation: the marker `<phase>` once, then `<phase>-loop` for as
 * long as the phase lasts, or else its last frame.
 *
 * A phase without a marker keeps whatever is on screen. Without an `intro` marker the whole animation is
 * the intro.
 */
class SplashTimeline(private val animation: LottieAnimation) {

    enum class Phase(val marker: String) {
        INTRO("intro"),
        CHOICE("choice"),
        TASKS("tasks"),
        OUTRO("outro"),
        ERROR("error"),
    }

    var phase = Phase.INTRO
        private set

    private var frame = 0f
    private var start = 0f
    private var end = 0f
    private var looping = false
    private var pending: Phase? = null
    private var hurried = false

    val isFinished get() = !looping && frame >= end

    /** The frame to draw; a finished segment holds its last frame without dropping layers that end there. */
    val drawFrame get() = if (isFinished) end - END_EPSILON else frame

    init {
        val intro = animation.marker(Phase.INTRO.marker)
        if (intro != null) {
            enter(intro.frame, intro.end, loop = false)
        } else {
            enter(animation.inPoint, animation.outPoint, loop = false)
        }
    }

    /**
     * True once [phase] has played in and loops or holds, or, without a marker for it, once the segment on
     * screen has.
     */
    fun isSettled(phase: Phase) = pending == null && (looping || frame >= end) &&
        (this.phase == phase || animation.marker(phase.marker) == null)

    /**
     * Moves on to [phase] once the current segment is done; a loop is left right away.
     * Phases without a marker are ignored.
     */
    fun request(phase: Phase) {
        if (phase == this.phase || phase == pending || animation.marker(phase.marker) == null) {
            return
        }
        pending = phase
    }

    /** Plays the rest of the current segment faster, so it is over before the splash is. */
    fun hurry() {
        hurried = true
    }

    /**
     * Advances by [millis] of real time and reports every `sound:<name>` marker passed to [sound].
     */
    fun advance(millis: Float, sound: (String) -> Unit) {
        switchIfDue()

        val speed = if (!looping && hurried) HURRY_SPEED else 1f
        var next = frame + millis * animation.frameRate / 1000f * speed

        if (looping && next >= end) {
            fireSounds(frame, end, sound)
            next = start + (next - end) % (end - start)
            fireSounds(start, next, sound)
            frame = next
            return
        }

        next = next.coerceAtMost(end)
        fireSounds(frame, next, sound)
        frame = next
        switchIfDue()
    }

    private fun switchIfDue() {
        val phase = pending
        if (phase != null && (looping || frame >= end)) {
            val marker = animation.marker(phase.marker) ?: return
            pending = null
            hurried = false
            this.phase = phase
            enter(marker.frame, marker.end, loop = false)
            return
        }

        if (!looping && frame >= end) {
            val loop = animation.marker(this.phase.marker + LOOP_SUFFIX)?.takeIf { it.duration > 0f } ?: return
            enter(loop.frame, loop.end, loop = true)
        }
    }

    private fun enter(from: Float, to: Float, loop: Boolean) {
        start = from
        end = to
        frame = from
        looping = loop
    }

    /** Fires the sound markers in `[from, to)`, so a marker on the end of a segment belongs to the next one. */
    private fun fireSounds(from: Float, to: Float, sound: (String) -> Unit) {
        for (marker in animation.markers) {
            if (marker.frame >= from && marker.frame < to && marker.name.startsWith(SOUND_PREFIX)) {
                sound(marker.name.removePrefix(SOUND_PREFIX))
            }
        }
    }

    companion object {
        const val SOUND_PREFIX = "sound:"
        const val LOOP_SUFFIX = "-loop"
        private const val HURRY_SPEED = 3f
        private const val END_EPSILON = 0.001f
    }

}
