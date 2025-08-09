/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SubmoduleBasePlace
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer.getMaxRange
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer.oldVersion
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.conditions.*
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.utils.block.getSortedSphere
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntitiesReturnCrystal
import net.minecraft.util.math.BlockPos

object CrystalAuraPlaceTargetFactory : MinecraftShortcuts {

    var placementTarget: BlockPos? = null
    var previousTarget: BlockPos? = null

    /**
     * Conditions to include a position into the positions to consider.
     */
    private val conditionChain: Array<PlacementCondition> = arrayOf(
        AirAboveCondition,
        AirOldVersionCondition,
        OnlyAboveCondition,
        ValidHeightCondition,
        BasePlaceCondition,
        PredictBlockageCondition
    )

    private var sphere: Array<BlockPos> = BlockPos.ORIGIN.getSortedSphere(4.5f)

    fun updateSphere() {
        sphere = BlockPos.ORIGIN.getSortedSphere(getMaxRange())
    }

    fun updateTarget(excludeIds : IntArray?) {
        // Reset current target
        previousTarget = placementTarget
        placementTarget = null

        val basePlace = SubmoduleBasePlace.shouldBasePlaceRun()
        val currentBasePlaceTarget = if (basePlace) null else SubmoduleBasePlace.currentTarget

        val positions = mutableListOf<PlacementPositionCandidate>()

        // find all valid positions
        if (evaluateCandidatePositions(basePlace, excludeIds, positions)) {
            // return if the crystal aura doesn't target anything
            return
        }

        // collect all valid positions
        val finalPositions = positions.filter(PlacementPositionCandidate::isNotInvalid)

        // choose the best available target
        val bestTarget = selectOptimalCandidate(finalPositions, currentBasePlaceTarget) ?: return

        // set the base place target
        val notCurrentBasePlaceTarget = bestTarget != currentBasePlaceTarget
        if (bestTarget.requiresBasePlace && notCurrentBasePlaceTarget) {
            SubmoduleBasePlace.currentTarget = bestTarget
        } else if (notCurrentBasePlaceTarget) {
            // base place isn't required
            SubmoduleBasePlace.currentTarget = null
        }

        // finally, update the current target
        if (bestTarget.notBlockedByCrystal && !bestTarget.requiresBasePlace) {
            placementTarget = bestTarget.pos
        }
    }

    private fun evaluateCandidatePositions(
        basePlace: Boolean,
        excludeIds: IntArray?,
        positions: MutableList<PlacementPositionCandidate>
    ): Boolean {
        val target = ModuleCrystalAura.targetTracker.target ?: return true
        val expectedCrystal = if (oldVersion) FULL_BOX.withMaxX(2.0) else FULL_BOX
        val basePlaceLayers = if (basePlace) SubmoduleBasePlace.getBasePlaceLayers(target.y) else IntRange.EMPTY

        // create the context
        val context = PlacementContext(basePlace, basePlaceLayers, expectedCrystal, target)

        val playerPos = player.blockPos
        val pos = BlockPos.Mutable()
        sphere.forEach {
            // conditionChain
            pos.set(playerPos).move(it)

            val cache = CandidateCache(pos)
            if (conditionChain.all { condition -> condition.isValid(context, cache, pos) }) {
                val blocked = cache.up.isBlockedByEntitiesReturnCrystal(box = expectedCrystal, excludeIds = excludeIds)
                val crystal = blocked.value() != null
                if (!blocked.keyBoolean() || crystal) {
                    positions.add(PlacementPositionCandidate(pos.toImmutable(), !crystal, !cache.canPlace))
                }
            }
        }

        return false
    }

    private fun selectOptimalCandidate(
        finalPositions: List<PlacementPositionCandidate>,
        currentBasePlaceTarget: PlacementPositionCandidate?
    ): PlacementPositionCandidate? {
        // choose the target with the maximum damage
        var bestTarget = finalPositions.maxOrNull() ?: return null

        // find a target position that will not require base place if possible
        if (bestTarget.requiresBasePlace) {
            finalPositions.filterNot { it.requiresBasePlace }.maxOrNull()?.let {
                if (it.enemyDamage!! - bestTarget.enemyDamage!! >= SubmoduleBasePlace.minAdvantage) {
                    bestTarget = it
                }
            }
        }

        // if base place already is placing a block and this is block is still relevant, we wait for it
        currentBasePlaceTarget?.let {
            it.calculate()
            if (it.isNotInvalid() &&
                it.enemyDamage!! - bestTarget.enemyDamage!! >= SubmoduleBasePlace.minAdvantage
            ) {
                bestTarget = it
            }
        }

        return bestTarget
    }

}
