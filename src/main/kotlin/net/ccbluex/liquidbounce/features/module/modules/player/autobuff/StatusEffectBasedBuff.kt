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

package net.ccbluex.liquidbounce.features.module.modules.player.autobuff

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.registry.entry.RegistryEntry

abstract class StatusEffectBasedBuff(name: String) : Buff(name) {

    private class HealthBasedPotion(
        parent: StatusEffectBasedBuff,
        name: String,
        val statusEffect: RegistryEntry<StatusEffect>,
    ) : ToggleableConfigurable(parent, name, true) {
        private val healthPercent by int("Health", 40, 1..100, "%HP")

        val health
            get() = player.maxHealth * healthPercent / 100
    }

    private val healthPotion = HealthBasedPotion(this, "HealthPotion", StatusEffects.INSTANT_HEALTH)
    private val regenPotion = HealthBasedPotion(this, "RegenPotion", StatusEffects.REGENERATION)

    init {
        tree(healthPotion)
        tree(regenPotion)
    }

    private val strengthPotion by boolean("StrengthPotion", true)
    private val speedPotion by boolean("SpeedPotion", true)
    private val fireResistancePotion by boolean("FireResistancePotion", true)

    protected fun foundTargetEffect(effect: StatusEffectInstance, health: Float) =
        when (effect.effectType) {
            StatusEffects.INSTANT_HEALTH -> healthPotion.enabled && health <= healthPotion.health
            StatusEffects.REGENERATION -> regenPotion.enabled && health <= regenPotion.health
            && !player.hasStatusEffect(StatusEffects.REGENERATION)
            StatusEffects.STRENGTH -> strengthPotion && !player.hasStatusEffect(StatusEffects.STRENGTH)
            StatusEffects.SPEED -> speedPotion && !player.hasStatusEffect(StatusEffects.SPEED)
            StatusEffects.FIRE_RESISTANCE -> fireResistancePotion &&
                !player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)
            else -> false
        }

}
