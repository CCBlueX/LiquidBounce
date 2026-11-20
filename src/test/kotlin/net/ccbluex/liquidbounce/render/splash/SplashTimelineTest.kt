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

import net.ccbluex.liquidbounce.render.splash.SplashTimeline.Phase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * At 1000 frames per second, one millisecond is one frame.
 */
class SplashTimelineTest {

    private fun timeline(vararg markers: Pair<String, IntRange>): SplashTimeline {
        val json = markers.joinToString(",") { (name, range) ->
            """{"cm":"$name","tm":${range.first},"dr":${range.last - range.first}}"""
        }
        return SplashTimeline(
            LottieAnimation.parse("""{"fr":1000,"ip":0,"op":1000,"w":10,"h":10,"layers":[],"markers":[$json]}""")
        )
    }

    private fun SplashTimeline.advance(millis: Float, sounds: MutableList<String> = ArrayList()) =
        sounds.also { advance(millis) { name -> it += name } }

    @Test
    fun `the intro plays once and holds its last frame`() {
        val timeline = timeline("intro" to 0..100)
        timeline.advance(60f)
        assertEquals(60f, timeline.drawFrame, 1e-3f)

        timeline.advance(500f)
        assertTrue(timeline.isFinished)
        assertTrue(timeline.drawFrame < 100f && timeline.drawFrame > 99.9f)
    }

    @Test
    fun `a requested phase waits for the intro to finish`() {
        val timeline = timeline("intro" to 0..100, "tasks" to 200..300)
        timeline.request(Phase.TASKS)

        timeline.advance(50f)
        assertEquals(Phase.INTRO, timeline.phase)

        timeline.advance(60f)
        assertEquals(Phase.TASKS, timeline.phase)
        assertEquals(200f, timeline.drawFrame, 1e-3f)
    }

    @Test
    fun `hurrying plays the rest of the intro faster`() {
        val timeline = timeline("intro" to 0..100)
        timeline.hurry()
        timeline.advance(10f)
        assertEquals(30f, timeline.drawFrame, 1e-3f)
    }

    @Test
    fun `loops repeat until another phase is requested`() {
        val timeline = timeline("intro" to 0..100, "intro-loop" to 100..150, "outro" to 150..200)
        timeline.advance(100f)
        timeline.advance(120f)
        assertEquals(Phase.INTRO, timeline.phase)
        assertEquals(120f, timeline.drawFrame, 1e-3f)

        // A loop is left right away
        timeline.request(Phase.OUTRO)
        timeline.advance(1f)
        assertEquals(Phase.OUTRO, timeline.phase)
    }

    @Test
    fun `sound markers fire once when passed, including on the first frame`() {
        val timeline = timeline("intro" to 0..100, "sound:start" to 0..0, "sound:tick" to 50..50)
        assertEquals(listOf("start"), timeline.advance(10f))
        assertEquals(listOf("tick"), timeline.advance(45f))
        assertEquals(emptyList(), timeline.advance(100f))
    }

    @Test
    fun `sound markers in a loop fire on every lap`() {
        val timeline = timeline("intro" to 0..10, "intro-loop" to 10..20, "sound:beat" to 15..15)
        val sounds = timeline.advance(10f)
        repeat(4) { timeline.advance(10f, sounds) }
        assertEquals(4, sounds.count { it == "beat" })
    }

    @Test
    fun `a sound marker on the end of a loop belongs to the next phase`() {
        val timeline = timeline("intro" to 0..10, "intro-loop" to 10..20, "outro" to 20..30, "sound:out" to 20..20)
        val sounds = timeline.advance(10f)
        repeat(4) { timeline.advance(10f, sounds) }
        assertEquals(emptyList(), sounds)

        timeline.request(Phase.OUTRO)
        repeat(2) { timeline.advance(10f, sounds) }
        assertEquals(listOf("out"), sounds)
    }

    @Test
    fun `phases without a marker keep the current frame`() {
        val timeline = timeline("intro" to 0..100)
        timeline.advance(100f)
        timeline.request(Phase.CHOICE)
        timeline.advance(50f)
        assertEquals(Phase.INTRO, timeline.phase)
    }

    @Test
    fun `without an intro marker the whole animation is the intro`() {
        val timeline = timeline()
        timeline.advance(999f)
        assertTrue(!timeline.isFinished)
        timeline.advance(5f)
        assertTrue(timeline.isFinished)
    }

    @Test
    fun `a phase settles once its segment has played in`() {
        val timeline = timeline("intro" to 0..100, "error" to 100..150, "error-loop" to 150..200)
        timeline.request(Phase.ERROR)
        timeline.advance(100f)
        timeline.advance(30f)
        assertFalse(timeline.isSettled(Phase.ERROR))

        timeline.advance(30f)
        assertTrue(timeline.isSettled(Phase.ERROR))
    }

    @Test
    fun `a phase without a marker settles with the segment on screen`() {
        val timeline = timeline("intro" to 0..100)
        timeline.advance(50f)
        assertFalse(timeline.isSettled(Phase.CHOICE))

        timeline.advance(60f)
        assertTrue(timeline.isSettled(Phase.CHOICE))
    }

}
