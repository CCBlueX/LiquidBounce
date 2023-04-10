/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file

import java.io.File
import java.io.IOException

abstract class FileConfig(val file: File) {

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    abstract fun loadConfig()

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    abstract fun saveConfig()

    /**
     * Create config
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun createConfig() = file.createNewFile()

    /**
     * @return config file exist
     */
    fun hasConfig() = file.exists() && file.length() > 0

    /**
     * Load defaults when config file doesn't exist.
     */
    open fun loadDefault() {}
}