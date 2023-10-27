/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.minecraft.text.CharacterVisitor
import net.minecraft.text.OrderedText
import net.minecraft.text.Style
import net.minecraft.text.TextColor

/**
 * NameProtect module
 *
 * Changes players names clientside.
 */

object ModuleNameProtect : Module("NameProtect", Category.MISC) {

    val replacement by text("Replacement", "You")

    val color by color("Color", Color4b.WHITE)
    val colorRainbow by boolean("Rainbow", false)

    object ReplaceFriendNames : ToggleableConfigurable(this, "ObfuscateFriends", true) {
        val color by color("Color", Color4b(255, 179, 72, 255))
        val colorRainbow by boolean("Rainbow", false)
    }

    init {
        tree(ReplaceFriendNames)

        // Entirely keep out from public config
        doNotInclude()
    }

    val replacements = ArrayList<ReplacementMapping>()

    val renderEventHandler = handler<GameRenderEvent> {
        replacements.clear()

        if (ReplaceFriendNames.enabled) {
            FriendManager.friends.withIndex().forEach { (id, friend) ->
                val color4b = if (ReplaceFriendNames.colorRainbow) rainbow() else ReplaceFriendNames.color

                replacements.add(ReplacementMapping(friend.name, "Friend $id", color4b))
            }
        }

        val color4b = if (colorRainbow) rainbow() else color

        replacements.add(ReplacementMapping(mc.session.username, this.replacement, color4b))

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

    class ReplacementMapping(val originalName: String, val replacement: String, val color4b: Color4b)

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

                            if (originalCharacters.lastIndex < origIndex || originalCharacters[origIndex].codePoint != c.code) {
                                canReplace = false
                                break
                            }
                        }

                        if (canReplace) {
                            this.mappedCharacters.addAll(replacement.replacement.map {
                                MappedCharacter(
                                    originalChar.style.withColor(TextColor.parse(replacement.color4b.toHex())), it.code
                                )
                            })
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
