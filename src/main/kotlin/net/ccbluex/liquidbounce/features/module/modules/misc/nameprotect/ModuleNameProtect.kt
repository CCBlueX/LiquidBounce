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
package net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.fastutil.Pool
import net.ccbluex.fastutil.Pool.Companion.use
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.GenericColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.engine.font.processor.LegacyTextSanitizer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.bypassesNameProtection
import net.ccbluex.liquidbounce.utils.client.toText
import net.ccbluex.liquidbounce.utils.collection.LfuCache
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.minecraft.text.CharacterVisitor
import net.minecraft.text.OrderedText
import net.minecraft.text.Style
import net.minecraft.text.Text

private const val DEFAULT_CACHE_SIZE = 512

/**
 * NameProtect module
 *
 * Changes players names clientside.
 */

object ModuleNameProtect : ClientModule("NameProtect", Category.MISC) {

    private val replacement by text("Replacement", "You")

    private val colorMode = choices<GenericColorMode<Unit>>(
        "ColorMode",
        0,
        {
            arrayOf(GenericStaticColorMode(it, Color4b(255, 179, 72, 50)), GenericRainbowColorMode(it))
        }
    )

    private object ReplaceFriendNames : ToggleableConfigurable(this, "ObfuscateFriends", true) {
        val colorMode = choices<GenericColorMode<Unit>>(
            ReplaceFriendNames,
            "ColorMode",
            0
        ) {
            arrayOf(GenericStaticColorMode(it, Color4b(0, 241, 255)), GenericRainbowColorMode(it))
        }
    }

    private object ReplaceOthers : ToggleableConfigurable(this, "ObfuscateOthers", false) {
        val colorMode = choices<GenericColorMode<Unit>>(
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
        username = { this.colorMode.activeChoice.getColor(Unit) },
        friends = { ReplaceFriendNames.colorMode.activeChoice.getColor(Unit) },
        otherPlayers = { ReplaceOthers.colorMode.activeChoice.getColor(Unit) },
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

        val playerName = player.gameProfile?.name ?: mc.session.username

        val otherPlayers = if (ReplaceOthers.enabled) {
            network.playerList?.mapNotNull { playerListEntry ->
                val otherName = playerListEntry?.profile?.name

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

    private val stringMappingCache = LfuCache<String, String>(DEFAULT_CACHE_SIZE)
    private val orderedTextMappingCache = LfuCache<OrderedText, WrappedOrderedText>(DEFAULT_CACHE_SIZE) { _, v ->
        mappedCharListPool.recycle(v.mappedCharacters)
    }
    private val mappedCharListPool = Pool(
        initializer = ::ObjectArrayList,
        finalizer = ObjectArrayList<MappedCharacter>::clear,
    ).synchronized()

    fun replace(original: String): String =
        when {
            !running -> original
            mc.isOnThread -> stringMappingCache.getOrPut(original) { uncachedReplace(original) }
            else -> uncachedReplace(original)
        }

    private fun uncachedReplace(original: String): String {
        val replacements = replacementMappings.findReplacements(original)

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

    fun wrap(original: OrderedText): OrderedText =
        when {
            !running -> original
            mc.isOnThread -> orderedTextMappingCache.getOrPut(original) { uncachedWrap(original) }
            else -> uncachedWrap(original)
        }

    /**
     * Wraps an [OrderedText] to apply name protection.
     */
    private fun uncachedWrap(original: OrderedText): WrappedOrderedText {
        val mappedCharacters = mappedCharListPool.borrow()

        val originalCharacters = mappedCharListPool.borrow()

        original.accept { _, style, codePoint ->
            originalCharacters += MappedCharacter(
                style,
                style.color?.bypassesNameProtection ?: false,
                codePoint
            )

            true
        }

        val replacements = Pools.StringBuilder.use {
            it.ensureCapacity(originalCharacters.size)
            originalCharacters.forEach { c -> it.appendCodePoint(c.codePoint) }
            replacementMappings.findReplacements(it)
        }

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

                mappedCharacters.ensureCapacity(mappedCharacters.size + replacement.second.newName.length)
                replacement.second.newName.mapTo(mappedCharacters) { ch ->
                    MappedCharacter(
                        originalCharacters[currentIndex].style.withColor(color.toARGB()),
                        false,
                        ch.code
                    )
                }

                currentIndex = replacement.first.end + 1
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

    private class WrappedOrderedText(@JvmField val mappedCharacters: ObjectArrayList<MappedCharacter>) : OrderedText {
        override fun accept(visitor: CharacterVisitor): Boolean {
            for (index in 0 until mappedCharacters.size) {
                val char = mappedCharacters[index] as MappedCharacter
                if (!visitor.accept(index, char.style, char.codePoint)) {
                    return false
                }
            }

            return true
        }
    }
}

/**
 * Sanitizes texts which are sent to the client.
 * 1. Degenerates legacy formatting into new formatting [LegacyTextSanitizer]
 * 2. Applies [ModuleNameProtect] - if needed
 */
fun Text.sanitizeForeignInput(): Text {
    val degeneratedText = LegacyTextSanitizer.SanitizedLegacyText(this)

    if (!ModuleNameProtect.running) {
        return degeneratedText.toText()
    }

    return ModuleNameProtect.wrap(degeneratedText).toText()
}
