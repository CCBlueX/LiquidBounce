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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.CrystalAuraDamageOptions.RequestingSubmodule
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

/**
 * Drops a queued place or break when the situation changed before the rotation finished.
 */
object SubmoduleAbort : ToggleableValueGroup(ModuleCrystalAura, "Abort", false) {

    private val maxTargetMove by float("MaxTargetMove", 0.5f, 0f..3f, "blocks")
    private val damageTolerance by float("DamageTolerance", 2f, 0f..10f)

    /**
     * Captures the situation a place or break at [crystal] dealing [damage] to the target was planned in,
     * or `null` when aborting is disabled.
     */
    fun capture(crystal: Vec3, damage: Float, requester: RequestingSubmodule): Plan? {
        if (!enabled) {
            return null
        }

        val target = ModuleCrystalAura.targetTracker.target ?: return null
        return Plan(target, target.position(), crystal, damage, requester)
    }

    class Plan internal constructor(
        private val target: LivingEntity,
        private val targetPos: Vec3,
        private val crystal: Vec3,
        private val damage: Float,
        private val requester: RequestingSubmodule
    ) {

        /**
         * Whether the target changed, moved more than [maxTargetMove], or the crystal now deals
         * more than [damageTolerance] less damage to it.
         */
        fun isOutdated(): Boolean {
            if (target !== ModuleCrystalAura.targetTracker.target || target.isRemoved || target.isDeadOrDying) {
                return true
            }

            if (target.position().distanceToSqr(targetPos) > maxTargetMove.sq()) {
                return true
            }

            val damageNow = CrystalAuraDamageOptions.approximateExplosionDamage(crystal, requester)?.secondFloat()
                ?: return true

            return damage - damageNow > damageTolerance
        }

    }

}
