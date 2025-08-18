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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.registry.Registries
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Language

/**
 * PotionSpoof
 *
 * Allows the player to have potion effects without actually having the potion.
 */
object ModulePotionSpoof : ClientModule("PotionSpoof", Category.PLAYER) {

    private class StatusEffectConfigurable(
        val registryEntry: RegistryEntry<StatusEffect>,
        specifiedLanguage: Map<String, String>,
    ) : ToggleableConfigurable(
        parent = this,
        // Value name (en_us)
        name = specifiedLanguage.getOrDefault(registryEntry.value().translationKey, "Unknown"),
        enabled = false,
    ) {
        private val level = int("Level", 1, 1..10).onChanged {
            instance = StatusEffectInstance(registryEntry, 0, it - 1)
        }

        var instance: StatusEffectInstance = StatusEffectInstance(registryEntry, 0, level.get() - 1)
            private set
    }

    private val statusEffectValues = run {
        /** @see Language.create */
        val language = Language::class.java.getResourceAsStream("/assets/minecraft/lang/en_us.json").let { stream ->
            val map = HashMap<String, String>(8192)
            Language.load(stream, map::put)
            map
        }

        Registries.STATUS_EFFECT.streamEntries().map {
            tree(StatusEffectConfigurable(it, specifiedLanguage = language))
        }.toList()
    }

    override fun onDisabled() {
        for (spoofedEffect in statusEffectValues) {
            if (spoofedEffect.enabled && player.getStatusEffect(spoofedEffect.registryEntry)?.duration == 0) {
                player.removeStatusEffect(spoofedEffect.registryEntry)
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        for (configurable in statusEffectValues) {
            if (configurable.enabled) {
                player.addStatusEffect(configurable.instance)
            } else if (player.getStatusEffect(configurable.registryEntry)?.duration == 0) {
                player.removeStatusEffect(configurable.registryEntry)
            }
        }
    }
}
