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

    private val soundIds = listOf(
        "bonk",
        "pop",
        "click",
        "uwu",
        "nya",
        "tung",
        "meow",
    )

    val registeredSounds = mutableMapOf<String, SoundEvent>()

    lateinit var BONK: SoundEvent private set
    lateinit var POP: SoundEvent private set
    lateinit var CLICK: SoundEvent private set
    lateinit var UWU: SoundEvent private set
    lateinit var NYA: SoundEvent private set
    lateinit var TUNG: SoundEvent private set
    lateinit var MEOW: SoundEvent private set

    fun registerAll() {
        for (id in soundIds) {
            val soundId = identifier(id)
            val sound = Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                soundId,
                SoundEvent.createVariableRangeEvent(soundId)
            )
            registeredSounds[id] = sound

            when (id) {
                "bonk" -> BONK = sound
                "pop" -> POP = sound
                "click" -> CLICK = sound
                "uwu" -> UWU = sound
                "nya" -> NYA = sound
                "tung" -> TUNG = sound
                "meow" -> MEOW = sound
            }
        }
    }
}
