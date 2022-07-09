/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.ui.client.hud.Config
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Constructor of config
 *
 * @param file
 * of config
 */
class HudConfig(file: File) : FileConfig(file)
{
    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig()
    {
        LiquidBounce.hud.clearElements()
        LiquidBounce.hud = Config(FileUtils.readFileToString(file, StandardCharsets.UTF_8)).toHUD()
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun saveConfig()
    {
        val writer = file.bufferedWriter()
        writer.write(Config(LiquidBounce.hud).toJson() + System.lineSeparator())
        writer.close()
    }
}
