package net.ccbluex.liquidbounce.web.background

import net.ccbluex.liquidbounce.render.shader.Shader
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.resourceToString
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import java.io.File

class Background(val name: String, val folder: File) {

    private val backgroundShader: File
        get() = File(folder, "background.frag")
    private val backgroundImage: File
        get() = File(folder, "background.png")
    var compiledShaderBackground: Shader? = null
        private set
    var loadedBackgroundImage: Identifier? = null
        private set

    fun compileShader(): Boolean {
        if (compiledShaderBackground != null) {
            return true
        }

        readShaderBackground()?.let { shaderBackground ->
            compiledShaderBackground = Shader(
                resourceToString("/assets/liquidbounce/shaders/vertex.vert"),
                shaderBackground)
            logger.info("Compiled background shader for theme $name")
            return true
        }
        return false
    }

    private fun readShaderBackground() = backgroundShader.takeIf { it.exists() }?.readText()
    private fun readBackgroundImage() = backgroundImage.takeIf { it.exists() }
        ?.inputStream()?.use { NativeImage.read(it) }

    fun loadBackgroundImage(): Boolean {
        if (loadedBackgroundImage != null) {
            return true
        }

        val image = NativeImageBackedTexture(readBackgroundImage() ?: return false)
        loadedBackgroundImage = mc.textureManager.registerDynamicTexture("liquidbounce-bg-$name", image)
        logger.info("Loaded background image for theme $name")
        return true
    }

}
