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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.sounds.SoundEvent

object ModuleSoundFX : ClientModule("SoundFX", ModuleCategories.MISC) {

    private val volume by float("volume", 1f, 0.1f..1f)

    // --- SFX ---
    private val HitSFX by enumChoice("Hit", Hitsfx.Bonk)

    private enum class Hitsfx(override val choiceName: String) : NamedChoice {
        Bonk("Bonk"), Boykisser("Boykisser"),
        Click("Click"), Meow("Meow"),
        Nya("NYA"), Pop("Pop"),
        Tung("Tung"), Uwu("UWU"),
    }

    private val BoykisserVariants = arrayOf(
        Sounds.BOYKISSER1, Sounds.BOYKISSER2, Sounds.BOYKISSER3,
        Sounds.BOYKISSER4, Sounds.BOYKISSER5, Sounds.BOYKISSER6,
        )

    private val ClickVariants = arrayOf(
        Sounds.CLICK1, Sounds.CLICK2, Sounds.CLICK3,
    )

    private val HitSound: SoundEvent
        get() = when (HitSFX) {
            // --- without variants ---
            Hitsfx.Bonk -> Sounds.BONK
            Hitsfx.Meow -> Sounds.MEOW
            Hitsfx.Nya -> Sounds.NYA
            Hitsfx.Pop -> Sounds.POP
            Hitsfx.Tung -> Sounds.TUNG
            Hitsfx.Uwu -> Sounds.UWU

            // --- with variants ---
            Hitsfx.Click -> ClickVariants.random()
            Hitsfx.Boykisser -> BoykisserVariants.random()
        }
    // --- Play sound ---
    @Suppress("unused")
    private val hitHandler = handler<AttackEntityEvent> { event ->
        val player = mc.player ?: return@handler
        if(event.entity.isAlive) {
            player.playSound(HitSound, volume, 1f)
        }
    }
}
