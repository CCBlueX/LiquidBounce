package net.ccbluex.liquidbounce.render.engine.font.processor

import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.text.StringVisitable
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
        assertEquals(listOf("This is a Test!" to Style.EMPTY), getResults("This is a Test!".asText()))
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
