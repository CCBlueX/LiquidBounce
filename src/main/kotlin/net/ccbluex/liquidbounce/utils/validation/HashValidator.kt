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
        val hashExtractor: (FileInputStream) -> Map<String, String> = {
            Gson().fromJson(
                InputStreamReader(it),
                object : TypeToken<Map<String, String>>() {}.type
            )
        }

        val delete = runCatching {
            val hashes = FileInputStream(hashFile).use(hashExtractor)

            shouldDelete(hashFile, hashes)
        }.onFailure {
            logger.warn("Invalid hash file ${hashFile.absolutePath}", it)
        }.getOrDefault(true)

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
