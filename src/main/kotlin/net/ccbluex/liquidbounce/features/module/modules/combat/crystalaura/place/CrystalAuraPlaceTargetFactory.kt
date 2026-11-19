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

import it.unimi.dsi.fastutil.ints.IntCollection
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SubmoduleBasePlace
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer.getMaxRange
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer.oldVersion
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.conditions.AirAboveCondition
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.conditions.AirOldVersionCondition
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.conditions.BasePlaceCondition
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.conditions.OnlyAboveCondition
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.conditions.PredictBlockageCondition
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.conditions.ValidHeightCondition
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.utils.aiming.utils.findClosestPointOnBlockInLineWithCrystal
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceUpperBlockSide
import net.ccbluex.liquidbounce.utils.block.getSortedSphere
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntitiesReturnCrystal
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity

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

    private var sphere: Array<BlockPos> = BlockPos.ZERO.getSortedSphere(4.5f)

    fun updateSphere() {
        sphere = BlockPos.ZERO.getSortedSphere(getMaxRange())
    }

    /**
     * Evaluates placements without changing placement or BasePlace state. The caller is responsible for making
     * [target] the current damage target while this method runs.
     */
    internal fun findImmediatePlacementOpportunities(target: LivingEntity): List<PlacementPositionCandidate> {
        val positions = mutableListOf<PlacementPositionCandidate>()
        evaluateCandidatePositions(
            basePlace = false,
            excludeIds = null,
            positions = positions,
            targetOverride = target,
            immediateOnly = true
        )

        return positions.filter { it.isImmediatelyActionable && hasActionableFace(it.pos) }
    }

    private fun hasActionableFace(pos: BlockPos): Boolean {
        val eyePos = player.eyePosition
        val range = SubmoduleCrystalPlacer.range.toDouble()
        val wallsRange = SubmoduleCrystalPlacer.wallsRange.toDouble()
        return if (SubmoduleCrystalPlacer.onlyAbove) {
            raytraceUpperBlockSide(eyePos, range, wallsRange, pos) != null
        } else {
            findClosestPointOnBlockInLineWithCrystal(
                eyePos,
                range,
                wallsRange,
                pos,
                notFacingAway = SubmoduleCrystalPlacer.notFacingAway,
                rotationsNotToMatch = null
            ) != null
        }
    }

    fun updateTarget(excludeIds: IntCollection?) {
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
        excludeIds: IntCollection?,
        positions: MutableList<PlacementPositionCandidate>,
        targetOverride: LivingEntity? = null,
        immediateOnly: Boolean = false
    ): Boolean {
        val target = targetOverride ?: ModuleCrystalAura.targetTracker.target ?: return true
        val expectedCrystal = if (oldVersion) FULL_BOX.setMaxY(2.0) else FULL_BOX
        val basePlaceLayers = if (basePlace) SubmoduleBasePlace.getBasePlaceLayers(target.y) else IntRange.EMPTY

        // create the context
        val context = PlacementContext(basePlace, basePlaceLayers, expectedCrystal, target)

        val playerPos = player.blockPosition()
        val pos = BlockPos.MutableBlockPos()
        sphere.forEach {
            // conditionChain
            pos.set(playerPos).move(it)

            val cache = CandidateCache(pos)
            if (conditionChain.all { condition -> condition.isValid(context, cache, pos) }) {
                val blocked = cache.up.isBlockedByEntitiesReturnCrystal(
                    box = expectedCrystal, excludeIds = excludeIds, buildingOnly = false)
                val crystal = blocked.value() != null
                if (immediateOnly) {
                    if (!blocked.keyBoolean() && !crystal && cache.canPlace) {
                        positions.add(PlacementPositionCandidate(pos.immutable(), true, false))
                    }
                } else if (!blocked.keyBoolean() || crystal) {
                    positions.add(PlacementPositionCandidate(pos.immutable(), !crystal, !cache.canPlace))
                }
            }
        }

        return false
    }

    private fun selectOptimalCandidate(
        finalPositions: List<PlacementPositionCandidate>,
        currentBasePlaceTarget: PlacementPositionCandidate?
    ): PlacementPositionCandidate? {
        currentBasePlaceTarget?.calculate()
        val currentChoice = currentBasePlaceTarget?.takeIf(PlacementPositionCandidate::isNotInvalid)?.let {
            it.toChoice(!it.requiresBasePlace || SubmoduleBasePlace.hasFoundation(it.pos))
        }

        return PlacementCandidateDecider.select(
            finalPositions.toChoices(),
            currentChoice,
            SubmoduleBasePlace.minAdvantage
        )?.source
    }

    /**
     * Supplies foundation inputs only until ordered selection has found its first usable candidate. Lower base-place
     * candidates cannot win the initial comparison and are not queried; a retained target is checked independently.
     */
    private fun List<PlacementPositionCandidate>.toChoices():
        List<PlacementCandidateChoice<PlacementPositionCandidate>> {
        var bestOverallFound = false
        return sortedDescending().map { candidate ->
            val hasFoundation = when {
                !candidate.requiresBasePlace -> true
                bestOverallFound -> false
                else -> SubmoduleBasePlace.hasFoundation(candidate.pos)
            }

            if (!candidate.requiresBasePlace || hasFoundation) {
                bestOverallFound = true
            }

            candidate.toChoice(hasFoundation)
        }
    }

    private fun PlacementPositionCandidate.toChoice(hasFoundation: Boolean) =
        PlacementCandidateChoice(
            source = this,
            features = PlacementCandidateFeatures(
                enemyDamage = enemyDamage!!,
                selfDamage = selfDamage!!,
                distanceSq = distanceSq,
                requiresBasePlace = requiresBasePlace,
                notBlockedByCrystal = notBlockedByCrystal,
                hasFoundation = hasFoundation
            )
        )

}
