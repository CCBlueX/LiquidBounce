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
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.ccbluex.liquidbounce.utils.text.asFormattedCharSequence
import net.ccbluex.liquidbounce.utils.text.codePointsToString
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
     * Builds the plain text once for matching, then defers the substitution to [withReplacements], so
     * a text without a match is returned as is instead of being copied character by character.
     */
    private fun uncachedWrap(original: FormattedCharSequence, useCache: Boolean): FormattedCharSequence {
        val text = original.codePointsToString()

        val replacements = if (useCache) {
            replacementMappings.findReplacementsCached(text)
        } else {
            replacementMappings.findReplacements(text)
        }

        if (replacements.isEmpty()) {
            return original
        }

        return original.withReplacements(replacements)
    }

}


/**
 * Sanitizes texts which are sent to the client.
 * 1. Degenerates legacy formatting into new formatting [StringDecomposer]
 * 2. Applies [ModuleNameProtect] - if needed
 */
fun Component.sanitizeForeignInput(): FormattedCharSequence =
    ModuleNameProtect.wrap(this.asFormattedCharSequence())

/**
 * Applies [ModuleNameProtect] - if needed
 */
inline fun FormattedCharSequence.sanitizeForeignInput(): FormattedCharSequence =
    ModuleNameProtect.wrap(this)

/**
 * Returns a sequence that substitutes each match of [replacements] as it is accepted, without
 * copying the characters or the styles of the receiver.
 */
internal fun FormattedCharSequence.withReplacements(replacements: Replacements): FormattedCharSequence =
    ReplacedSequence(this, replacements)

/**
 * A [FormattedCharSequence] that substitutes the matches of [replacements] while it is accepted.
 *
 * The indices of [org.ahocorasick.trie.Emit] count UTF-16 code units of the plain text built from
 * the receiver, which is not the index the sink is handed: that one restarts per style part and
 * skips the legacy formatting codes, so positions are accumulated with `Character.charCount`.
 *
 * The instance is its own [FormattedCharSink], which keeps an acceptance free of allocations. The
 * state lives in fields, so it must neither be accepted reentrantly nor from several threads.
 */
private class ReplacedSequence(
    private val original: FormattedCharSequence,
    private val replacements: Replacements,
) : FormattedCharSequence, FormattedCharSink {

    // Position in the original text, sharing the index space of the emits
    private var sourceIndex = 0
    // Position in the replaced text, which is the index the sink has to see
    private var outputIndex = 0
    private var replacementIndex = 0
    // Last character of the match whose substitution was already emitted
    private var substitutedUntil = -1
    private lateinit var sink: FormattedCharSink

    override fun accept(output: FormattedCharSink): Boolean {
        sourceIndex = 0
        outputIndex = 0
        replacementIndex = 0
        substitutedUntil = -1
        sink = output

        return original.accept(this)
    }

    override fun accept(index: Int, style: Style, codePoint: Int): Boolean {
        val charCount = Character.charCount(codePoint)

        if (sourceIndex <= substitutedUntil) {
            // The substitution took this character's place
            sourceIndex += charCount

            return true
        }

        var replacement = replacements.getOrNull(replacementIndex)

        // Drop the matches that end before this character
        while (replacement != null && sourceIndex > replacement.first.end) {
            replacementIndex++
            replacement = replacements.getOrNull(replacementIndex)
        }

        if (replacement != null && sourceIndex == replacement.first.start &&
            style.color?.bypassesNameProtection != true
        ) {
            substitutedUntil = replacement.first.end
            replacementIndex++

            // Every character of the substitution shares the style of the name it replaces
            val replacedStyle = style.withColor(replacement.second.colorGetter().argb)
            val newName = replacement.second.newName
            var nameIndex = 0

            while (nameIndex < newName.length) {
                val nameCodePoint = newName.codePointAt(nameIndex)
                val nameCharCount = Character.charCount(nameCodePoint)

                if (!sink.accept(outputIndex, replacedStyle, nameCodePoint)) {
                    return false
                }

                outputIndex += nameCharCount
                nameIndex += nameCharCount
            }

            sourceIndex += charCount

            return true
        }

        if (!sink.accept(outputIndex, style, codePoint)) {
            return false
        }

        outputIndex += charCount
        sourceIndex += charCount

        return true
    }

}
