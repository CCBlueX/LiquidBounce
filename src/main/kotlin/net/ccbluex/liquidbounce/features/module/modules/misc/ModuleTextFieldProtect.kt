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

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.fastutil.objectLinkedSetOf
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.repeat
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.OrderedText
import java.util.function.BiFunction

/**
 * TextFieldProtect Module
 *
 * Hides rendered text of text field widget when it matches certain patterns.
 */
object ModuleTextFieldProtect : ClientModule("TextFieldProtect", Category.MISC) {

    private val patterns by regexList("Patterns",
        objectLinkedSetOf(
            Regex("^/register.*"),
            Regex("^/login.*"),
            Regex("^/email.*"),
        ),
    )

    private const val MASK_CHAR = '*'

    fun getWrappedRenderTextProvider(
        textFieldWidget: TextFieldWidget,
        original: BiFunction<String, Int, OrderedText>,
    ): BiFunction<String, Int, OrderedText> {
        if (!running) return original

        return BiFunction<String, Int, OrderedText> { t, u ->
            val fullText = textFieldWidget.text

            val wrapped = if (patterns.none { it.matches(fullText) }) {
                fullText
            } else {
                MASK_CHAR.repeat(t.length)
            }

            original.apply(wrapped, u)
        }
    }

}
