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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlacementCandidateDeciderTest {

    private fun choice(
        source: String,
        enemyDamage: Float = 10f,
        selfDamage: Float = 1f,
        distanceSq: Double = 1.0,
        requiresBasePlace: Boolean = false,
        notBlockedByCrystal: Boolean = true,
        hasFoundation: Boolean = true,
    ) = PlacementCandidateChoice(
        source,
        PlacementCandidateFeatures(
            enemyDamage,
            selfDamage,
            distanceSq,
            requiresBasePlace,
            notBlockedByCrystal,
            hasFoundation,
        ),
    )

    private fun select(
        candidates: List<PlacementCandidateChoice<String>>,
        currentBasePlaceTarget: PlacementCandidateChoice<String>? = null,
        minAdvantage: Float = 2f,
    ) = PlacementCandidateDecider.select(candidates, currentBasePlaceTarget, minAdvantage)

    @Test
    fun `enemy damage is the primary ranking`() {
        val lower = choice("lower", enemyDamage = 8f)
        val higher = choice("higher", enemyDamage = 12f)

        assertSame(higher, select(listOf(lower, higher)))
    }

    @Test
    fun `lower self damage and then nearer distance break ranking ties`() {
        val safer = choice("safer", selfDamage = 1f)
        val dangerous = choice("dangerous", selfDamage = 4f)
        assertSame(safer, select(listOf(dangerous, safer)))

        val near = choice("near", distanceSq = 1.0)
        val far = choice("far", distanceSq = 9.0)
        assertSame(near, select(listOf(far, near)))
    }

    @Test
    fun `immediate actionability excludes blocked and base-place candidates`() {
        assertTrue(choice("immediate").features.isImmediatelyActionable)
        assertFalse(choice("blocked", notBlockedByCrystal = false).features.isImmediatelyActionable)
        assertFalse(choice("base", requiresBasePlace = true).features.isImmediatelyActionable)
    }

    @Test
    fun `crystal-blocked winner falls back to an immediately actionable candidate`() {
        val blocked = choice("blocked", enemyDamage = 100f, notBlockedByCrystal = false)
        val immediate = choice("immediate", enemyDamage = 1f)

        assertSame(immediate, select(listOf(blocked, immediate), minAdvantage = 0.1f))
    }

    @Test
    fun `fallback chooses the best of multiple actionable candidates`() {
        val blocked = choice("blocked", enemyDamage = 30f, notBlockedByCrystal = false)
        val weaker = choice("weaker", enemyDamage = 8f)
        val stronger = choice("stronger", enemyDamage = 12f)

        assertSame(stronger, select(listOf(blocked, weaker, stronger)))
    }

    @Test
    fun `base-place advantage is compared with the actionable fallback`() {
        val base = choice("base", enemyDamage = 12f, requiresBasePlace = true)
        val blocked = choice("blocked", enemyDamage = 11f, notBlockedByCrystal = false)
        val immediate = choice("immediate", enemyDamage = 10f)

        assertSame(immediate, select(listOf(base, blocked, immediate), minAdvantage = 2f))
    }

    @Test
    fun `base place remains when its advantage exceeds the fallback margin`() {
        val base = choice("base", enemyDamage = 13f, requiresBasePlace = true)
        val immediate = choice("immediate", enemyDamage = 10f)

        assertSame(base, select(listOf(base, immediate), minAdvantage = 2f))
    }

    @Test
    fun `base-place candidates without a foundation cannot win`() {
        val unsupportedBest = choice(
            "unsupported-best",
            enemyDamage = 20f,
            requiresBasePlace = true,
            hasFoundation = false,
        )
        val supported = choice("supported", enemyDamage = 15f, requiresBasePlace = true)

        assertSame(supported, select(listOf(unsupportedBest, supported)))
        assertNull(
            select(
                listOf(
                    unsupportedBest,
                    choice("unsupported-lower", requiresBasePlace = true, hasFoundation = false),
                ),
            ),
        )
    }

    @Test
    fun `valid current base place is retained at the exact advantage boundary`() {
        val challenger = choice("challenger", enemyDamage = 12f)
        val current = choice("current", enemyDamage = 10f, requiresBasePlace = true)

        assertSame(
            current,
            select(listOf(challenger), currentBasePlaceTarget = current, minAdvantage = 2f),
        )
    }

    @Test
    fun `current base place is replaced when the challenger exceeds the margin`() {
        val challenger = choice("challenger", enemyDamage = 13f)
        val current = choice("current", enemyDamage = 10f, requiresBasePlace = true)

        assertSame(
            challenger,
            select(listOf(challenger), currentBasePlaceTarget = current, minAdvantage = 2f),
        )
    }

    @Test
    fun `current base place without a foundation is not retained`() {
        val challenger = choice("challenger", enemyDamage = 11f)
        val current = choice(
            "current",
            enemyDamage = 10f,
            requiresBasePlace = true,
            hasFoundation = false,
        )

        assertSame(
            challenger,
            select(listOf(challenger), currentBasePlaceTarget = current, minAdvantage = 2f),
        )
    }

    @Test
    fun `current base-place retention follows actionable fallback`() {
        val blocked = choice("blocked", enemyDamage = 20f, notBlockedByCrystal = false)
        val immediate = choice("immediate", enemyDamage = 11f)
        val current = choice("current", enemyDamage = 10f, requiresBasePlace = true)

        assertSame(
            current,
            select(listOf(blocked, immediate), currentBasePlaceTarget = current, minAdvantage = 2f),
        )
    }

    @Test
    fun `empty candidate input has no selection`() {
        assertNull(select(emptyList()))
    }
}
