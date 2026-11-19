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

/** Pure placement inputs. Every entry represents damage-valid position data. */
internal data class PlacementCandidateFeatures(
    val enemyDamage: Float,
    val selfDamage: Float,
    val distanceSq: Double,
    val requiresBasePlace: Boolean,
    val notBlockedByCrystal: Boolean,
    val hasFoundation: Boolean
) : Comparable<PlacementCandidateFeatures> {

    val isImmediatelyActionable: Boolean
        get() = notBlockedByCrystal && !requiresBasePlace

    override fun compareTo(other: PlacementCandidateFeatures): Int {
        val enemyDamageComparison = enemyDamage.compareTo(other.enemyDamage)
        if (enemyDamageComparison != 0) {
            return enemyDamageComparison
        }

        val selfDamageComparison = other.selfDamage.compareTo(selfDamage)
        if (selfDamageComparison != 0) {
            return selfDamageComparison
        }

        return other.distanceSq.compareTo(distanceSq)
    }

}

internal data class PlacementCandidateChoice<T>(
    val source: T,
    val features: PlacementCandidateFeatures
) : Comparable<PlacementCandidateChoice<T>> {
    override fun compareTo(other: PlacementCandidateChoice<T>) = features.compareTo(other.features)
}

/** Final ordered placement choice with actionable fallback and BasePlace hysteresis. */
internal object PlacementCandidateDecider {

    fun <T> select(
        candidates: List<PlacementCandidateChoice<T>>,
        currentBasePlaceTarget: PlacementCandidateChoice<T>?,
        minAdvantage: Float
    ): PlacementCandidateChoice<T>? {
        var bestTarget = candidates.maxWithFoundation() ?: return null
        val bestImmediateTarget = candidates.asSequence()
            .filter { it.features.isImmediatelyActionable }
            .maxOrNull()

        if (bestTarget.features.requiresBasePlace) {
            candidates.filterNot { it.features.requiresBasePlace }.maxOrNull()?.let { nonBasePlaceTarget ->
                val availableNonBaseTarget = if (!nonBasePlaceTarget.features.notBlockedByCrystal) {
                    bestImmediateTarget ?: nonBasePlaceTarget
                } else {
                    nonBasePlaceTarget
                }

                if (bestTarget.features.enemyDamage - availableNonBaseTarget.features.enemyDamage <= minAdvantage) {
                    bestTarget = availableNonBaseTarget
                }
            }
        }

        if (!bestTarget.features.notBlockedByCrystal &&
            !bestTarget.features.requiresBasePlace &&
            bestImmediateTarget != null
        ) {
            bestTarget = bestImmediateTarget
        }

        currentBasePlaceTarget?.let {
            if ((!it.features.requiresBasePlace || it.features.hasFoundation) &&
                bestTarget.features.enemyDamage - it.features.enemyDamage <= minAdvantage
            ) {
                bestTarget = it
            }
        }

        return bestTarget
    }

    private fun <T> List<PlacementCandidateChoice<T>>.maxWithFoundation(): PlacementCandidateChoice<T>? =
        sortedDescending().firstOrNull {
            !it.features.requiresBasePlace || it.features.hasFoundation
        }

}
