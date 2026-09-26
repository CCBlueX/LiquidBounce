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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.locale.Language
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance

/**
 * PotionSpoof
 *
 * Allows the player to have potion effects without actually having the potion.
 */
object ModulePotionSpoof : ClientModule("PotionSpoof", ModuleCategories.PLAYER) {

    private class StatusEffectValueGroup(
        val registryEntry: Holder<MobEffect>,
        specifiedLanguage: Map<String, String>,
    ) : ToggleableValueGroup(
        parent = this,
        // Value name (en_us)
        name = specifiedLanguage.getOrDefault(registryEntry.value().descriptionId, "Unknown"),
        enabled = false,
    ) {
        private val level = int("Level", 1, 1..10).onChanged {
            instance = MobEffectInstance(registryEntry, 0, it - 1)
        }

        var instance: MobEffectInstance = MobEffectInstance(registryEntry, 0, level.get() - 1)
            private set
    }

    private val statusEffectValues = run {
        /** @see Language.loadDefault */
        val language = Language::class.java.getResourceAsStream("/assets/minecraft/lang/en_us.json").let { stream ->
            val map = HashMap<String, String>(8192)
            Language.loadFromJson(stream, map::put)
            map
        }

        BuiltInRegistries.MOB_EFFECT.listElements().map {
            tree(StatusEffectValueGroup(it, specifiedLanguage = language))
        }.toList()
    }

    override fun onDisabled() {
        for (spoofedEffect in statusEffectValues) {
            if (spoofedEffect.enabled && player.getEffect(spoofedEffect.registryEntry)?.duration == 0) {
                player.removeEffect(spoofedEffect.registryEntry)
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        for (effect in statusEffectValues) {
            if (effect.enabled) {
                player.addEffect(effect.instance)
                effect.instance.effect.value().addAttributeModifiers(
                    player.attributes,
                    effect.instance.amplifier
                )
            } else if (player.getEffect(effect.registryEntry)?.duration == 0) {
                player.removeEffect(effect.registryEntry)
                effect.instance.effect.value().removeAttributeModifiers(player.attributes)
            }
        }
    }
}
