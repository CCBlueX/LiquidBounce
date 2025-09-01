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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.player.autobuff.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.Buff
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.item.PotionItem
import net.minecraft.item.SplashPotionItem

object Drink : Buff("Drink") {

    private object HealthPotion : ToggleableConfigurable(Drink, "HealthPotion", true) {
        private val healthPercent by int("Health", 40, 1..100, "%HP")

        val health
            get() = player.maxHealth * healthPercent / 100

    }

    private object RegenPotion : ToggleableConfigurable(Drink, "RegenPotion", true) {
        private val healthPercent by int("Health", 70, 1..100, "%HP")

        val health
            get() = player.maxHealth * healthPercent / 100

    }

    init {
        tree(HealthPotion)
        tree(RegenPotion)
    }

    private val strengthPotion by boolean("StrengthPotion", true)
    private val speedPotion by boolean("SpeedPotion", true)

    private var forceUseKey = false

    override suspend fun execute(sequence: Sequence, slot: HotbarItemSlot) {
        forceUseKey = true
        sequence.waitUntil { !passesRequirements }
        forceUseKey = false
    }

    @Suppress("unused")
    private val keyBindIsPressedHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding == mc.options.useKey && forceUseKey) {
            event.isPressed = true
        }
    }

    override fun onDisabled() {
        forceUseKey = false
        super.onDisabled()
    }

    override fun isValidItem(stack: ItemStack, forUse: Boolean): Boolean {
        if (stack.isNothing() || !isValidPotion(stack)) {
            return false
        }

        val health = if (forUse) player.health else 0f

        return stack.getPotionEffects().any { foundTargetEffect(it, health) }
    }

    private fun isValidPotion(stack: ItemStack) =
        stack.item is PotionItem && stack.item !is SplashPotionItem

    private fun foundTargetEffect(effect: StatusEffectInstance, health: Float) =
        when (effect.effectType) {
            StatusEffects.INSTANT_HEALTH -> HealthPotion.enabled && health <= HealthPotion.health
            StatusEffects.REGENERATION -> RegenPotion.enabled && health <= RegenPotion.health
                && !player.hasStatusEffect(StatusEffects.REGENERATION)
            StatusEffects.STRENGTH -> strengthPotion && !player.hasStatusEffect(StatusEffects.STRENGTH)
            StatusEffects.SPEED -> speedPotion && !player.hasStatusEffect(StatusEffects.SPEED)
            else -> false
        }

}
