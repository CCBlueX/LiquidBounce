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

package net.ccbluex.liquidbounce.features.module.modules.misc.soundfx

import net.ccbluex.liquidbounce.LiquidBounce.identifier
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.sounds.SoundEvent

object Sounds {

    enum class SoundKey(val ids: List<String>) {
        BONK(listOf("bonk")),
        POP(listOf("pop")),
        UWU(listOf("uwu")),
        NYA(listOf("nya")),
        TUNG(listOf("tung")),
        MEOW(listOf("meow")),
        BRING(listOf("bring")),
        SOFT(listOf("soft")),
        SQUASH(listOf("squash")),
        MAGICSQUASH(listOf("magicsquash")),

        CLICK(listOf("click-1", "click-2", "click-3")),
        BOYKISSER(listOf("boykisser-1","boykisser-2","boykisser-3","boykisser-4","boykisser-5","boykisser-6")),
        GLASS(listOf("glass-1","glass-2","glass-3")),
        MOAN(listOf("moan-1","moan-2","moan-3","moan-4"))
    }

    private val soundsMap = mutableMapOf<SoundKey, List<SoundEvent>>()

    fun get(key: SoundKey): SoundEvent = soundsMap[key]?.random() ?: error("Sound ${key.name} not registered")

    fun registerAll() {
        SoundKey.entries.forEach { key ->
            val events = key.ids.map { id ->
                val soundId = identifier(id)
                Registry.register(
                    BuiltInRegistries.SOUND_EVENT,
                    soundId,
                    SoundEvent.createVariableRangeEvent(soundId)
                )
            }
            soundsMap[key] = events
        }
    }
}
