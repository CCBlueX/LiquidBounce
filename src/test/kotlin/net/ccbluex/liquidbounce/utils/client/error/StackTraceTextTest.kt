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
package net.ccbluex.liquidbounce.utils.client.error

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StackTraceTextTest {

    @Test
    fun `renders the trace the way the JDK prints it`() {
        val error = RuntimeException("boom").apply {
            stackTrace = arrayOf(
                StackTraceElement("net.ccbluex.liquidbounce.Foo", "bar", "Foo.kt", 12),
                StackTraceElement(null, "java.base", null, "java.lang.Thread", "run", null, -1),
            )
        }

        assertEquals(
            """
                java.lang.RuntimeException: boom
                  at net.ccbluex.liquidbounce.Foo.bar(Foo.kt:12)
                  at java.base/java.lang.Thread.run(Unknown Source)
            """.trimIndent(),
            error.stackTraceText().string,
        )
    }

    @Test
    fun `indents a suppressed exception and precedes the cause`() {
        val error = RuntimeException("outer").apply {
            stackTrace = emptyArray()
            addSuppressed(IllegalStateException("suppressed").apply { stackTrace = emptyArray() })
            initCause(IllegalArgumentException("cause").apply { stackTrace = emptyArray() })
        }

        assertEquals(
            """
                java.lang.RuntimeException: outer
                  Suppressed: java.lang.IllegalStateException: suppressed
                Caused by: java.lang.IllegalArgumentException: cause
            """.trimIndent(),
            error.stackTraceText().string,
        )
    }

    @Test
    fun `colors the throwable, our own frames and their source`() {
        val error = RuntimeException("boom").apply {
            stackTrace = arrayOf(StackTraceElement("net.ccbluex.liquidbounce.Foo", "bar", "Foo.kt", 42))
        }
        val leaves = error.stackTraceText().leaves()

        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), leaves.colorOf("java.lang.RuntimeException"))
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), leaves.colorOf(": boom"))
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE), leaves.colorOf("net.ccbluex.liquidbounce.Foo"))
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), leaves.colorOf("bar"))
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), leaves.colorOf("Foo.kt"))
        assertTrue(leaves.single { it.first == "42" }.second.isBold)
    }

}

private fun Component.leaves(): List<Pair<String, Style>> = buildList {
    if (siblings.isEmpty()) {
        add(string to style)
    } else {
        siblings.forEach { addAll(it.leaves()) }
    }
}

private fun List<Pair<String, Style>>.colorOf(text: String): TextColor? =
    single { it.first == text }.second.color
