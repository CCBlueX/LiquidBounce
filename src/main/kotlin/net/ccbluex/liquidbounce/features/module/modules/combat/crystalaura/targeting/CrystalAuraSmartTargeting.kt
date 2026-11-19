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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.CrystalAuraDamageOptions
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.CrystalAuraPlaceTargetFactory
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer
import net.ccbluex.liquidbounce.utils.aiming.utils.canSeeBox
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.isWithinWorldBorder
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.item.Items

/** Opt-in target selection based on crystal actions which can be used immediately. */
internal object CrystalAuraSmartTargeting : ToggleableValueGroup(ModuleCrystalAura, "SmartTargeting", false) {

    private const val MAX_CRYSTALS_PER_EVALUATION = 32

    private val evaluationLimit by int("EvaluationLimit", 4, 1..8)
    private val switchAdvantage by float("SwitchAdvantage", 1.0f, 0.0f..10.0f)
    private val switchDelay by int("SwitchDelay", 250, 0..2000, "ms")

    private var pendingTargetId: Int? = null
    private var pendingSince: Long? = null

    fun reset() {
        pendingTargetId = null
        pendingSince = null
    }

    override fun onDisabled() = reset()

    fun selectTarget() {
        val targets = ModuleCrystalAura.targetTracker.targets()
        if (targets.isEmpty()) {
            reset()
            ModuleCrystalAura.targetTracker.reset()
            return
        }

        val currentTarget = ModuleCrystalAura.targetTracker.target
        val evaluatedTargets = evaluationTargets(targets, currentTarget)
        val crystals = findDestroyCrystals()
        val canPlace = SubmoduleCrystalPlacer.enabled &&
            Slots.OffhandWithHotbar.findClosestSlot(Items.END_CRYSTAL) != null
        val opportunities = mutableListOf<SmartTargetOpportunity>()

        try {
            for (target in evaluatedTargets) {
                // DamageOptions uses the tracker's current target. Restore it before applying the final decision.
                ModuleCrystalAura.targetTracker.target = target
                val targetOpportunities = mutableListOf<SmartTargetOpportunity>()
                addDestroyOpportunities(target, crystals, targetOpportunities)
                if (canPlace) {
                    addPlacementOpportunities(target, targetOpportunities)
                }

                SmartTargetDecider.bestOpportunity(targetOpportunities)?.let(opportunities::add)
            }
        } finally {
            ModuleCrystalAura.targetTracker.target = currentTarget
        }

        val selection = SmartTargetDecider.select(
            opportunities = opportunities,
            currentTargetId = currentTarget?.id,
            pendingTargetId = pendingTargetId,
            pendingSince = pendingSince,
            now = System.currentTimeMillis(),
            switchAdvantage = switchAdvantage.toDouble(),
            switchDelayMs = switchDelay.toLong()
        )

        if (selection == null) {
            // Keep legacy priority as a fallback so BasePlace can still establish a new opportunity.
            reset()
            ModuleCrystalAura.targetTracker.selectFirst()
            return
        }

        ModuleCrystalAura.targetTracker.target = evaluatedTargets.first { it.id == selection.targetId }
        pendingTargetId = selection.pendingTargetId
        pendingSince = selection.pendingSince
    }

    private fun evaluationTargets(
        targets: List<LivingEntity>,
        currentTarget: LivingEntity?
    ): List<LivingEntity> {
        val result = ArrayList<LivingEntity>(evaluationLimit.coerceAtMost(targets.size))
        currentTarget?.let { current ->
            targets.firstOrNull { it.id == current.id }?.let(result::add)
        }

        for (target in targets) {
            if (result.size >= evaluationLimit) {
                break
            }

            if (result.none { it.id == target.id }) {
                result.add(target)
            }
        }

        return result
    }

    private fun findDestroyCrystals(): List<EndCrystal> {
        if (!SubmoduleCrystalDestroyer.enabled) {
            return emptyList()
        }

        val eyePos = player.eyePosition
        return world.getEntitiesBoxInRange(
            eyePos,
            SubmoduleCrystalDestroyer.getMaxRange().toDouble()
        ) {
            it is EndCrystal && it.isWithinWorldBorder && canSeeBox(
                eyePos,
                it.boundingBox,
                SubmoduleCrystalDestroyer.range.toDouble(),
                SubmoduleCrystalDestroyer.wallsRange.toDouble()
            )
        }.filterIsInstance<EndCrystal>()
            .sortedBy { it.id }
            .take(MAX_CRYSTALS_PER_EVALUATION)
    }

    private fun addDestroyOpportunities(
        target: LivingEntity,
        crystals: List<EndCrystal>,
        output: MutableList<SmartTargetOpportunity>
    ) {
        for (crystal in crystals) {
            val damage = CrystalAuraDamageOptions.approximateExplosionDamage(
                crystal.position(),
                CrystalAuraDamageOptions.RequestingSubmodule.DESTROY
            ) ?: continue

            output.add(target.opportunity(
                enemyDamage = damage.secondFloat(),
                selfDamage = damage.firstFloat(),
                actionDistanceSq = crystal.squaredBoxedDistanceTo(player.eyePosition),
                action = SmartTargetAction.DESTROY
            ))
        }
    }

    private fun addPlacementOpportunities(
        target: LivingEntity,
        output: MutableList<SmartTargetOpportunity>
    ) {
        val candidates = CrystalAuraPlaceTargetFactory.findImmediatePlacementOpportunities(target)
        for (candidate in candidates) {
            output.add(target.opportunity(
                enemyDamage = candidate.enemyDamage!!,
                selfDamage = candidate.selfDamage!!,
                actionDistanceSq = candidate.distanceSq,
                action = SmartTargetAction.PLACE
            ))
        }
    }

    private fun LivingEntity.opportunity(
        enemyDamage: Float,
        selfDamage: Float,
        actionDistanceSq: Double,
        action: SmartTargetAction
    ) = SmartTargetOpportunity(
        targetId = id,
        enemyDamage = enemyDamage,
        selfDamage = selfDamage,
        targetHealth = getActualHealth() + absorptionAmount,
        targetDistanceSq = squaredBoxedDistanceTo(player),
        actionDistanceSq = actionDistanceSq,
        action = action
    )

}
