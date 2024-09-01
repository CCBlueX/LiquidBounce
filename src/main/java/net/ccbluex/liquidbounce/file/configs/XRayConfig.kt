/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.features.module.modules.render.XRay
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.minecraft.block.Block
import java.io.*

class XRayConfig(file: File) : FileConfig(file) {

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig() {
        val jsonArray = JsonParser().parse(file.bufferedReader()).asJsonArray

        XRay.xrayBlocks.clear()
        for (jsonElement in jsonArray) {
            try {
                val block = Block.getBlockFromName(jsonElement.asString)
                if (block in XRay.xrayBlocks) {
                    LOGGER.error("[FileManager] Skipped xray block '${block.registryName}' because the block is already added.")
                    continue
                }
                XRay.xrayBlocks += block
            } catch (throwable: Throwable) {
                LOGGER.error("[FileManager] Failed to add block to xray.", throwable)
            }
        }
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun saveConfig() {
        val jsonArray = JsonArray()
        for (block in XRay.xrayBlocks) jsonArray.add(PRETTY_GSON.toJsonTree(Block.getIdByBlock(block)))

        file.writeText(PRETTY_GSON.toJson(jsonArray))
    }
}