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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.targeting

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SmartTargetDeciderTest {

    private fun opportunity(
        targetId: Int = 1,
        enemyDamage: Float = 10f,
        selfDamage: Float = 2f,
        targetHealth: Float = 20f,
        targetDistanceSq: Double = 0.0,
        actionDistanceSq: Double = 0.0,
        action: SmartTargetAction = SmartTargetAction.PLACE,
    ) = SmartTargetOpportunity(
        targetId,
        enemyDamage,
        selfDamage,
        targetHealth,
        targetDistanceSq,
        actionDistanceSq,
        action,
    )

    private fun select(
        opportunities: List<SmartTargetOpportunity>,
        currentTargetId: Int? = null,
        pendingTargetId: Int? = null,
        pendingSince: Long? = null,
        now: Long = 0L,
        switchAdvantage: Double = 2.0,
        switchDelayMs: Long = 100L,
    ) = SmartTargetDecider.select(
        opportunities,
        currentTargetId,
        pendingTargetId,
        pendingSince,
        now,
        switchAdvantage,
        switchDelayMs,
    )

    @Test
    fun `empty actionable input clears the selection`() {
        assertNull(select(emptyList(), currentTargetId = 1, pendingTargetId = 2, pendingSince = 10L))
    }

    @Test
    fun `higher damage efficiency wins without lethal or distance bonuses`() {
        val efficient = opportunity(targetId = 1, enemyDamage = 10f, selfDamage = 1f)
        val inefficient = opportunity(targetId = 2, enemyDamage = 10f, selfDamage = 4f)

        assertEquals(efficient, SmartTargetDecider.bestOpportunity(listOf(inefficient, efficient)))
    }

    @Test
    fun `lethal opportunity outranks greater nonlethal damage`() {
        val lethal = opportunity(targetId = 1, enemyDamage = 12f, targetHealth = 12f)
        val nonlethal = opportunity(targetId = 2, enemyDamage = 30f, targetHealth = 100f)

        assertEquals(lethal, SmartTargetDecider.bestOpportunity(listOf(nonlethal, lethal)))
    }

    @Test
    fun `distance penalty can decide between otherwise equal targets`() {
        val near = opportunity(targetId = 1, targetDistanceSq = 0.0)
        val far = opportunity(targetId = 2, targetDistanceSq = 100.0)

        assertEquals(near, SmartTargetDecider.bestOpportunity(listOf(far, near)))
    }

    @Test
    fun `higher enemy damage breaks an equal score tie`() {
        val higherDamage = opportunity(targetId = 1, enemyDamage = 10f, selfDamage = 2f)
        val lowerDamage = opportunity(targetId = 2, enemyDamage = 9f, selfDamage = 1f)

        assertEquals(higherDamage.score, lowerDamage.score)
        assertEquals(higherDamage, SmartTargetDecider.bestOpportunity(listOf(lowerDamage, higherDamage)))
    }

    @Test
    fun `destroy breaks an otherwise exact action tie`() {
        val place = opportunity(action = SmartTargetAction.PLACE)
        val destroy = opportunity(action = SmartTargetAction.DESTROY)

        assertEquals(destroy, SmartTargetDecider.bestOpportunity(listOf(place, destroy)))
    }

    @Test
    fun `target id makes an exact target tie deterministic`() {
        val first = opportunity(targetId = 1)
        val second = opportunity(targetId = 2)

        assertEquals(first, SmartTargetDecider.bestOpportunity(listOf(second, first)))
        assertEquals(first, SmartTargetDecider.bestOpportunity(listOf(first, second)))
    }

    @Test
    fun `current target is retained at the exact advantage boundary`() {
        val current = opportunity(targetId = 1, enemyDamage = 10f)
        val challenger = opportunity(targetId = 2, enemyDamage = 12f)

        assertEquals(
            SmartTargetSelection(targetId = 1),
            select(listOf(current, challenger), currentTargetId = 1, switchAdvantage = 2.0),
        )
    }

    @Test
    fun `qualified challenger starts a dwell period instead of switching immediately`() {
        val current = opportunity(targetId = 1, enemyDamage = 10f)
        val challenger = opportunity(targetId = 2, enemyDamage = 13f)

        assertEquals(
            SmartTargetSelection(targetId = 1, pendingTargetId = 2, pendingSince = 500L),
            select(listOf(current, challenger), currentTargetId = 1, now = 500L),
        )
    }

    @Test
    fun `same challenger switches only after the full dwell period`() {
        val opportunities = listOf(
            opportunity(targetId = 1, enemyDamage = 10f),
            opportunity(targetId = 2, enemyDamage = 13f),
        )

        assertEquals(
            SmartTargetSelection(targetId = 1, pendingTargetId = 2, pendingSince = 500L),
            select(
                opportunities,
                currentTargetId = 1,
                pendingTargetId = 2,
                pendingSince = 500L,
                now = 599L,
            ),
        )
        assertEquals(
            SmartTargetSelection(targetId = 2),
            select(
                opportunities,
                currentTargetId = 1,
                pendingTargetId = 2,
                pendingSince = 500L,
                now = 600L,
            ),
        )
    }

    @Test
    fun `a different leading challenger resets the dwell timestamp`() {
        val current = opportunity(targetId = 1, enemyDamage = 10f)
        val oldChallenger = opportunity(targetId = 2, enemyDamage = 13f)
        val newChallenger = opportunity(targetId = 3, enemyDamage = 14f)

        assertEquals(
            SmartTargetSelection(targetId = 1, pendingTargetId = 3, pendingSince = 550L),
            select(
                listOf(current, oldChallenger, newChallenger),
                currentTargetId = 1,
                pendingTargetId = 2,
                pendingSince = 500L,
                now = 550L,
            ),
        )
    }

    @Test
    fun `current target regaining the lead clears stale pending state`() {
        val current = opportunity(targetId = 1, enemyDamage = 14f)
        val formerChallenger = opportunity(targetId = 2, enemyDamage = 13f)

        assertEquals(
            SmartTargetSelection(targetId = 1),
            select(
                listOf(current, formerChallenger),
                currentTargetId = 1,
                pendingTargetId = 2,
                pendingSince = 500L,
                now = 900L,
            ),
        )
    }

    @Test
    fun `losing the current actionable target bypasses dwell`() {
        val replacement = opportunity(targetId = 2)

        assertEquals(
            SmartTargetSelection(targetId = 2),
            select(
                listOf(replacement),
                currentTargetId = 1,
                pendingTargetId = 3,
                pendingSince = 500L,
                now = 501L,
            ),
        )
    }

    @Test
    fun `zero dwell allows an immediate qualified switch`() {
        val current = opportunity(targetId = 1, enemyDamage = 10f)
        val challenger = opportunity(targetId = 2, enemyDamage = 13f)

        assertEquals(
            SmartTargetSelection(targetId = 2),
            select(
                listOf(current, challenger),
                currentTargetId = 1,
                switchDelayMs = 0L,
            ),
        )
    }
}
