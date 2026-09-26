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
package net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.fastutil.Pool
import net.ccbluex.fastutil.Pool.Companion.use
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.GenericColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.bypassesNameProtection
import net.ccbluex.liquidbounce.utils.text.toText
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.FormattedCharSink
import net.minecraft.util.StringDecomposer

/**
 * NameProtect module
 *
 * Changes players names clientside.
 */

object ModuleNameProtect : ClientModule("NameProtect", ModuleCategories.MISC) {

    private val replacement by text("Replacement", "You")

    private val colorMode = choices<GenericColorMode<Unit>>(
        "ColorMode",
        0
    ) {
        arrayOf(GenericStaticColorMode(it, Color4b(255, 179, 72, 50)), GenericRainbowColorMode(it))
    }

    private object ReplaceFriendNames : ToggleableValueGroup(this, "ObfuscateFriends", true) {
        val colorMode = modes<GenericColorMode<Unit>>(
            ReplaceFriendNames,
            "ColorMode",
            0
        ) {
            arrayOf(GenericStaticColorMode(it, Color4b(0, 241, 255)), GenericRainbowColorMode(it))
        }
    }

    private object ReplaceOthers : ToggleableValueGroup(this, "ObfuscateOthers", false) {
        val colorMode = modes<GenericColorMode<Unit>>(
            ReplaceOthers,
            "ColorMode",
            0
        ) {
            arrayOf(GenericStaticColorMode(it, Color4b(71, 71, 71)), GenericRainbowColorMode(it))
        }
    }

    init {
        tree(ReplaceFriendNames)
        tree(ReplaceOthers)

        // Entirely keep out from public config
        doNotIncludeAlways()
    }

    private val replacementMappings = NameProtectMappings()

    private val coloringInfo = NameProtectMappings.ColoringInfo(
        username = { this.colorMode.activeMode.getColor(Unit) },
        friends = { ReplaceFriendNames.colorMode.activeMode.getColor(Unit) },
        otherPlayers = { ReplaceOthers.colorMode.activeMode.getColor(Unit) },
    )

    @Suppress("unused")
    private val renderHandler = handler<GameTickEvent> {
        val friendMappings = if (ReplaceFriendNames.enabled) {
            FriendManager.friends.filter { it.name.isNotBlank() }.mapIndexed { id, friend ->
                friend.name to (friend.alias ?: friend.getDefaultName(id))
            }
        } else {
            emptyList()
        }

        val playerName = player.gameProfile.name ?: mc.user.name

        val otherPlayers = if (ReplaceOthers.enabled) {
            network.onlinePlayers.mapNotNull { playerListEntry ->
                val otherName = playerListEntry.profile.name

                if (otherName != playerName) otherName else null
            }
        } else { null } ?: emptyList()

        this.replacementMappings.update(
            playerName to this.replacement,
            friendMappings,
            otherPlayers,
            coloringInfo
        )
    }

    private val mappedCharListPool = Pool(
        initializer = { ObjectArrayList(128) },
        finalizer = ObjectArrayList<MappedCharacter>::clear,
    ).synchronized()

    fun replace(original: String): String =
        when {
            !running -> original
            mc.isSameThread -> applyReplacements(original, replacementMappings.findReplacementsCached(original))
            else -> applyReplacements(original, replacementMappings.findReplacements(original))
        }

    private fun applyReplacements(original: String, replacements: Replacements): String {
        if (replacements.isEmpty()) {
            return original
        }

        return Pools.buildStringPooled {
            var currReplacementIndex = 0
            var currentIndex = 0

            while (currentIndex < original.length) {
                val replacement = replacements.getOrNull(currReplacementIndex)

                val replacementStartIdx = replacement?.first?.start

                if (replacementStartIdx == currentIndex) {
                    append(replacement.second.newName)

                    currentIndex = replacement.first.end + 1
                    currReplacementIndex += 1
                } else {
                    val maxCopyIdx = replacementStartIdx ?: original.length

                    append(original, currentIndex, maxCopyIdx)

                    currentIndex = maxCopyIdx
                }
            }
        }
    }

    fun wrap(original: FormattedCharSequence): FormattedCharSequence =
        when {
            !running -> original
            mc.isSameThread -> uncachedWrap(original, useCache = true)
            else -> uncachedWrap(original, useCache = false)
        }

    /**
     * The collected characters are indexed by code point, while the indices reported by
     * [org.ahocorasick.trie.Emit] count UTF-16 code units. The two only diverge on text holding
     * surrogate pairs, so indices are translated with [codePointIndex].
     */
    private fun uncachedWrap(original: FormattedCharSequence, useCache: Boolean): FormattedCharSequence {
        val originalCharacters = mappedCharListPool.borrow()

        val text = Pools.StringBuilder.use { builder ->
            original.accept { _, style, codePoint ->
                builder.appendCodePoint(codePoint)
                originalCharacters += MappedCharacter(
                    style,
                    style.color?.bypassesNameProtection ?: false,
                    codePoint
                )

                true
            }

            builder.toString()
        }

        val replacements = if (useCache) {
            replacementMappings.findReplacementsCached(text)
        } else {
            replacementMappings.findReplacements(text)
        }

        if (replacements.isEmpty()) {
            mappedCharListPool.recycle(originalCharacters)

            return original
        }

        val mappedCharacters = mappedCharListPool.borrow()

        var currReplacementIndex = 0
        var currentIndex = 0

        while (currentIndex < originalCharacters.size) {
            val replacement = replacements.getOrNull(currReplacementIndex)

            val replacementStartIdx = replacement?.let { text.codePointIndex(it.first.start) }

            if (replacementStartIdx == currentIndex) {
                if (originalCharacters[replacementStartIdx].bypassesNameProtection) {
                    currReplacementIndex++

                    continue
                }

                val newName = replacement.second.newName

                // Every character of the replacement shares one style
                val style = originalCharacters[currentIndex].style.withColor(replacement.second.colorGetter().argb)

                mappedCharacters.ensureCapacity(mappedCharacters.size + newName.length)
                var nameIndex = 0
                while (nameIndex < newName.length) {
                    val codePoint = newName.codePointAt(nameIndex)
                    mappedCharacters += MappedCharacter(style, false, codePoint)
                    nameIndex += Character.charCount(codePoint)
                }

                currentIndex = text.codePointIndex(replacement.first.end + 1)
                currReplacementIndex += 1
            } else {
                val maxCopyIdx = replacementStartIdx ?: originalCharacters.size

                mappedCharacters.addAll(originalCharacters.subList(currentIndex, maxCopyIdx))

                currentIndex = maxCopyIdx
            }
        }

        mappedCharListPool.recycle(originalCharacters)

        return WrappedOrderedText(mappedCharacters)
    }

    private class MappedCharacter(
        @JvmField val style: Style,
        @JvmField val bypassesNameProtection: Boolean,
        @JvmField val codePoint: Int,
    )

    private class WrappedOrderedText(@JvmField val mappedCharacters: ObjectArrayList<MappedCharacter>) :
        FormattedCharSequence {
        override fun accept(visitor: FormattedCharSink): Boolean {
            var index = 0
            for (element in mappedCharacters) {
                if (!visitor.accept(index, element.style, element.codePoint)) {
                    return false
                }

                index += Character.charCount(element.codePoint)
            }

            return true
        }
    }
}

/**
 * Translates a UTF-16 index into this string to the index of the code point list built from it.
 *
 * The two differ by the number of surrogate pairs that end at or before [charIndex], so text
 * without surrogate pairs maps onto itself.
 */
internal fun String.codePointIndex(charIndex: Int): Int {
    var cursor = 0
    var pairs = 0
    while (cursor < charIndex) {
        val count = Character.charCount(codePointAt(cursor))
        if (count == 2) pairs++
        cursor += count
    }
    return charIndex - pairs
}

/**
 * Sanitizes texts which are sent to the client.
 * 1. Degenerates legacy formatting into new formatting [StringDecomposer]
 * 2. Applies [ModuleNameProtect] - if needed
 */
fun Component.sanitizeForeignInput(): Component {
    val degeneratedText = FormattedCharSequence { output ->
        StringDecomposer.iterateFormatted(this, Style.EMPTY, output)
    }

    if (!ModuleNameProtect.running) {
        return degeneratedText.toText()
    }

    return ModuleNameProtect.wrap(degeneratedText).toText()
}
