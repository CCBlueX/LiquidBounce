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
package net.ccbluex.liquidbounce.utils.io

import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Base64
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream

@Throws(IOException::class)
fun Path.tryMoveReplacing(target: Path): Path =
    try {
        Files.move(this, target, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(this, target, StandardCopyOption.REPLACE_EXISTING)
    } catch (_: FileAlreadyExistsException) {
        Files.move(this, target, StandardCopyOption.REPLACE_EXISTING)
    }


private fun Path.readAsBase64(output: OutputStream) {
    this.inputStream().use { input ->
        Base64.getEncoder().wrap(output).use { base64 ->
            input.copyTo(base64)
        }
    }
}

fun Path.readAsBase64(): String {
    val output = FastByteArrayOutputStream(Math.toIntExact(((this.fileSize() + 2) / 3) * 4))
    this.readAsBase64(output)
    return output.toString(Charsets.US_ASCII)
}
