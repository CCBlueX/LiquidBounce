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

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.minecraft.client.resource.language.I18n
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects.*
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Language

/**
 * PotionSpoof
 *
 * Allows the player to have potion effects without actually having the potion.
 */
object ModulePotionSpoof : ClientModule("PotionSpoof", Category.PLAYER) {

    /**
     * @see net.minecraft.entity.effect.StatusEffects
     */
    private val ALL_STATUS_EFFECT = arrayOf(
        SPEED, SLOWNESS, HASTE, MINING_FATIGUE, STRENGTH,
        INSTANT_HEALTH, INSTANT_DAMAGE, JUMP_BOOST, NAUSEA,
        REGENERATION, RESISTANCE, FIRE_RESISTANCE, WATER_BREATHING,
        INVISIBILITY, BLINDNESS, NIGHT_VISION, HUNGER, WEAKNESS,
        POISON, WITHER, HEALTH_BOOST, ABSORPTION, SATURATION,
        GLOWING, LEVITATION, LUCK, UNLUCK, SLOW_FALLING,
        CONDUIT_POWER, DOLPHINS_GRACE, BAD_OMEN, HERO_OF_THE_VILLAGE,
        DARKNESS, TRIAL_OMEN, RAID_OMEN, WIND_CHARGED, WEAVING,
        OOZING, INFESTED
    )

    private class StatusEffectConfigurable(
        val registryEntry: RegistryEntry<StatusEffect>,
        translationKey: String = "effect.minecraft." + registryEntry.key.get().value.toShortTranslationKey(),
        specifiedLanguage: Map<String, String>,
    ) : ToggleableConfigurable(
        parent = this,
        // Value name (en_us)
        name = specifiedLanguage.getOrDefault(translationKey, "Unknown"),
        enabled = false,
        // Localized name
        aliases = if (I18n.hasTranslation(translationKey)) arrayOf(I18n.translate(translationKey)) else emptyArray()
    ) {
        private val level = int("Level", 1, 1..10).onChanged {
            instance = StatusEffectInstance(registryEntry, 0, it - 1)
        }
        var instance: StatusEffectInstance = StatusEffectInstance(registryEntry, 0, level.get() - 1)
            private set
    }

    private val statusEffectValues = run {
        val language = Language::class.java.getResourceAsStream("/assets/minecraft/lang/en_us.json").let { stream ->
            val map = HashMap<String, String>(8192)
            Language.load(stream, map::put)
            map
        }

        ALL_STATUS_EFFECT.mapArray {
            tree(StatusEffectConfigurable(it, specifiedLanguage = language))
        }
    }

    override fun disable() {
        for (configurable in statusEffectValues) {
            if (configurable.enabled && player.getStatusEffect(configurable.registryEntry) == configurable.instance) {
                player.removeStatusEffect(configurable.registryEntry)
            }
        }
    }

    override fun enable() {
        if (statusEffectValues.none { it.enabled }) {
            chat(message("nothingEnabled"), this)
            this.enabled = false
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        for (configurable in statusEffectValues) {
            if (configurable.enabled) {
                player.addStatusEffect(configurable.instance)
            } else if (player.getStatusEffect(configurable.registryEntry) == configurable.instance) {
                player.removeStatusEffect(configurable.registryEntry)
            }
        }
    }
}
