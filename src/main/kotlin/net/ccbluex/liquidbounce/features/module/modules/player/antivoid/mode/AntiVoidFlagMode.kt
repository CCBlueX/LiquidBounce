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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid

object AntiVoidFlagMode : AntiVoidMode("Flag") {

    private val fallDistance by float("FallDistance", 0.5f, 0.0f..6.0f)

    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiVoid.mode

    override fun rescue(): Boolean {
        if (player.fallDistance >= fallDistance) {
            player.setPosition(player.pos.add(0.0, 0.42, 0.0))
            return true
        }

        return false
    }

}
