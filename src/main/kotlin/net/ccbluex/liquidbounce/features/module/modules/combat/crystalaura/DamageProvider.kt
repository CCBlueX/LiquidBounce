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

import kotlin.math.max

interface DamageProvider {
    fun isSmallerThan(float: Float): Boolean
    fun isSmallerThanOrEqual(float: Float): Boolean
    fun isSmallerThanOrEqual(other: DamageProvider): Boolean
    fun isGreaterThan(float: Float): Boolean
    fun isGreaterThanOrEqual(float: Float): Boolean

    /**
     * Any because it's used for self-damage caps and death prevention.
     */
    fun isAnyGreaterThan(float: Float): Boolean
    fun isAnyGreaterThanOrEqual(float: Float): Boolean

    /**
     * Returns the most important damage value that should also be used for comparison.
     */
    fun getFixed(): Float
}

internal fun DamageProvider.isLethal(targetHealth: Float): Boolean =
    targetHealth > 0 && isGreaterThanOrEqual(targetHealth)

class NormalDamageProvider(val damage: Float) : DamageProvider {

    override fun isSmallerThan(float: Float): Boolean = damage < float

    override fun isSmallerThanOrEqual(float: Float): Boolean = damage <= float

    override fun isSmallerThanOrEqual(other: DamageProvider): Boolean {
        return when (other) {
            is NormalDamageProvider -> isSmallerThanOrEqual(other.damage)
            is BiDamageProvider -> other.isGreaterThanOrEqual(damage)
            else -> isSmallerThanOrEqual(other.getFixed())
        }
    }

    override fun isGreaterThan(float: Float): Boolean = damage > float

    override fun isGreaterThanOrEqual(float: Float): Boolean = damage >= float

    override fun isAnyGreaterThan(float: Float): Boolean = damage > float

    override fun isAnyGreaterThanOrEqual(float: Float): Boolean = damage >= float

    override fun getFixed() = damage

}

abstract class BiDamageProvider(val damage: Float, val damage1: Float) : DamageProvider {

    override fun isAnyGreaterThan(float: Float): Boolean = damage > float || damage1 > float

    override fun isAnyGreaterThanOrEqual(float: Float): Boolean = damage >= float || damage1 >= float

    /**
     * The predicted value is prioritized by default.
     */
    override fun getFixed() = damage1

}

class OrBiDamageProvider(damage: Float, damage1: Float) : BiDamageProvider(damage, damage1) {

    override fun isSmallerThan(float: Float): Boolean = damage < float || damage1 < float

    override fun isSmallerThanOrEqual(float: Float): Boolean = damage <= float || damage1 <= float

    override fun isSmallerThanOrEqual(other: DamageProvider): Boolean {
        return when (other) {
            is NormalDamageProvider -> isSmallerThanOrEqual(other.damage)
            is BiDamageProvider -> other.isGreaterThanOrEqual(damage) || other.isGreaterThanOrEqual(damage1)
            else -> isSmallerThanOrEqual(other.getFixed())
        }
    }

    override fun isGreaterThan(float: Float): Boolean = damage > float || damage1 > float

    override fun isGreaterThanOrEqual(float: Float): Boolean = damage >= float || damage1 >= float

    override fun getFixed() = max(damage, damage1)

}

class AndBiDamageProvider(damage: Float, damage1: Float) : BiDamageProvider(damage, damage1) {

    override fun isSmallerThan(float: Float): Boolean = damage < float && damage1 < float

    override fun isSmallerThanOrEqual(float: Float): Boolean = damage <= float && damage1 <= float

    override fun isSmallerThanOrEqual(other: DamageProvider): Boolean {
        return when (other) {
            is NormalDamageProvider -> isSmallerThanOrEqual(other.damage)
            is AndBiDamageProvider -> damage <= other.damage && damage1 <= other.damage1
            is BiDamageProvider -> other.isGreaterThanOrEqual(damage) && other.isGreaterThanOrEqual(damage1)
            else -> isSmallerThanOrEqual(other.getFixed())
        }
    }

    override fun isGreaterThan(float: Float): Boolean = damage > float && damage1 > float

    override fun isGreaterThanOrEqual(float: Float): Boolean = damage >= float && damage1 >= float

}
