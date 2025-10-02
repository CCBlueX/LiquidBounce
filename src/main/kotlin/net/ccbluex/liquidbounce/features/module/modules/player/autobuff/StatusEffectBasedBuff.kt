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

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.registry.entry.RegistryEntry

abstract class StatusEffectBasedBuff(name: String) : Buff(name) {

    private open class Potion(
        parent: StatusEffectBasedBuff,
        name: String,
        val statusEffect: RegistryEntry<StatusEffect>,
    ) : ToggleableConfigurable(parent, name, true) {

        open fun isValid(effect: StatusEffectInstance, health: Float): Boolean {
            return enabled && statusEffect == effect.effectType && !player.hasStatusEffect(statusEffect)
        }
    }

    private class HealthBasedPotion(
        parent: StatusEffectBasedBuff,
        name: String,
        statusEffect: RegistryEntry<StatusEffect>,
    ) : Potion(parent, name, statusEffect) {
        private val healthPercent by int("Health", 40, 1..100, "%HP")

        private val health
            get() = player.maxHealth * healthPercent / 100

        override fun isValid(effect: StatusEffectInstance, health: Float): Boolean {
            return super.isValid(effect, health) && health <= this.health
        }
    }

    private class Potions(parent: StatusEffectBasedBuff) : Configurable("Potions") {

        private val healthPotion = HealthBasedPotion(parent, "Health", StatusEffects.INSTANT_HEALTH)
        private val regenPotion = HealthBasedPotion(parent, "Regen", StatusEffects.REGENERATION)
        private val strengthPotion = Potion(parent, "Strength", StatusEffects.STRENGTH)
        private val speedPotion = Potion(parent, "Speed", StatusEffects.SPEED)
        private val fireResistancePotion = Potion(parent, "FireResistance", StatusEffects.FIRE_RESISTANCE)
        private val jumpBoostPotion = Potion(parent, "JumpBoost", StatusEffects.JUMP_BOOST)
        private val waterBreathingPotion = Potion(parent, "WaterBreathing", StatusEffects.WATER_BREATHING)

        private val values = arrayOf(
            healthPotion,
            regenPotion,
            strengthPotion,
            speedPotion,
            fireResistancePotion,
            jumpBoostPotion,
            waterBreathingPotion,
        ).onEach(::tree).associateBy { it.statusEffect }

        operator fun get(statusEffect: RegistryEntry<StatusEffect>) = values[statusEffect]
    }

    private val potions = tree(Potions(this))

    protected fun foundTargetEffect(effect: StatusEffectInstance, health: Float) =
        potions[effect.effectType]?.isValid(effect, health) ?: false

    protected abstract fun isValidPotion(stack: ItemStack): Boolean

    final override fun isValidItem(stack: ItemStack, forUse: Boolean): Boolean {
        if (stack.isEmpty || !isValidPotion(stack)) {
            return false
        }

        val health = if (forUse) player.health else 0f

        return stack.getPotionEffects().any { foundTargetEffect(it, health) }
    }

}
