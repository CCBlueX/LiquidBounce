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

package net.ccbluex.liquidbounce.utils.input

import net.minecraft.client.util.InputUtil

/**
 * Translates a key name to an InputUtil.Key using GLFW key codes.
 * If the name is unrecognized, defaults to NONE.
 *
 * The input can be provided in the following formats:
 * - Full key name: "key.mouse.left", "key.keyboard.a", "key.keyboard.keypad.decimal"
 * - Abbreviated: "a" -> "key.keyboard.a", "lshift" -> "key.keyboard.left_shift"
 *
 * @param name The key name as a string.
 * @return The corresponding InputUtil.Key object.
 */
fun inputByName(name: String): InputUtil.Key {
    val formattedName = name.replace('_', '.')
    val translationKey = when {
        formattedName.startsWith("key.mouse.", ignoreCase = true) ||
            formattedName.startsWith("key.keyboard.", ignoreCase = true) -> formattedName.lowercase()

        formattedName.startsWith("mouse.", ignoreCase = true) ||
            formattedName.startsWith("keyboard.", ignoreCase = true) -> "key.$formattedName"

        else -> "key.keyboard.${formattedName.lowercase()}"
    }
    return InputUtil.fromTranslationKey(translationKey)
}

/**
 * Reduces a full key name (e.g., "key.keyboard.a") to its minimal form (e.g., "a").
 * This is useful for simplifying key names for easier recognition.
 *
 * @param translationKey The full key name as a string.
 * @return The reduced key name as a string.
 */
fun reduceInputName(translationKey: String): String {
    return translationKey
        .removePrefix("key.")
        .removePrefix("keyboard.")
}

/**
 * Retrieves a set of reduced mouse input names available in InputUtil.
 *
 * @return A set of simplified mouse input names.
 */
val mouseList: Set<String>
    get() = InputUtil.Type.MOUSE.map.values
        .map { key -> reduceInputName(key.translationKey) }
        .toSet()

/**
 * Retrieves a set of reduced keyboard input names available in InputUtil.
 *
 * @return A set of simplified keyboard input names.
 */
val keyList: Set<String>
    get() = InputUtil.Type.KEYSYM.map.values
        .map { key -> reduceInputName(key.translationKey) }
        .toSet()
