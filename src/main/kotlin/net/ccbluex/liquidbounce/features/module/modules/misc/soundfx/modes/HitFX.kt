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

    private enum class Hitsfx(
        override val choiceName: String,
    ) : NamedChoice {
        Bonk("Bonk"),
        Boykisser("Boykisser"),
        Bring("Bring"),
        Glass("Glass"),
        Click("Click"),
        Meow("Meow"),
        Moan("Moan"),
        MagicSquash("MagicSquash"),
        Nya("NYA"),
        Pop("Pop"),
        Soft("Soft"),
        Squash("Squash"),
        Tung("Tung"),
        Uwu("UWU"),
    }

    private val hitSound: SoundEvent
        get() =
            when (hitSFX) {
                // --- without variants ---
                Hitsfx.Bonk -> Sounds.get(Sounds.SoundKey.BONK)
                Hitsfx.Bring -> Sounds.get(Sounds.SoundKey.BRING)
                Hitsfx.Meow -> Sounds.get(Sounds.SoundKey.MEOW)
                Hitsfx.MagicSquash -> Sounds.get(Sounds.SoundKey.MAGICSQUASH)
                Hitsfx.Nya -> Sounds.get(Sounds.SoundKey.NYA)
                Hitsfx.Pop -> Sounds.get(Sounds.SoundKey.POP)
                Hitsfx.Soft -> Sounds.get(Sounds.SoundKey.SOFT)
                Hitsfx.Squash -> Sounds.get(Sounds.SoundKey.SQUASH)
                Hitsfx.Tung -> Sounds.get(Sounds.SoundKey.TUNG)
                Hitsfx.Uwu -> Sounds.get(Sounds.SoundKey.UWU)

                // --- with variants ---
                Hitsfx.Boykisser -> Sounds.get(Sounds.SoundKey.BOYKISSER)
                Hitsfx.Click -> Sounds.get(Sounds.SoundKey.CLICK)
                Hitsfx.Glass -> Sounds.get(Sounds.SoundKey.GLASS)
                Hitsfx.Moan -> Sounds.get(Sounds.SoundKey.MOAN)
            }

    fun hitSound(): SoundEvent? {
        if (!enabled) return null
        return hitSound
    }
}
