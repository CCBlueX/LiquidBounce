/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.ui.client.hud.Config
import java.io.File
import java.io.IOException

class HudConfig(file: File) : FileConfig(file) {

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig() {
        hud.clearElements()
        hud = Config(file.readText()).toHUD()
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun saveConfig() = file.writeText(Config(hud).toJson())
}