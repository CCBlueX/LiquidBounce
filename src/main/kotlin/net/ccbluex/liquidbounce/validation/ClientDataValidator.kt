package net.ccbluex.liquidbounce.validation

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

private const val HASH_FILE_NAME = ".hash"

object ClientDataValidator {
    /**
     * These folders are *recursively* searched for hash files (.hash). The files are checked against the hash and if
     * the hash does not match the **ENTIRE FOLDER IS DELETED**. Never add a folder to this list
     * that contains user data.
     */
    private val VALIDATED_FOLDERS = arrayOf("fonts", "mcef", "themes/default")

    fun validateInstallation() {
        LiquidBounce.logger.info("Validing installation...")

        for (folder in VALIDATED_FOLDERS) {
            val file = ConfigSystem.rootFolder.resolve(folder)

            when {
                !file.exists() -> continue
                !file.isDirectory -> file.delete()
                else -> validateFolder(file)
            }
        }
    }

    private fun containsHashFile(f: File) = f.resolve(HASH_FILE_NAME).exists()

    private fun validateFolder(file: File) {
        file
            .walk()
            .mapNotNull { it.resolve(HASH_FILE_NAME).takeIf(File::exists) }
            .forEach(::validateHashFile)
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

            LiquidBounce.logger.warn("Verification of ${folderToDelete.absolutePath} failed. Deleting folder..")

            deleteFolder(folderToDelete)
        }
    }

    private fun deleteFolder(folderToDelete: File) {
        runCatching {
            folderToDelete.deleteRecursively()
        }.onSuccess { return }

        LiquidBounce.logger.warn("Failed to delete ${folderToDelete.absolutePath}. Retrying on exit...")

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
                    LiquidBounce.logger.warn("File ${resolveSibling.absolutePath} went missing.")

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
            LiquidBounce.logger.error("Failed to validate ${hashFile.absolutePath}", e)

            return true
        }

        return false
    }

    fun expectHashOrDelete(f: File) {
        if (!f.isDirectory || !containsHashFile(f)) {
            deleteFolder(f)
        }
    }

}
