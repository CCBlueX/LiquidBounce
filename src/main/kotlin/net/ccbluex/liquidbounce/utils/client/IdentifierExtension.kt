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
 *
 */

@file:Suppress("NOTHING_TO_INLINE")
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import java.util.*

/**
 * @param path prefix /resources/liquidbounce/$path
 */
internal fun Identifier.registerDynamicImageFromResources(path: String) {
    val resourceStream = LiquidBounce::class.java.getResourceAsStream("/resources/liquidbounce/$path")!!

    NativeImage.read(resourceStream).registerTexture(this@registerDynamicImageFromResources)
}

internal inline fun String.registerAsDynamicImageFromClientResources(): Identifier =
    Identifier.of(LiquidBounce.CLIENT_NAME.lowercase(), "dynamic-texture-" + UUID.randomUUID()).apply {
        registerDynamicImageFromResources(this@registerAsDynamicImageFromClientResources)
    }

fun NativeImage.registerTexture(identifier: Identifier) {
    mc.textureManager.registerTexture(identifier, NativeImageBackedTexture(this))
}

/**
 * Converts an [Identifier] to a human-readable name without localization.
 */
inline fun Identifier.toName() = toString()
    .split(':')
    .last()
    .replace('.', ' ')
    .replace('_', ' ')
    .split(' ')
    .joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
    }
