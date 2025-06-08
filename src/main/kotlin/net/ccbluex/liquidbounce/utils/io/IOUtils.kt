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
@file:Suppress("NOTHING_TO_INLINE")
package net.ccbluex.liquidbounce.utils.io

import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import okio.buffer
import okio.source
import java.io.File
import java.io.InputStream

inline fun InputStream.readNativeImage(
    format: NativeImage.Format? = NativeImage.Format.RGBA,
): NativeImage = NativeImage.read(format, this)

inline fun NativeImage?.asTexture(): NativeImageBackedTexture = NativeImageBackedTexture(this)

inline fun File.takeIfExists() = this.takeIf { it.exists() }

fun File.listAllDirectory() = this.listFiles()?.filter { it.isDirectory } ?: emptyList()

fun File.listAllFile() = this.listFiles()?.filter { it.isFile } ?: emptyList()

fun File.readUtf8(): String = inputStream().readUtf8()

fun InputStream.readUtf8(): String = use { source().buffer().readUtf8() }
