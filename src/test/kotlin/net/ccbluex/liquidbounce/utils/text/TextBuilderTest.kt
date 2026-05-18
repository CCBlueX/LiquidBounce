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

package net.ccbluex.liquidbounce.utils.text

import net.minecraft.ChatFormatting
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextBuilderTest {

    @Test
    fun `test empty builder`() {
        val text = TextBuilder().build()

        assertEquals("", text.string)
    }

    @Test
    fun `test initial component`() {
        val text = TextBuilder("Hello".asPlainText()).build()

        assertEquals("Hello", text.string)
    }

    @Test
    fun `test null append is ignored`() {
        val text = TextBuilder()
            .append(null)
            .append("Hello".asPlainText())
            .append(null)
            .append(" World".asPlainText())
            .build()

        assertEquals("Hello World", text.string)
    }

    @Test
    fun `test multiple appends keep order`() {
        val text = TextBuilder()
            .append("Liquid".asPlainText(ChatFormatting.BLUE))
            .append("Bounce".asPlainText(ChatFormatting.AQUA))
            .append(" Client".asPlainText(ChatFormatting.GRAY))
            .build()

        assertEquals("LiquidBounce Client", text.string)
    }

}
