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
package net.ccbluex.liquidbounce.utils.client

import com.google.common.math.IntMath
import it.unimi.dsi.fastutil.chars.Char2CharArrayMap
import it.unimi.dsi.fastutil.ints.IntCharPair
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.ccbluex.liquidbounce.utils.kotlin.subList
import kotlin.random.Random

private val ADJECTIVE_LISTS_BY_SIZE = buildShorterThanList(loadLines("adjectives.txt"))
private val ANIMAL_LISTS_BY_SIZE = buildShorterThanList(loadLines("animals.txt"))

private val LEET_MAP = Char2CharArrayMap(
    charArrayOf('a', 'b', 'e', 'g', 'i', 'o', 's', 't', 'z'),
    charArrayOf('4', '8', '3', '6', '1', '0', '5', '7', '2'),
)

private fun loadLines(name: String): List<String> {
    val resourceName = "/resources/liquidbounce/data/usernames/$name"
    val inputStream =
        LiquidBounce::class.java.getResourceAsStream(resourceName)
            ?: error("Failed to load resource $resourceName")

    return inputStream.bufferedReader().readLines()
}

/**
 * Generates 16 char long names in this format:
 * (x = random separator character (0-9_))
 */
fun randomUsername(
    maxLength: Int = Random.Default.nextInt(8, 17),
    rng: Random = Random.Default
): String {
    val (firstWordList, secondWordList) = if (rng.nextBoolean()) {
        ADJECTIVE_LISTS_BY_SIZE to ANIMAL_LISTS_BY_SIZE
    } else {
        ANIMAL_LISTS_BY_SIZE to ADJECTIVE_LISTS_BY_SIZE
    }

    // Subtract 3 because the smallest second word is 3 long.
    val firstWord = findWordShorterOrEqual(firstWordList, maxLength - 3).random(rng)
    val secondWord = findWordShorterOrEqual(secondWordList, maxLength - firstWord.length).random(rng)

    val elements = mutableListOf(firstWord, secondWord)

    val currLen = elements.sumOf { it.length }

    if (currLen + 1 < maxLength && rng.nextInt(20) != 0) {
        val until = (maxLength - currLen).coerceAtMost(3)
        val digits = if (until <= 2) until else rng.nextInt(2, until)

        elements += rng.nextInt(IntMath.pow(10, digits)).toString()
    }

    val allowedDelimiters = maxLength - elements.sumOf { it.length }

    var currentDelimiters = rng.nextBits(2)

    while (currentDelimiters.countOneBits() > allowedDelimiters.coerceAtLeast(0)) {
        currentDelimiters = rng.nextBits(2)
    }

    val output = StringBuilder(elements[0])

    elements.subList(1).forEach {
        if (currentDelimiters and 1 == 1) {
            output.append('_')
        }

        currentDelimiters = currentDelimiters shr 1

        output.append(it)
    }

    return leetRandomly(rng, output.toString(), rng.nextInt(3))
}

fun leetRandomly(rng: Random, str: String, leetReplacements: Int): String {
    val charArray = str.toCharArray()

    val list = ArrayList<IntCharPair>()
    for (i in charArray.indices) {
        val char = LEET_MAP[charArray[i]]
        if (char != 0.toChar()) {
            list += IntCharPair.of(i, char)
        }
    }

    list.shuffle(rng)

    for ((i, char) in list.subList(0, leetReplacements.coerceAtMost(list.size))) {
        charArray[i] = char
    }

    return String(charArray)
}

private fun findWordShorterOrEqual(strings: Array<List<String>>, maxLength: Int) =
    strings[maxLength.coerceIn(0, strings.lastIndex)]

private fun buildShorterThanList(list: List<String>): Array<List<String>> {
    val sortedList = list.sortedBy { it.length }

    val out = Array<List<String>>(sortedList.last().length) { emptyList() }

    var lastLen = 0

    sortedList.forEachIndexed { idx, s ->
        if (s.length != lastLen) {
            out[lastLen] = sortedList.subList(0, idx)
            lastLen = s.length
        }
    }

    // Fill remaining slots
    for (idx in 1 until out.size) {
        if (out[idx].isEmpty()) {
            out[idx] = out[idx - 1]
        }
    }

    return out
}
