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

/**
 * A srg remapper
 *
 * @author CCBlueX
 */
object Remapper {

    private const val srgName = "stable_22"
    private val srgFile = File(dir, "mcp-$srgName.srg")

    private val fields : HashMap<String, HashMap<String, String>> = hashMapOf()
    private val methods : HashMap<String, HashMap<String, String>> = hashMapOf()

    /**
     * Load srg
     */
    fun loadSrg() {
        // Check if srg file is already downloaded
        if (!srgFile.exists()) {
            // Download srg file
            srgFile.createNewFile()

            LOGGER.info("[Remapper] Downloading $srgName srg...")
            download("$CLIENT_CLOUD/srgs/mcp-$srgName.srg", srgFile)
            LOGGER.info("[Remapper] Downloaded $srgName.")
        }

        // Load srg
        LOGGER.info("[Remapper] Loading srg...")
        parseSrg()
        LOGGER.info("[Remapper] Loaded srg.")
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