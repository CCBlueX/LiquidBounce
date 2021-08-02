/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.text.CharacterVisitor
import net.minecraft.text.OrderedText
import net.minecraft.text.Style
import net.minecraft.util.Formatting

/**
 * NameProtect module
 *
 * Changes players names clientside.
 */

object ModuleNameProtect : Module("NameProtect", Category.MISC) {

    val replacement by text("Replacement", "You")
    val replaceFriendNames by boolean("ObfuscateFriends", true)

    val replacements = ArrayList<ReplacementMapping>()

    val renderEventHandler = handler<GameRenderEvent> {
        replacements.clear()

        replacements.add(ReplacementMapping(mc.session.username, this.replacement))

        // Prevent shorter names being replaced before longer names
        replacements.sortByDescending { it.originalName.length }
    }

    fun replace(original: String): String {
        if (!enabled) {
            return original
        }

        val output = StringBuilder()

        val chars = original.toCharArray()

        var wasParagraph = false

        var index = 0

        while (index < chars.size) {
            val c = chars[index++]

            if (c == '§') {
                wasParagraph = true
                output.append(c)

                continue
            }
            if (wasParagraph) {
                wasParagraph = false
                output.append(c)

                continue
            }

            var found = false

            for (replacement in replacements) {
                val commonLength = getCommonLength(replacement, chars, index - 1)

                if (commonLength != -1) {
                    index += commonLength - 1

                    output.append(replacement.replacement)
                    found = true

                    break
                }
            }

            if (!found) output.append(c)
        }

        return output.toString()
    }

    private fun getCommonLength(replacement: ReplacementMapping, chars: CharArray, index: Int): Int {
        val replacementChars = replacement.originalName.toCharArray()
        var charsIndex = index
        var replacementCharsIndex = 0

        var wasParagraph = false

        while (replacementCharsIndex < replacementChars.size) {
            if (charsIndex > chars.lastIndex) {
                return -1
            }

            val c = chars[charsIndex++]

            if (c == '§') {
                wasParagraph = true
                continue
            }
            if (wasParagraph) {
                wasParagraph = false
                continue
            }

            if (c != replacementChars[replacementCharsIndex++]) return -1
        }

        return charsIndex - index
    }

    class ReplacementMapping(val originalName: String, val replacement: String)

    class NameProtectOrderedText(original: OrderedText) : OrderedText {
        private val mappedCharacters = ArrayList<MappedCharacter>()

        init {
            val originalCharacters = ArrayList<MappedCharacter>()

            original.accept { _, style, codePoint ->
                originalCharacters.add(MappedCharacter(style, codePoint))

                true
            }

            var index = 0

            while (index < originalCharacters.size) {
                val originalChar = originalCharacters[index]

                run {
                    for (replacement in replacements) {
                        // Empty names would cause undefined behaviour
                        if (replacement.originalName.isEmpty()) {
                            continue
                        }

                        var canReplace = true

                        for ((replacementIdx, c) in replacement.originalName.toCharArray().withIndex()) {
                            val origIndex = index + replacementIdx

                            if (originalCharacters.lastIndex < origIndex || originalCharacters[origIndex].codePoint != c.toInt()) {
                                canReplace = false
                                break
                            }
                        }

                        if (canReplace) {
                            this.mappedCharacters.addAll(
                                replacement.replacement.map {
                                    MappedCharacter(
                                        originalChar.style.withColor(
                                            Formatting.RED
                                        ),
                                        it.toInt()
                                    )
                                }
                            )
                            index += replacement.originalName.length
                            return@run
                        }
                    }

                    this.mappedCharacters.add(originalChar)

                    index++
                }
            }

        }

        override fun accept(visitor: CharacterVisitor): Boolean {
            var index = 0

            for ((style, codePoint) in this.mappedCharacters) {
                if (!visitor.accept(index, style, codePoint)) {
                    return false
                }

                index++
            }

            return true
        }

        data class MappedCharacter(val style: Style, val codePoint: Int)
    }
}
