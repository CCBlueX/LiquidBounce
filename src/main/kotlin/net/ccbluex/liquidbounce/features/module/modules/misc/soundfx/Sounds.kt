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
        // --- other ---
        "bonk", "pop", "uwu", "nya", "tung", "meow",
        // --- click ---
        "click-1", "click-2", "click-3",
        // --- boykisser ---
        "boykisser-1", "boykisser-2", "boykisser-3", "boykisser-4", "boykisser-5", "boykisser-6",
    )

    val registeredSounds = mutableMapOf<String, SoundEvent>()

    // --- other ---
    lateinit var BONK: SoundEvent private set; lateinit var POP: SoundEvent private set
    lateinit var UWU: SoundEvent private set; lateinit var NYA: SoundEvent private set
    lateinit var TUNG: SoundEvent private set; lateinit var MEOW: SoundEvent private set

    // --- click ---
    lateinit var CLICK1: SoundEvent private set; lateinit var CLICK2: SoundEvent private set
    lateinit var CLICK3: SoundEvent private set

    // --- boykisser ---
    lateinit var BOYKISSER1: SoundEvent private set; lateinit var BOYKISSER2: SoundEvent private set
    lateinit var BOYKISSER3: SoundEvent private set; lateinit var BOYKISSER4: SoundEvent private set
    lateinit var BOYKISSER5: SoundEvent private set; lateinit var BOYKISSER6: SoundEvent private set

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
                // --- other ---
                "bonk" -> BONK = sound
                "pop" -> POP = sound
                "uwu" -> UWU = sound
                "nya" -> NYA = sound
                "tung" -> TUNG = sound
                "meow" -> MEOW = sound

                // --- click ---
                "click-1" -> CLICK1 = sound; "click-2" -> CLICK2 = sound; "click-3" -> CLICK3 = sound

                // --- boykisser ---
                "boykisser-1" -> BOYKISSER1 = sound; "boykisser-2" -> BOYKISSER2 = sound
                "boykisser-3" -> BOYKISSER3 = sound; "boykisser-4" -> BOYKISSER4 = sound
                "boykisser-5" -> BOYKISSER5 = sound; "boykisser-6" -> BOYKISSER6 = sound
            }
        }
    }
}
