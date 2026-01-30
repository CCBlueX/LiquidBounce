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
import net.ccbluex.fastutil.LfuCache
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
import net.ccbluex.liquidbounce.render.engine.font.processor.LegacyTextSanitizer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.bypassesNameProtection
import net.ccbluex.liquidbounce.utils.client.toText
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.FormattedCharSink

private const val DEFAULT_CACHE_SIZE = 512

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

    private val stringMappingCache =
        LfuCache<String, String>(DEFAULT_CACHE_SIZE)
    private val orderedTextMappingCache =
        LfuCache<FormattedCharSequence, WrappedOrderedText>(DEFAULT_CACHE_SIZE) { _, v ->
            mappedCharListPool.recycle(v.mappedCharacters)
        }
    private val mappedCharListPool = Pool(
        initializer = { ObjectArrayList(128) },
        finalizer = ObjectArrayList<MappedCharacter>::clear,
    ).synchronized()

    fun replace(original: String): String =
        when {
            !running -> original
            mc.isSameThread -> stringMappingCache.getOrPut(original) { uncachedReplace(original) }
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

    fun wrap(original: FormattedCharSequence): FormattedCharSequence =
        when {
            !running -> original
            mc.isSameThread -> orderedTextMappingCache.getOrPut(original) { uncachedWrap(original) }
            else -> uncachedWrap(original)
        }

    /**
     * Wraps an [FormattedCharSequence] to apply name protection.
     */
    private fun uncachedWrap(original: FormattedCharSequence): WrappedOrderedText {
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
            for (c in originalCharacters) {
                it.appendCodePoint(c.codePoint)
            }
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
                        originalCharacters[currentIndex].style.withColor(color.argb),
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

    private class WrappedOrderedText(@JvmField val mappedCharacters: ObjectArrayList<MappedCharacter>) :
        FormattedCharSequence {
        override fun accept(visitor: FormattedCharSink): Boolean {
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
fun Component.sanitizeForeignInput(): Component {
    val degeneratedText = LegacyTextSanitizer.SanitizedLegacyText(this)

    if (!ModuleNameProtect.running) {
        return degeneratedText.toText()
    }

    return ModuleNameProtect.wrap(degeneratedText).toText()
}
