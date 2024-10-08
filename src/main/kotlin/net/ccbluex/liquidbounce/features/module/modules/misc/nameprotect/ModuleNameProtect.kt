/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.GenericColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.bypassesNameProtection
import net.ccbluex.liquidbounce.utils.client.toText
import net.minecraft.text.CharacterVisitor
import net.minecraft.text.OrderedText
import net.minecraft.text.Style
import net.minecraft.text.Text

/**
 * NameProtect module
 *
 * Changes players names clientside.
 */

object ModuleNameProtect : Module("NameProtect", Category.MISC) {

    private val replacement by text("Replacement", "You")

    private val colorMode = choices<GenericColorMode<Unit>>(
        "ColorMode",
        { it.choices[0] },
        {
            arrayOf(GenericStaticColorMode(it, Color4b(255, 179, 72, 50)), GenericRainbowColorMode(it))
        }
    )

    private object ReplaceFriendNames : ToggleableConfigurable(this, "ObfuscateFriends", true) {
        val colorMode = choices<GenericColorMode<Unit>>(
            ReplaceFriendNames,
            "ColorMode",
            { it.choices[0] },
            {
                arrayOf(GenericStaticColorMode(it, Color4b(0, 241, 255)), GenericRainbowColorMode(it))
            }
        )
    }

    private object ReplaceOthers : ToggleableConfigurable(this, "ObfuscateOthers", false) {
        val colorMode = ReplaceOthers.choices<GenericColorMode<Unit>>(
            ReplaceOthers,
            "ColorMode",
            { it.choices[0] },
            {
                arrayOf(GenericStaticColorMode(it, Color4b(71, 71, 71)), GenericRainbowColorMode(it))
            }
        )
    }

    init {
        tree(ReplaceFriendNames)
        tree(ReplaceOthers)

        // Entirely keep out from public config
        doNotIncludeAlways()
    }

    private val replacementMappings = NameProtectMappings()

    private val coloringInfo = NameProtectMappings.ColoringInfo(
        username = { this.colorMode.activeChoice.getColor(Unit) },
        friends = { ReplaceFriendNames.colorMode.activeChoice.getColor(Unit) },
        otherPlayers = { ReplaceOthers.colorMode.activeChoice.getColor(Unit) },
    )

    @Suppress("unused")
    val renderEventHandler = handler<GameTickEvent> {
        val friendMappings = if (ReplaceFriendNames.enabled) {
            FriendManager.friends.mapIndexed { id, friend ->
                friend.name to (friend.alias ?: friend.getDefaultName(id))
            }
        } else {
            emptyList()
        }

        val playerName = player.gameProfile?.name

        val otherPlayers = mc.networkHandler?.playerList?.mapNotNull {
            val otherName = it?.profile?.name

            if (otherName != playerName) otherName else null
        } ?: emptyList()

        this.replacementMappings.update(
            mc.session.username to this.replacement,
            friendMappings,
            otherPlayers,
            coloringInfo
        )
    }

    fun replace(original: String): String {
        if (!enabled) {
            return original
        }

        val output = StringBuilder()

        val replacements = replacementMappings.findReplacements(original)

        if (replacements.isEmpty()) {
            return original
        }

        var currReplacementIndex = 0
        var currentIndex = 0

        while (currentIndex < original.length) {
            val replacement = replacements.getOrNull(currReplacementIndex)

            val replacementStartIdx = replacement?.first?.start

            if (replacementStartIdx == currentIndex) {
                output.append(replacement.second.newName)

                currentIndex = replacement.first.end + 1
                currReplacementIndex += 1
            } else {
                val maxCopyIdx = replacementStartIdx ?: original.length

                output.append(original.substring(currentIndex, maxCopyIdx))

                currentIndex = maxCopyIdx
            }
        }

        return output.toString()
    }

    class ReplacementMapping(val originalName: String, val replacement: String, val color4b: Color4b)

    class NameProtectOrderedText(original: OrderedText) : OrderedText {
        private val mappedCharacters = ArrayList<MappedCharacter>()

        init {
            val originalCharacters = ArrayList<MappedCharacter>()

            original.accept { _, style, codePoint ->
                originalCharacters.add(MappedCharacter(
                    style,
                    style.color?.let { it.bypassesNameProtection } ?: false,
                    codePoint
                ))

                true
            }

            val text = originalCharacters.joinToString(separator = "") { it.codePoint.toChar().toString() }
            val replacements = replacementMappings.findReplacements(text)

            var currReplacementIndex = 0
            var currentIndex = 0

            while (currentIndex < originalCharacters.size) {
                val replacement = replacements.getOrNull(currReplacementIndex)

                val replacementStartIdx = replacement?.first?.start

                if (replacementStartIdx == currentIndex) {
                    if (originalCharacters[replacementStartIdx].bypassesNameProtection) {
                        currReplacementIndex++

                        continue
                    }

                    val color = replacement.second.colorGetter()

                    replacement.second.newName.forEachIndexed { idx, ch ->
                        this.mappedCharacters.add(
                            MappedCharacter(
                                originalCharacters[currentIndex].style.withColor(color.toRGBA()),
                                false,
                                ch.code
                            )
                        )
                    }

                    currentIndex = replacement.first.end + 1
                    currReplacementIndex += 1
                } else {
                    val maxCopyIdx = replacementStartIdx ?: originalCharacters.size

                    this.mappedCharacters.addAll(originalCharacters.subList(currentIndex, maxCopyIdx))

                    currentIndex = maxCopyIdx
                }
            }
        }

        override fun accept(visitor: CharacterVisitor): Boolean {
            var index = 0

            for ((style, _, codePoint) in this.mappedCharacters) {
                if (!visitor.accept(index, style, codePoint)) {
                    return false
                }

                index++
            }

            return true
        }

        data class MappedCharacter(val style: Style, val bypassesNameProtection: Boolean, val codePoint: Int)
    }
}

fun Text.sanitizeWithNameProtect(): Text {
    if (!ModuleNameProtect.enabled) {
        return this
    }

    return ModuleNameProtect.NameProtectOrderedText(this.asOrderedText()).toText()
}
