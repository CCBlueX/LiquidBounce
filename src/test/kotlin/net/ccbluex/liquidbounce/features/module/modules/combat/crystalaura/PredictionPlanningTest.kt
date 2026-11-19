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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import net.minecraft.core.BlockPos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class PredictionPlanningTest {

    private val placePos = BlockPos(4, 64, -2)

    private fun pending(
        generation: Long = 7L,
        placePos: BlockPos = this.placePos,
        highestId: Int = 100,
        createdAtMs: Long = 1_000L,
        dispatchedIds: List<Int> = emptyList(),
        dispatchedAtMs: Long? = null,
    ) = PendingPrediction(generation, placePos, highestId, createdAtMs, dispatchedIds, dispatchedAtMs)

    private fun plan(
        pending: PendingPrediction? = pending(),
        generation: Long = 7L,
        placePos: BlockPos = this.placePos,
        offsetRange: IntRange = 1..2,
        nowMs: Long = 1_000L,
        maxAgeMs: Long = 1_000L,
        existingEntities: Map<Int, PredictedEntityKind> = emptyMap(),
    ) = planPrediction(pending, generation, placePos, offsetRange, nowMs, maxAgeMs, existingEntities)

    private fun assertRejected(
        transition: PredictionTransition,
        reason: PredictionRejectReason,
        entityId: Int? = null,
    ) {
        val rejection = assertIs<PredictionOutcome.Reject>(transition.outcome)
        assertEquals(reason, rejection.reason)
        assertEquals(entityId, rejection.entityId)
    }

    @Test
    fun `missing pending action is stale`() {
        val transition = plan(pending = null)

        assertRejected(transition, PredictionRejectReason.STALE_ACTION)
        assertNull(transition.pending)
    }

    @Test
    fun `stale action cannot clear a newer pending prediction`() {
        val newer = pending(generation = 8L, placePos = BlockPos(9, 70, 3), createdAtMs = 900L)

        val transition = plan(
            pending = newer,
            generation = 7L,
            placePos = BlockPos(1, 1, 1),
            nowMs = 10_000L,
        )

        assertRejected(transition, PredictionRejectReason.STALE_ACTION)
        assertSame(newer, transition.pending)
    }

    @Test
    fun `position mismatch invalidates the pending action`() {
        val transition = plan(placePos = BlockPos(5, 64, -2))

        assertRejected(transition, PredictionRejectReason.POSITION_MISMATCH)
        assertNull(transition.pending)
    }

    @Test
    fun `fresh prediction dispatches every offset in order`() {
        val original = pending(highestId = 100, createdAtMs = 1_000L)

        val transition = plan(pending = original, offsetRange = 2..4, nowMs = 1_100L)

        assertEquals(PredictionOutcome.Dispatch(listOf(102, 103, 104)), transition.outcome)
        assertEquals(
            original.copy(dispatchedIds = listOf(102, 103, 104), dispatchedAtMs = 1_100L),
            transition.pending,
        )
    }

    @Test
    fun `prediction at the exact age boundary remains valid`() {
        val transition = plan(nowMs = 2_000L, maxAgeMs = 1_000L)

        assertEquals(PredictionOutcome.Dispatch(listOf(101, 102)), transition.outcome)
    }

    @Test
    fun `expired and negatively aged predictions are rejected`() {
        val expired = plan(nowMs = 2_001L, maxAgeMs = 1_000L)
        val negativeAge = plan(nowMs = 999L, maxAgeMs = 1_000L)

        assertRejected(expired, PredictionRejectReason.EXPIRED)
        assertNull(expired.pending)
        assertRejected(negativeAge, PredictionRejectReason.EXPIRED)
        assertNull(negativeAge.pending)
    }

    @Test
    fun `duplicate invocation cannot dispatch the same prediction twice`() {
        val dispatched = pending(dispatchedIds = listOf(101, 102), dispatchedAtMs = 1_000L)

        val transition = plan(pending = dispatched)

        assertRejected(transition, PredictionRejectReason.ALREADY_DISPATCHED)
        assertSame(dispatched, transition.pending)
    }

    @Test
    fun `empty offset range has no dispatch`() {
        val transition = plan(offsetRange = IntRange.EMPTY)

        assertRejected(transition, PredictionRejectReason.EMPTY_RANGE)
        assertNull(transition.pending)
    }

    @Test
    fun `predicted entity id must be positive and fit an int`() {
        val zero = plan(pending = pending(highestId = 0), offsetRange = 0..0)
        val overflow = plan(pending = pending(highestId = Int.MAX_VALUE), offsetRange = 1..1)

        assertRejected(zero, PredictionRejectReason.ID_OVERFLOW)
        assertNull(zero.pending)
        assertRejected(overflow, PredictionRejectReason.ID_OVERFLOW)
        assertNull(overflow.pending)
    }

    @Test
    fun `unexpected existing entity invalidates the complete offset sequence`() {
        val transition = plan(
            offsetRange = 1..3,
            existingEntities = mapOf(102 to PredictedEntityKind.OTHER),
        )

        assertRejected(transition, PredictionRejectReason.UNEXPECTED_ENTITY, entityId = 102)
        assertNull(transition.pending)
    }

    @Test
    fun `existing crystal uses the normal attack path instead of prediction`() {
        val transition = plan(
            offsetRange = 1..3,
            existingEntities = mapOf(103 to PredictedEntityKind.END_CRYSTAL),
        )

        assertEquals(PredictionOutcome.AttackExisting(103), transition.outcome)
        assertNull(transition.pending)
    }

    @Test
    fun `unrelated existing entities do not affect prediction`() {
        val transition = plan(
            existingEntities = mapOf(
                100 to PredictedEntityKind.OTHER,
                104 to PredictedEntityKind.END_CRYSTAL,
            ),
        )

        assertEquals(PredictionOutcome.Dispatch(listOf(101, 102)), transition.outcome)
    }
}
