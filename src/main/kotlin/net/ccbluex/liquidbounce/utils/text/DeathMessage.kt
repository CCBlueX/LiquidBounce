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
package net.ccbluex.liquidbounce.utils.text

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents

/**
 * [exact] when vanilla sent it.
 */
internal data class DeathMessage(val victim: String, val killer: String?, val exact: Boolean)

/**
 * Reads this message as a death message about one of [names].
 *
 * Vanilla: a `death.*` translation with victim and killer as arguments.
 * Custom, like Hypixel's "Enemy was killed by Player.": the victim starts the sentence, at most behind
 * a prefix of the server. A prefix naming a player ([isPlayer]) is someone talking.
 */
internal fun Component.toDeathMessage(
    names: Collection<String>,
    isPlayer: (String) -> Boolean = { false },
): DeathMessage? {
    val contents = contents
    return if (contents is TranslatableContents) {
        contents.takeIf { it.key.startsWith("death.") }?.let { vanillaDeathMessage(it, names) }
    } else {
        customDeathMessage(ChatFormatting.stripFormatting(string).orEmpty(), names, isPlayer)
    }
}

// Arguments are display names, which may carry a team prefix
private fun vanillaDeathMessage(contents: TranslatableContents, names: Collection<String>): DeathMessage? {
    val players = contents.args.map { arg ->
        val text = (arg as? Component)?.string ?: arg.toString()
        text.namesIn(names).firstOrNull() ?: text
    }
    return players.firstOrNull()?.let { DeathMessage(it, players.getOrNull(1), exact = true) }
}

// "Enemy was ..." is a sentence, "Enemy: gg" and "<Enemy> gg" are chat
private fun customDeathMessage(
    message: String,
    names: Collection<String>,
    isPlayer: (String) -> Boolean,
): DeathMessage? {
    val text = message.trimStart { !it.isLetterOrDigit() && it != '_' }
    val named = text.positionsOf(names)
    val (start, victim) = named.firstOrNull() ?: return null
    val end = start + victim.length
    val isSentence = text.getOrNull(end) == ' ' && text.getOrNull(end + 1)?.isLetter() == true
    return if (isSentence && text.substring(0, start).isServerPrefix(isPlayer)) {
        DeathMessage(victim, named.getOrNull(1)?.second, exact = false)
    } else {
        null
    }
}

private val NAME = Regex("[A-Za-z0-9_]+")

// "SkyWars ▸ " and "Duels ▸ Player " lead into a sentence, "[VIP] Other: " and "Other was killed by " do not
private fun String.isServerPrefix(isPlayer: (String) -> Boolean): Boolean {
    if (isEmpty()) {
        return true
    }
    val tag = trimEnd().dropLastWhile { it.isLetter() }.trimEnd()
    val separator = tag.lastOrNull() ?: return !isPlayer(trim())
    return !separator.isLetterOrDigit() && separator !in ":>_" && NAME.findAll(this).none { isPlayer(it.value) }
}

private fun String.positionsOf(names: Collection<String>) = names
    .mapNotNull { name ->
        Regex("(?<![A-Za-z0-9_])${Regex.escape(name)}(?![A-Za-z0-9_])").find(this)?.let { it.range.first to name }
    }
    .sortedBy { it.first }

private fun String.namesIn(names: Collection<String>) = positionsOf(names).map { it.second }
