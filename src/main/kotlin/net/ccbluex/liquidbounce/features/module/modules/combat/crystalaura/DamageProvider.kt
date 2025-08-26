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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

interface DamageProvider {
    fun isSmallerThan(float: Float): Boolean
    fun isSmallerThanOrEqual(float: Float): Boolean
    fun isSmallerThanOrEqual(other: DamageProvider): Boolean
    fun isGreaterThan(float: Float): Boolean

    /**
     * Any because it's used for death prevention.
     */
    fun isAnyGreaterThanOrEqual(float: Float): Boolean

    /**
     * Returns the most important damage value that should also be used for comparison.
     */
    fun getFixed(): Float
}

class NormalDamageProvider(val damage: Float) : DamageProvider {

    override fun isSmallerThan(float: Float): Boolean {
        return damage < float
    }

    override fun isSmallerThanOrEqual(float: Float): Boolean {
        return damage <= float
    }

    override fun isSmallerThanOrEqual(other: DamageProvider): Boolean {
        return when (other) {
            is NormalDamageProvider -> isSmallerThanOrEqual(other.damage)
            is BiDamageProvider -> other.isGreaterThan(damage)
            else -> isSmallerThanOrEqual(getFixed())
        }
    }

    override fun isGreaterThan(float: Float): Boolean {
        return damage > float
    }

    override fun isAnyGreaterThanOrEqual(float: Float): Boolean {
        return damage >= float
    }

    override fun getFixed() = damage

}

abstract class BiDamageProvider(val damage: Float, val damage1: Float) : DamageProvider {

    /**
     * The possible predicted value is more important.
     */
    override fun getFixed() = damage1

}

class OrBiDamageProvider(damage: Float, damage1: Float) : BiDamageProvider(damage, damage1) {

    override fun isSmallerThan(float: Float): Boolean {
        return damage < float || damage1 < float
    }

    override fun isSmallerThanOrEqual(float: Float): Boolean {
        return damage <= float || damage1 <= float
    }

    override fun isSmallerThanOrEqual(other: DamageProvider): Boolean {
        return when (other) {
            is NormalDamageProvider -> isSmallerThanOrEqual(other.damage)
            is BiDamageProvider -> other.isGreaterThan(damage) || other.isGreaterThan(damage1)
            else -> isSmallerThanOrEqual(getFixed())
        }
    }

    override fun isGreaterThan(float: Float): Boolean {
        return damage > float || damage1 > float
    }

    override fun isAnyGreaterThanOrEqual(float: Float): Boolean {
        return damage >= float || damage1 >= float
    }

}

class AndBiDamageProvider(damage: Float, damage1: Float) : BiDamageProvider(damage, damage1) {

    override fun isSmallerThan(float: Float): Boolean {
        return damage < float && damage1 < float
    }

    override fun isSmallerThanOrEqual(float: Float): Boolean {
        return damage <= float && damage1 <= float
    }

    override fun isSmallerThanOrEqual(other: DamageProvider): Boolean {
        return when (other) {
            is NormalDamageProvider -> isSmallerThanOrEqual(other.damage)
            is BiDamageProvider -> other.isGreaterThan(damage) && other.isGreaterThan(damage1)
            else -> isSmallerThanOrEqual(getFixed())
        }
    }

    override fun isGreaterThan(float: Float): Boolean {
        return damage > float && damage1 > float
    }

    override fun isAnyGreaterThanOrEqual(float: Float): Boolean {
        return damage >= float || damage1 >= float
    }

}
