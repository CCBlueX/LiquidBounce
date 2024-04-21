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
 *
 */
package net.ccbluex.liquidbounce.utils.validation

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.logger
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

private const val HASH_FILE_NAME = ".hash"

object HashValidator {

    private fun containsHashFile(f: File) = f.resolve(HASH_FILE_NAME).exists()

    fun validateFolder(file: File) {
        if (!file.exists()) {
            return
        }

        if (!file.isDirectory) {
            file.delete()
            return
        }

        expectHashOrDelete(file)

        file.walk()
            .mapNotNull { it.resolve(HASH_FILE_NAME).takeIf(File::exists) }
            .forEach(HashValidator::validateHashFile)
    }

    private fun validateHashFile(hashFile: File) {
        val hashes: (FileInputStream) -> Map<String, String> = {
            Gson().fromJson(
                InputStreamReader(it),
                object : TypeToken<Map<String, String>>() {}.type
            )
        }

        val delete = shouldDelete(hashFile, FileInputStream(hashFile).use(hashes))

        if (delete) {
            val folderToDelete = hashFile.parentFile

            logger.warn("Verification of ${folderToDelete.absolutePath} failed. Deleting folder..")
            deleteFolder(folderToDelete)
        }
    }

    private fun deleteFolder(folderToDelete: File) {
        runCatching {
            folderToDelete.deleteRecursively()
        }.onSuccess { return }

        logger.warn("Failed to delete ${folderToDelete.absolutePath}. Retrying on exit...")

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                runCatching {
                    folderToDelete.deleteRecursively()
                }.onFailure {
                    LiquidBounce.logger.error("Failed to delete ${folderToDelete.absolutePath}.", it)
                }
            }
        })
    }

    private fun shouldDelete(hashFile: File, hashes: Map<String, String>): Boolean {
        try {
            for (checkedFile in hashes.entries) {
                val resolveSibling = hashFile.resolveSibling(checkedFile.key)

                // A file went missing? A file is not a file anymore? Better delete it.
                if (!resolveSibling.exists() || !resolveSibling.isFile) {
                    logger.warn("File ${resolveSibling.absolutePath} went missing.")

                    return true
                }

                // Read the file, hash it and compare it to the hash in the hash file
                val data = resolveSibling.readBytes()

                val sha256Hex = DigestUtils.sha256Hex(data)

                if (!sha256Hex.equals(checkedFile.value, ignoreCase = true)) {
                    return true
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to validate ${hashFile.absolutePath}", e)

            return true
        }

        return false
    }

    private fun expectHashOrDelete(f: File) {
        if (!f.isDirectory || !containsHashFile(f)) {
            deleteFolder(f)
        }
    }

}
