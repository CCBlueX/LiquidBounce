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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.lang.translation
import net.minecraft.network.chat.MutableComponent
import net.ccbluex.liquidbounce.utils.client.highlight as highlightText
import net.ccbluex.liquidbounce.utils.client.markAsError as errorText
import net.ccbluex.liquidbounce.utils.client.regular as regularText
import net.ccbluex.liquidbounce.utils.client.variable as variableText
import net.ccbluex.liquidbounce.utils.client.warning as warningText

/**
 * Text in the client's colours, for [Chat.print] and command output. Chain `.append(...)` for mixed lines.
 */
object Text {

    @JvmStatic
    fun regular(text: String): MutableComponent = regularText(text)

    /** A value inside a sentence. */
    @JvmStatic
    fun variable(text: String): MutableComponent = variableText(text)

    @JvmStatic
    fun highlight(text: String): MutableComponent = highlightText(text)

    @JvmStatic
    fun warning(text: String): MutableComponent = warningText(text)

    @JvmStatic
    fun error(text: String): MutableComponent = errorText(text)

    /**
     * A key from the client's or an add-on's language files, resolved when shown.
     */
    @JvmStatic
    fun translate(key: String, vararg args: Any?): MutableComponent = translation(key, *args)

}
