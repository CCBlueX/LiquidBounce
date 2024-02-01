/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.script.remapper

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.file.FileManager.dir
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.download
import java.io.File
import java.security.MessageDigest

/**
 * A srg remapper
 *
 * @author CCBlueX
 */
object Remapper {

    private const val srgName = "stable_22"
    private val srgFile = File(dir, "mcp-$srgName.srg")

    internal var mappingsLoaded = false

    private val fields = hashMapOf<String, HashMap<String, String>>()
    private val methods = hashMapOf<String, HashMap<String, String>>()

    /**
     * Load srg
     */
    fun loadSrg() {
        // Download sha256 file
        val sha256File = File(dir, "mcp-$srgName.srg.sha256")
        if (!sha256File.exists() || !sha256File.isFile || sha256File.readText().isEmpty()) {
            sha256File.createNewFile()

            download("$CLIENT_CLOUD/srgs/mcp-$srgName.srg.sha256", sha256File)
            LOGGER.info("[Remapper] Downloaded $srgName sha256.")
        }

        // Check if srg file is already downloaded
        if (!srgFile.exists() || !hashMatches(srgFile, sha256File)) {
            // Download srg file
            srgFile.createNewFile()

            download("$CLIENT_CLOUD/srgs/mcp-$srgName.srg", srgFile)
            LOGGER.info("[Remapper] Downloaded $srgName.")
        }

        // Load srg
        parseSrg()
        LOGGER.info("[Remapper] Successfully loaded SRG mappings.")
    }

    private fun hashMatches(srgFile: File, sha256File: File): Boolean {
        val fileContent = srgFile.readText()

        // Generate SHA-256 hash of file content
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(fileContent.toByteArray()).joinToString("") { "%02x".format(it) }

        // sha265sum mcp-stable_22.srg
        // -> a8486671a5e85153773eaac313f8babd1913b41524b45e92d42e6cf019e658eb  mcp-stable_22.srg
        if (sha256File.exists()) {
            val sha256 = sha256File.readText().split(" ")[0]

            LOGGER.info("[Remapper] Hash $sha256 compared to $hash")
            return sha256 == hash
        }

        LOGGER.warn("[Remapper] No sha256 file found.")
        return false
    }

    private fun parseSrg() {
        srgFile.readLines().forEach {
            val args = it.split(" ")

            when {
                it.startsWith("FD:") -> {
                    val name = args[1]
                    val srg = args[2]

                    val className = name.substring(0, name.lastIndexOf('/')).replace('/', '.')
                    val fieldName = name.substring(name.lastIndexOf('/') + 1)
                    val fieldSrg = srg.substring(srg.lastIndexOf('/') + 1)

                    if (className !in fields)
                        fields[className] = hashMapOf()

                    fields[className]!![fieldSrg] = fieldName
                }

                it.startsWith("MD:") -> {
                    val name = args[1]
                    val desc = args[2]
                    val srg = args[3]

                    val className = name.substring(0, name.lastIndexOf('/')).replace('/', '.')
                    val methodName = name.substring(name.lastIndexOf('/') + 1)
                    val methodSrg = srg.substring(srg.lastIndexOf('/') + 1)

                    if (className !in methods)
                        methods[className] = hashMapOf()

                    methods[className]!![methodSrg + desc] = methodName
                }
            }
        }

        mappingsLoaded = true
    }

    /**
     * Remap field
     */
    fun remapField(clazz : Class<*>, name : String) =
        fields[clazz.name]?.getOrDefault(name, name) ?: name

    /**
     * Remap method
     */
    fun remapMethod(clazz : Class<*>, name : String, desc : String) =
        methods[clazz.name]?.getOrDefault(name + desc, name) ?: name
}