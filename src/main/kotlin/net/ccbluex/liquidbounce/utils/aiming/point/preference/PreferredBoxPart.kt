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
 */

package net.ccbluex.liquidbounce.utils.aiming.point.preference

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.minecraft.util.math.Box

enum class PreferredBoxPart(override val choiceName: String, val cutOff: (Box) -> Double) : NamedChoice {
    HEAD("Head", { box -> box.maxY }),
    BODY("Body", { box -> box.center.y }),
    FEET("Feet", { box -> box.minY });

    /**
     * Check if this part of the box is higher than the other by the index of the enum.
     * So please DO NOT change the order of the enum.
     */
    fun isHigherThan(other: PreferredBoxPart) = entries.indexOf(this) < entries.indexOf(other)

}
