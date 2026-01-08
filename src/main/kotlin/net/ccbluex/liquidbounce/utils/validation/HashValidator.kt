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

package net.ccbluex.liquidbounce.utils.validation

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.gson.util.readJson
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.util.Util
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.util.concurrent.CompletableFuture

object HashValidator {

    private const val HASH_FILE_NAME = ".hash"

    private fun containsHashFile(f: File) = f.resolve(HASH_FILE_NAME).exists()

    fun validateFolder(file: File) {
        if (!file.exists()) {
            return
        }

        if (!file.isDirectory || !containsHashFile(file)) {
            deleteFolder(file)
        }

        file.walk().mapNotNull { it.resolve(HASH_FILE_NAME).takeIf(File::exists) }
            .mapTo(mutableListOf()) {
                CompletableFuture.runAsync({ validateHashFile(it) }, Util.backgroundExecutor())
            }.forEach {
                it.join()
            }
    }

    @JvmStatic
    private fun validateHashFile(hashFile: File) {
        val delete = try {
            val hashes = hashFile.readJson<Map<String, String>>()
            shouldDelete(hashFile, hashes)
        } catch (e: Exception) {
            logger.warn("Invalid hash file ${hashFile.absolutePath}", e)
            false
        }

        if (delete) {
            val folderToDelete = hashFile.parentFile

            logger.warn("Verification of ${folderToDelete.absolutePath} failed. Deleting folder..")
            deleteFolder(folderToDelete)
        }
    }

    private fun deleteFolder(folderToDelete: File) {
        try {
            folderToDelete.deleteRecursively()
        } catch (e: Exception) {
            logger.warn("Failed to delete ${folderToDelete.absolutePath}. Retrying on exit...", e)

            Runtime.getRuntime().addShutdownHook(Thread {
                runCatching {
                    folderToDelete.deleteRecursively()
                }.onFailure {
                    LiquidBounce.logger.error("Failed to delete ${folderToDelete.absolutePath}.", it)
                }
            })
        }
    }

    private fun shouldDelete(hashFile: File, hashes: Map<String, String>): Boolean {
        try {
            val folder = hashFile.parentFile
            val hashAndFile = hashes.mapKeys { (k, _) -> File(folder, k) }
            hashAndFile.keys.find {
                !it.exists() || !it.isFile
            }?.let {
                // A file went missing? A file is not a file anymore? Better delete it.
                logger.warn("File ${it.absolutePath} went missing.")

                return true
            }

            return hashAndFile.entries.any { (file, expected) ->
                // Read the file, hash it and compare it to the hash in the hash file
                // Use the InputStream, don't read the full file
                val sha256Hex = file.inputStream().use(DigestUtils::sha256Hex)

                !sha256Hex.equals(expected, ignoreCase = true)
            }
        } catch (e: Exception) {
            logger.error("Failed to validate ${hashFile.absolutePath}", e)

            return true
        }
    }

}
