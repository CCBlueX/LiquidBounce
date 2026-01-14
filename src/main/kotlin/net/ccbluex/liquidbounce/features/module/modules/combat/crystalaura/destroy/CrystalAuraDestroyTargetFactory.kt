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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.CrystalAuraDamageOptions
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer.getMaxRange
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer.range
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer.wallsRange
import net.ccbluex.liquidbounce.utils.aiming.utils.canSeeBox
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal

object CrystalAuraDestroyTargetFactory : MinecraftShortcuts {

    var currentTarget: EndCrystal? = null

    /**
     * For the main loop. Finds the best target in range.
     *
     * Updates the current target.
     */
    fun updateTarget() {
        currentTarget =
            world.getEntitiesBoxInRange(player.getEyePosition(1.0F), getMaxRange().toDouble()) {
                it is EndCrystal
            }.mapNotNull {
                if (cannotSeeEntity(it)) {
                    return@mapNotNull null
                }

                val damage = dealsEnoughDamage(it) ?: return@mapNotNull null

                ComparisonListEntry(it as EndCrystal, damage.firstFloat(), damage.secondFloat())
            }.maxOrNull()?.crystalEntity
    }

    /**
     * For specific attacks, only checks if the given [crystal] is valid.
     *
     * Updates the current target.
     */
    fun validateAndUpdateTarget(crystal: EndCrystal) {
        val maxRange = getMaxRange().toDouble() + crystal.boundingBox.maxX - crystal.boundingBox.minX
        currentTarget = null
        if (player.eyePosition.distanceToSqr(crystal.position()) > maxRange.sq()) {
            return
        }

        if (cannotSeeEntity(crystal)) {
            return
        }

        dealsEnoughDamage(crystal) ?: return

        currentTarget = crystal
    }

    private fun dealsEnoughDamage(entity: Entity) =
        CrystalAuraDamageOptions.approximateExplosionDamage(
            entity.position(),
            CrystalAuraDamageOptions.RequestingSubmodule.DESTROY
        )

    private fun cannotSeeEntity(entity: Entity) = !canSeeBox(
        player.eyePosition,
        entity.boundingBox,
        range = range.toDouble(),
        wallsRange = wallsRange.toDouble()
    )

    private class ComparisonListEntry(
        val crystalEntity: EndCrystal,
        val selfDamage: Float,
        val enemyDamage: Float
    ) : Comparable<ComparisonListEntry> {

        override fun compareTo(other: ComparisonListEntry): Int {
            // coarse sorting
            val enemyDamageComparison = this.enemyDamage.compareTo(other.enemyDamage)

            // not equal
            if (enemyDamageComparison != 0) {
                return enemyDamageComparison
            }

            // equal -> fine sorting
            return other.selfDamage.compareTo(this.selfDamage)
        }

    }

}
