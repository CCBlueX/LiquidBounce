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

/**
 * Moves this path to [target], replacing [target] if it already exists.
 *
 * The move is performed atomically when the platform and file stores allow it
 * (`StandardCopyOption.ATOMIC_MOVE`). If an atomic replace is not supported
 * (for example when source and target are on different file stores) or the
 * implementation refuses to replace an existing target under `ATOMIC_MOVE`,
 * the operation falls back to a non-atomic `REPLACE_EXISTING` move.
 *
 * The fallback is not crash-safe: a failure or interruption in the middle may
 * leave [target] deleted, truncated, or otherwise inconsistent. Callers that
 * require an all-or-nothing replace must keep source and target on the same
 * file store and treat [AtomicMoveNotSupportedException] as fatal instead of
 * using this helper.
 *
 * Replacing a non-empty directory is not supported and still fails with
 * [java.nio.file.DirectoryNotEmptyException] or another [IOException].
 *
 * @return [target]
 * @throws IOException if the move fails
 * @throws java.nio.file.DirectoryNotEmptyException if [target] is a non-empty directory
 * @throws SecurityException if the security manager denies the operation
 */
@Throws(IOException::class)
fun Path.atomicMoveTo(target: Path): Path {
    return try {
        Files.move(
            this,
            target,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    } catch (first: AtomicMoveNotSupportedException) {
        replaceExisting(target, first)
    } catch (first: java.nio.file.FileAlreadyExistsException) {
        replaceExisting(target, first)
    }
}

@Throws(IOException::class)
private fun Path.replaceExisting(target: Path, cause: IOException): Path =
    try {
        Files.move(this, target, StandardCopyOption.REPLACE_EXISTING)
    } catch (e: IOException) {
        e.addSuppressed(cause)
        throw e
    }

private fun Path.readAsBase64(output: OutputStream) {
    this.inputStream().use { input ->
        Base64.getEncoder().wrap(output).use { base64 ->
            input.transferTo(base64)
        }
    }
}

fun Path.readAsBase64(): String {
    val output = FastByteArrayOutputStream(Math.toIntExact(((this.fileSize() + 2) / 3) * 4))
    this.readAsBase64(output)
    return output.toString(Charsets.US_ASCII)
}
