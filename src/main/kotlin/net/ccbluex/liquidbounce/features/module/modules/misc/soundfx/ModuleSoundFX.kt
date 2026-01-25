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

import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.misc.soundfx.modes.HitFX
import net.ccbluex.liquidbounce.features.module.modules.misc.soundfx.modes.HitFX.hitSound

object ModuleSoundFX : ClientModule("SoundFX", ModuleCategories.MISC) {

    private val volume by float("volume", 1f, 0.1f..1f)

    init {
        tree(HitFX)
    }

    @Suppress("unused")
    private val hitHandler = handler<AttackEntityEvent> { event ->
        val sound = hitSound ?: return@handler

        if (event.entity.isAlive) {
            player.playSound(sound, volume, 1f)
        }
    }
}
