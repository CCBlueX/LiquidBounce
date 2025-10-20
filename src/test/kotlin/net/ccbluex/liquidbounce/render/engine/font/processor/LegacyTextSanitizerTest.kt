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
 */

package net.ccbluex.liquidbounce.render.engine.font.processor

import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.minecraft.text.StringVisitable.StyledVisitor
import net.minecraft.text.Style
import net.minecraft.text.Text
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.collections.ArrayList

class LegacyTextSanitizerTest {

    @Test
    fun test() {
        assertEquals(listOf("This is a Test!" to Style.EMPTY), getResults("This is a Test!".asPlainText()))
    }

    private fun getResults(text: Text): ArrayList<Pair<String, Style>> {
        val visitor = TestVisitor()

        text.visit(visitor, Style.EMPTY)

        return visitor.contents
    }

    private class TestVisitor : StyledVisitor<Unit> {
        val contents = ArrayList<Pair<String, Style>>()

        override fun accept(style: Style, asString: String): Optional<Unit> {
            contents.add(asString to style)

            return Optional.empty()
        }
    }
}
