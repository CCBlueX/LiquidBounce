package net.ccbluex.liquidbounce.utils.client

import com.google.common.math.IntMath
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.kotlin.subList
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.random.Random
import kotlin.random.nextInt

private val ADJECTIVE_LISTS_BY_SIZE = buildShorterThanList(loadLines("adjectives.txt"))
private val ANIMAL_LISTS_BY_SIZE = buildShorterThanList(loadLines("animals.txt"))

private val LEET_MAP = mapOf(
    'a' to '4',
    'b' to '8',
    'e' to '3',
    'g' to '6',
    'i' to '1',
    'o' to '0',
    's' to '5',
    't' to '7',
    'z' to '2'
)

private fun loadLines(name: String): List<String> {
    val resourceName = "/assets/liquidbounce/data/usernames/$name"
    val inputStream =
        LiquidBounce.javaClass.getResourceAsStream(resourceName)
            ?: error("Failed to load resource $resourceName")
    val reader = BufferedReader(InputStreamReader(inputStream))

    return reader.readLines()
}

private fun random(rng: Random, length: Int, chars: CharArray): String {
    val stringBuilder = StringBuilder()

    repeat(length) {
        stringBuilder.append(chars[rng.nextInt(chars.size)])
    }

    return stringBuilder.toString()
}

/**
 * @author mems01 (original in legacy), superblaubeere27 (edit)
 *
 * Generates 16 char long names in this format:
 * (x = random separator character (0-9_))
 */
fun randomUsername(
    maxLength: Int,
    rng: Random
): String {
    val (firstWordList, secondWordList) = if (rng.nextBoolean()) {
        ADJECTIVE_LISTS_BY_SIZE to ANIMAL_LISTS_BY_SIZE
    } else {
        ANIMAL_LISTS_BY_SIZE to ADJECTIVE_LISTS_BY_SIZE
    }

    // Subtract 3 because the smallest second word is 3 long.
    val firstWord = findWordShorterOrEqual(firstWordList, maxLength - 3).random(rng)
    val secondWord = findWordShorterOrEqual(secondWordList, maxLength - firstWord.length).random(rng)

    var elements = listOf(firstWord, secondWord)

    val currLen = elements.sumOf { it.length }

    if (currLen + 1 < maxLength && rng.nextInt(20) != 0) {
        val until = (maxLength - currLen).coerceAtMost(3)
        val digits = if (until <= 2) until else rng.nextInt(2, until)

        elements = elements + listOf(rng.nextInt(IntMath.pow(10, digits)).toString())
    }

    val allowedDelimiters = maxLength - elements.sumOf { it.length }

    var currentDelimiters = rng.nextBits(2)

    while (currentDelimiters.countOneBits() > allowedDelimiters.coerceAtLeast(0)) {
        currentDelimiters = rng.nextBits(2)
    }

    val output = StringBuilder(elements[0])

    elements.subList(1).forEach {
        if (currentDelimiters and 1 == 1) {
            output.append("_")
        }

        currentDelimiters = currentDelimiters shr 1

        output.append(it)
    }

    return leetRandomly(rng, output.toString(), rng.nextInt(3))
}

fun leetRandomly(rng: Random, str: String, leetReplacements: Int): String {
    val charArray = str.toCharArray()
    val indices = ArrayList(charArray.indices.filter { LEET_MAP.containsKey(charArray[it]) })

    for (ignored in 0..<leetReplacements.coerceAtMost(indices.size)) {
        val idx = indices.random(rng)

        charArray[idx] = LEET_MAP.get(charArray[idx])!!

        // Terrible performance, but ok
        indices.remove(idx)
    }

    return String(charArray)
}

private fun findWordShorterOrEqual(strings: List<List<String>>, maxLength: Int) =
    strings.getOrNull(maxLength - 3) ?: strings.last()

private fun buildShorterThanList(list: List<String>): List<List<String>> {
    val sortedList = list.sortedBy { it.length }

    var out = Array<List<String>?>(sortedList.last().length) { null }

    var lastLen = 0

    sortedList.forEachIndexed { idx, s ->
        if (s.length != lastLen) {
            out[lastLen] = sortedList.subList(0, idx)
            lastLen = s.length
        }
    }

    if (out[0] == null) {
        out[0] = emptyList()
    }

    // Fill remaining slots
    for (idx in 1..<out.size) {
        if (out[idx] == null) {
            out[idx] = out[idx - 1]
        }
    }

    return out.map { it!! }
}
