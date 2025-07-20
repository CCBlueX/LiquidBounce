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

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.CrystalAuraDamageOptions
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class PlacementPositionCandidate(
    val pos: BlockPos, // the block the crystal should be placed on
    val notBlockedByCrystal: Boolean,
    val requiresBasePlace: Boolean
) : Comparable<PlacementPositionCandidate> {

    /**
     * The damage a crystal at the specific position would deal to the enemy.
     */
    var enemyDamage: Float? = null

    /**
     * The damage a crystal at the specific position would deal to the enemy.
     */
    private var selfDamage: Float? = null

    /**
     * The distance to us.
     */
    private val distanceSq by lazy { pos.getSquaredDistance(player.pos) }

    init {
        calculate()
    }

    /**
     * Evaluates the explosion damage to the target, sets it to `null` if the position is invalid.
     */
    fun calculate() {
        val damageSourceLoc = Vec3d.of(pos).add(0.5, 1.0, 0.5)
        val explosionDamage = CrystalAuraDamageOptions.approximateExplosionDamage(
            damageSourceLoc,
            if (requiresBasePlace) {
                CrystalAuraDamageOptions.RequestingSubmodule.BASE_PLACE
            } else {
                CrystalAuraDamageOptions.RequestingSubmodule.PLACE
            }
        )

        explosionDamage?.let {
            selfDamage = it.firstFloat()
            enemyDamage = it.secondFloat()
        } ?: run {
            selfDamage = null
            enemyDamage = null
        }
    }

    fun isNotInvalid() = enemyDamage != null

    override fun compareTo(other: PlacementPositionCandidate): Int {
        // coarse sorting
        val enemyDamageComparison = this.enemyDamage!!.compareTo(other.enemyDamage!!)

        // not equal
        if (enemyDamageComparison != 0) {
            return enemyDamageComparison
        }

        // equal -> fine sorting 1
        val selfDamageComparison = other.selfDamage!!.compareTo(this.selfDamage!!)

        // not equal
        if (selfDamageComparison != 0) {
            return selfDamageComparison
        }

        // equal -> fine sorting 2
        return other.distanceSq.compareTo(this.distanceSq)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlacementPositionCandidate

        if (pos != other.pos) return false
        if (requiresBasePlace != other.requiresBasePlace) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pos.hashCode()
        result = 31 * result + requiresBasePlace.hashCode()
        return result
    }

}
