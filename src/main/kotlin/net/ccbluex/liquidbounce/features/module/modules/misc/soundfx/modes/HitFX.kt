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

package net.ccbluex.liquidbounce.features.module.modules.misc.soundfx.modes

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.misc.soundfx.ModuleSoundFX
import net.ccbluex.liquidbounce.features.module.modules.misc.soundfx.Sounds
import net.minecraft.sounds.SoundEvent

object HitFX : ToggleableConfigurable(ModuleSoundFX, "HitFX", true) {

    private val hitSFX by enumChoice("Sound", Hitsfx.Bonk)

    private enum class Hitsfx(override val choiceName: String) : NamedChoice {
        Bonk("Bonk"), Boykisser("Boykisser"),
        Click("Click"), Meow("Meow"),
        Moan("Moan"), Nya("NYA"),
        Pop("Pop"), Tung("Tung"),
        Uwu("UWU"),
    }

    private val boykisserVariants = arrayOf(
        Sounds.BOYKISSER1, Sounds.BOYKISSER2, Sounds.BOYKISSER3,
        Sounds.BOYKISSER4, Sounds.BOYKISSER5, Sounds.BOYKISSER6,
    )

    private val clickVariants = arrayOf(
        Sounds.CLICK1, Sounds.CLICK2, Sounds.CLICK3,
    )

    private val moanVariants = arrayOf(
        Sounds.MOAN1, Sounds.MOAN2, Sounds.MOAN3,
        Sounds.MOAN4,
    )

    private val hitSound: SoundEvent
        get() = when (hitSFX) {
            // --- without variants ---
            Hitsfx.Bonk -> Sounds.BONK
            Hitsfx.Meow -> Sounds.MEOW
            Hitsfx.Nya -> Sounds.NYA
            Hitsfx.Pop -> Sounds.POP
            Hitsfx.Tung -> Sounds.TUNG
            Hitsfx.Uwu -> Sounds.UWU

            // --- with variants ---
            Hitsfx.Boykisser -> boykisserVariants.random()
            Hitsfx.Click -> clickVariants.random()
            Hitsfx.Moan -> moanVariants.random()
        }

    fun hitSound(): SoundEvent? {
        if (!enabled) return null
        return hitSound
    }

}
