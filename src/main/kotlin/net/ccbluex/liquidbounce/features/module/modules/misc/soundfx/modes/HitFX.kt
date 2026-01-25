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

        private val hitSFX by enumChoice("Sound", Hitsfx.Soft)

    private enum class Hitsfx(
        override val choiceName: String,
        val soundKey: Sounds.SoundKey,
    ) : NamedChoice {
        Bonk("Bonk", Sounds.SoundKey.BONK),
        Boykisser("Boykisser", Sounds.SoundKey.BOYKISSER),
        Bring("Bring", Sounds.SoundKey.BRING),
        Glass("Glass", Sounds.SoundKey.GLASS),
        Click("Click", Sounds.SoundKey.CLICK),
        Meow("Meow", Sounds.SoundKey.MEOW),
        Moan("Moan", Sounds.SoundKey.MOAN),
        MagicSquash("MagicSquash", Sounds.SoundKey.MAGICSQUASH),
        Nya("NYA", Sounds.SoundKey.NYA),
        Pop("Pop", Sounds.SoundKey.POP),
        Soft("Soft", Sounds.SoundKey.SOFT),
        Squash("Squash", Sounds.SoundKey.SQUASH),
        Tung("Tung", Sounds.SoundKey.TUNG),
        Uwu("UWU", Sounds.SoundKey.UWU),
    }

    private val hitSound: SoundEvent
        get() = Sounds.get(hitSFX.soundKey)

    fun hitSound(): SoundEvent? {
        if (!enabled) return null
        return hitSound
    }
}
