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
package net.ccbluex.liquidbounce.gametest

import com.mojang.blaze3d.platform.NativeImage
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.util.ARGB
import java.nio.file.Files

/**
 * The value at [path] below [group], by value and mode names.
 */
fun findValue(group: ValueGroup, vararg path: String): Value<*> {
    var current: Value<*> = group
    for (name in path) {
        val parent = current as ValueGroup
        current = (parent as? ModeValueGroup<*>)?.modes?.firstOrNull { it.name.equals(name, true) }
            ?: parent.containedValues.firstOrNull { it.name.equals(name, true) }
            ?: error("No $name in ${parent.name}, only ${parent.containedValues.map { it.name }}")
    }
    return current
}

fun setValue(group: ValueGroup, vararg path: String, to: String) = findValue(group, *path).setByString(to)

fun enableValue(group: ValueGroup, vararg path: String) = setValue(group, *path, "Enabled", to = "true")

/**
 * Takes a screenshot after the changes settled and counts the pixels in [area] matching each predicate.
 *
 * @param area gets the pixel position relative to the screen size, `0..1`
 */
fun ClientGameTestContext.countPixels(
    name: String,
    vararg predicates: (Int) -> Boolean,
    area: (Float, Float) -> Boolean = { _, _ -> true },
): IntArray {
    waitTicks(2)
    val counts = IntArray(predicates.size)
    NativeImage.read(Files.newInputStream(takeScreenshot(name))).use { image ->
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                if (!area(x / image.width.toFloat(), y / image.height.toFloat())) {
                    continue
                }
                val pixel = image.getPixel(x, y)
                predicates.forEachIndexed { index, predicate ->
                    if (predicate(pixel)) {
                        counts[index]++
                    }
                }
            }
        }
    }
    return counts
}

fun isMagenta(pixel: Int) = ARGB.red(pixel) > 235 && ARGB.green(pixel) < 20 && ARGB.blue(pixel) > 235

fun isGreen(pixel: Int) = ARGB.green(pixel) > 235 && ARGB.red(pixel) < 20 && ARGB.blue(pixel) < 20
