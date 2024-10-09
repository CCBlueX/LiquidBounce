package net.ccbluex.liquidbounce.integration.theme

import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.render.shader.Shader
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.resourceToString
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import java.io.File

/**
 * A client wallpaper that is displayed in the background of the client UI no matter what theme implementation is
 * being used.
 *
 * There are two types of wallpapers:
 * - [ImageWallpaper] which is a simple image that is displayed in the background.
 * - [ShaderWallpaper] which is a shader that is rendered in the background.
 */
abstract class Wallpaper(val theme: Theme, val name: String, val file: File) {

    companion object {
        fun fromFile(theme: Theme, file: File): Wallpaper {
            return when (file.extension) {
                "png" -> ImageWallpaper(theme, file.name, file)
                "frag" -> ShaderWallpaper(theme, file.name, file)
                else -> throw IllegalArgumentException("Unknown wallpaper type for file $file")
            }
        }
    }

    class ImageWallpaper(theme: Theme, name: String, file: File) : Wallpaper(theme, name, file) {

        private var imageId: Identifier? = null

        override fun load(): Boolean {
            if (imageId != null || !file.exists()) {
                return true
            }

            val image = NativeImageBackedTexture(NativeImage.read(file.inputStream()))
            imageId = mc.textureManager.registerDynamicTexture("liquidbounce-bg-$name", image)
            logger.info("Loaded background image for theme $name")
            return true
        }

        override fun draw(context: DrawContext, width: Int, height: Int, mouseX: Int, mouseY: Int,
                          delta: Float): Boolean {
            val imageId = imageId ?: return false
            context.drawTexture(imageId, 0, 0, 0f, 0f, width, height, width, height)
            return true
        }

    }

    class ShaderWallpaper(theme: Theme, name: String, file: File) : Wallpaper(theme, name, file) {

        private var shader: Shader? = null

        override fun load(): Boolean {
            if (shader != null || !file.exists()) {
                return true
            }

            val shaderSource = file.readText()
            shader = Shader(resourceToString("/assets/liquidbounce/shaders/vertex.vert"), shaderSource)
            logger.info("Compiled background shader for theme $name")
            return true
        }

        override fun draw(context: DrawContext, width: Int, height: Int, mouseX: Int, mouseY: Int,
                          delta: Float): Boolean {
            val shader = shader ?: return false
            shader.draw(mouseX, mouseY, width, height, delta)
            return true
        }

    }

    abstract fun load(): Boolean
    abstract fun draw(context: DrawContext, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float): Boolean

}
