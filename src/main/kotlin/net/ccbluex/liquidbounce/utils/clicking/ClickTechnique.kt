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
package net.ccbluex.liquidbounce.utils.clicking

import net.ccbluex.liquidbounce.config.types.list.Tagged
import java.util.EnumSet

enum class ClickTechnique(override val tag: String) : Tagged {
    HUMAN("Human"),

    /**
     * Evenly spaced at the top of the CPS range, for anticheats that only look at the time since the last attack.
     */
    CONSTANT("Constant"),

    /**
     * Nothing is scheduled ahead; a clicker offering this decides each tick in [Clicker.getClickAmount].
     */
    AI("AI");

    companion object {
        val SCHEDULED: Set<ClickTechnique> = EnumSet.complementOf(EnumSet.of(AI))
    }
}
