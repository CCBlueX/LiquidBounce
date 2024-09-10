package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.LiquidBounce
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.random.Random

private val ADJECTIVES = loadLines("adjectives.txt")
private val ANIMALS = loadLines("animals.txt")

private val FILLER_CHARS = "0123456789_".toCharArray()

private val LEET_MAP = mapOf(
    "a" to "4",
    "b" to "8",
    "e" to "3",
    "g" to "6",
    "i" to "1",
    "o" to "0",
    "s" to "5",
    "t" to "7",
    "z" to "2"
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
 * @author mems01
 *
 * Generates 16 char long names in this format:
 * (x = random separator character (0-9_))
 *
 * xxx (random count, to fill target length)
 * ADJECTIVE (randomly swapped leetable letters, first and last char are excluded to keep name readable)
 * x (acts like a space, always if under target length)
 * ANIMAL (same as adjective)
 * xxx (random count, to fill target length)
 */
fun randomUsername(
    maxLength: Int,
    rng: Random
): String {
    val adjective: String
    val animal: String

    //For all combinations to be equally probable, it is randomised, whether adjective or animal is chosen first.
    if (rng.nextBoolean()) {
        adjective = ADJECTIVES.filter { it.length <= maxLength - 3 }.random(rng)
        animal = ANIMALS.filter { it.length <= maxLength - adjective.length }.random(rng)
    } else {
        animal = ANIMALS.filter { it.length <= maxLength - 3 }.random(rng)
        adjective = ADJECTIVES.filter { it.length <= maxLength - animal.length }.random(rng)
    }

    val baseName = leetRandomly(rng, adjective) + (if (adjective.length + animal.length < maxLength) random(
        rng,
        1,
        FILLER_CHARS,
    ) else "") + leetRandomly(rng, animal)

    if (true) {
        return adjective + (if (adjective.length + animal.length < maxLength) "_" else "") + animal
    }

    val fillerCount = maxLength - baseName.length

    val stringBuilder = StringBuilder(random(rng, fillerCount, FILLER_CHARS))

    val idx = if (fillerCount > 0) rng.nextInt(fillerCount) else 0

    stringBuilder.insert(idx, baseName)

    //Adds random prefix and suffix made up from filler characters.
    return stringBuilder.toString()
}

//Randomly converts "leetable" characters, skips first and last.
private fun leetRandomly(rng: Random, string: String) = string.mapIndexed { i, char ->
    if (i != 0 && i != string.lastIndex && rng.nextBoolean())
        LEET_MAP[char.lowercase()] ?: char
    else char
}.joinToString("")

