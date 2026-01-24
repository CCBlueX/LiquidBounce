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

package net.ccbluex.liquidbounce.render.engine.font.processor

import it.unimi.dsi.fastutil.ints.IntList
import net.ccbluex.liquidbounce.render.engine.type.Color4b

interface ProcessedText {
    val chars: List<ProcessedChar>

    /**
     * Elements: start char index, to char index, ...
     *
     * Size should be even,
     */
    val underlines: IntList

    /**
     * Elements: start char index, to char index, ...
     *
     * Size should be even,
     */
    val strikeThroughs: IntList

    @JvmRecord
    data class ProcessedChar(val char: Char, val font: Int, val obfuscated: Boolean, val color: Color4b)

}
