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

import kotlin.math.sqrt

/** The action which can realize an already validated crystal opportunity. */
internal enum class SmartTargetAction(val tieBreakPriority: Int) {
    DESTROY(0),
    PLACE(1)
}

/**
 * Pure inputs for smart target ranking. Distances and damage values come from the existing placement and destroy
 * calculations. [targetHealth] includes absorption, so [lethal] represents a one-action kill.
 */
internal data class SmartTargetOpportunity(
    val targetId: Int,
    val enemyDamage: Float,
    val selfDamage: Float,
    val targetHealth: Float,
    val targetDistanceSq: Double,
    val actionDistanceSq: Double,
    val action: SmartTargetAction
) {
    val lethal: Boolean
        get() = enemyDamage >= targetHealth

    val score: Double
        get() = (enemyDamage - selfDamage).toDouble() +
            (if (lethal) SmartTargetDecider.LETHAL_BONUS else 0.0) -
            sqrt(targetDistanceSq.coerceAtLeast(0.0)) * SmartTargetDecider.DISTANCE_WEIGHT
}

internal data class SmartTargetSelection(
    val targetId: Int,
    val pendingTargetId: Int? = null,
    val pendingSince: Long? = null
)

/** Deterministic ranking and switch hysteresis, independent of Minecraft state. */
internal object SmartTargetDecider {

    const val LETHAL_BONUS = 100.0
    const val DISTANCE_WEIGHT = 0.05

    private val comparator = Comparator<SmartTargetOpportunity> { left, right ->
        compareValues(right.score, left.score)
            .ifZero { compareValues(right.enemyDamage, left.enemyDamage) }
            .ifZero { compareValues(left.selfDamage, right.selfDamage) }
            .ifZero { compareValues(left.targetDistanceSq, right.targetDistanceSq) }
            .ifZero { compareValues(left.actionDistanceSq, right.actionDistanceSq) }
            .ifZero { compareValues(left.action.tieBreakPriority, right.action.tieBreakPriority) }
            .ifZero { compareValues(left.targetId, right.targetId) }
    }

    fun bestOpportunity(opportunities: List<SmartTargetOpportunity>): SmartTargetOpportunity? =
        opportunities.minWithOrNull(comparator)

    /**
     * Selects the best actionable target. A valid current target is retained unless a challenger leads by more than
     * [switchAdvantage]. That same challenger must remain eligible for [switchDelayMs] before the switch occurs.
     * If the current target has no actionable opportunity, the best target is selected immediately.
     */
    fun select(
        opportunities: List<SmartTargetOpportunity>,
        currentTargetId: Int?,
        pendingTargetId: Int?,
        pendingSince: Long?,
        now: Long,
        switchAdvantage: Double,
        switchDelayMs: Long
    ): SmartTargetSelection? {
        if (opportunities.isEmpty()) {
            return null
        }

        val ranked = opportunities
            .groupBy(SmartTargetOpportunity::targetId)
            .mapNotNull { (_, targetOpportunities) -> bestOpportunity(targetOpportunities) }
            .sortedWith(comparator)
        val best = ranked.first()
        val current = ranked.firstOrNull { it.targetId == currentTargetId }
            ?: return SmartTargetSelection(best.targetId)

        if (best.targetId == current.targetId) {
            return SmartTargetSelection(current.targetId)
        }

        if (best.score <= current.score + switchAdvantage) {
            return SmartTargetSelection(current.targetId)
        }

        val samePendingTarget = pendingTargetId == best.targetId && pendingSince != null
        if (switchDelayMs <= 0L || samePendingTarget && now - pendingSince!! >= switchDelayMs) {
            return SmartTargetSelection(best.targetId)
        }

        return SmartTargetSelection(
            targetId = current.targetId,
            pendingTargetId = best.targetId,
            pendingSince = if (samePendingTarget) pendingSince else now
        )
    }

    private inline fun Int.ifZero(next: () -> Int): Int = if (this == 0) next() else this

}
