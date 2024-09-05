package net.ccbluex.liquidbounce.render.engine.font.processor

import net.ccbluex.liquidbounce.render.engine.Color4b
import java.awt.Font

/**
 * Processor for legacy Minecraft text. (i.e. `§aHello, §bworld!`)
 */
class LegacyTextProcessor(
    val text: String,
    val defaultColor: Color4b,
    val obfuscationSeed: Long?
): TextProcessor(obfuscationSeed) {
    private var strikeThroughStart: Int? = null
    private var underlineStart: Int? = null

    private var obfuscated = false

    private var currentFont = 0

    private var color: Color4b = defaultColor

    private val underlines = ArrayList<IntRange>()
    private val strikeThroughs = ArrayList<IntRange>()

    override fun process(): ProcessedText {
        val chars = ArrayList<ProcessedTextCharacter>()

        // Was the last read character a §?
        var wasMagicChar = false

        for (c in text.toCharArray()) {
            val char = when {
                wasMagicChar -> {
                    processFormatCharacter(chars.size, c)

                    continue
                }
                c == '§' -> {
                    wasMagicChar = true

                    continue
                }
                this.obfuscated -> ProcessedTextCharacter(generateObfuscatedChar(), currentFont, true, color)
                else -> ProcessedTextCharacter(c, currentFont, false, color)
            }

            chars.add(char)
        }

        resetStyle(chars.size - 1)

        return ProcessedText(chars, underlines, strikeThroughs)
    }

    private fun pushUnderline(currCharIdx: Int) {
        this.underlineStart?.let {
            underlines.add(IntRange(it, currCharIdx))
            underlineStart = null
        }
    }

    private fun pushStriketrough(currCharIdx: Int, end: Boolean) {
        this.underlineStart?.let {
            underlines.add(IntRange(it, currCharIdx))
            underlineStart = if (end) null else currCharIdx
        }
    }

    private fun processFormatCharacter(outputIdx: Int, char: Char) {
        when (val colorIdx = getColorIndex(char)) {
            in 0..15 -> {
                this.color = hexColors[colorIdx]

                resetStyle(outputIdx)
            }

            16 -> obfuscated = true
            17 -> this.currentFont = this.currentFont or Font.BOLD
            18 -> strikeThroughStart = outputIdx
            19 -> underlineStart = outputIdx
            20 -> this.currentFont = this.currentFont or Font.ITALIC
            21 -> {
                this.color = defaultColor

                resetStyle(outputIdx)
            }
        }
    }

    private fun resetStyle(outputIdx: Int) {
        pushUnderline(outputIdx)
        pushStriketrough(outputIdx, end = true)

        this.obfuscated = false

        this.currentFont = 0
        obfuscated = false
    }

    companion object {
        private fun getColorIndex(type: Char): Int {
            return when (type) {
                in '0'..'9' -> type - '0'
                in 'a'..'f' -> type - 'a' + 10
                in 'k'..'o' -> type - 'k' + 16
                'r' -> 21
                else -> -1
            }
        }

    }
}
