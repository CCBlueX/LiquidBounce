@file:Suppress("NOTHING_TO_INLINE")
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import java.io.InputStream

/**
 * @param path prefix /resources/liquidbounce/$path
 */
inline fun Identifier.registerDynamicImageFromResources(path: String) {
    with(LiquidBounce.javaClass.getResourceAsStream("/resources/liquidbounce/$path")!!) {
        this@registerDynamicImageFromResources.registerDynamicImage(this)
    }
}

@Suppress("MagicNumber")
inline fun String.registerAsDynamicImageFromClientResources(): Identifier =
    Identifier.of("liquidbounce", "dynamic-texture-" + System.currentTimeMillis().toString(36)).apply {
        registerDynamicImageFromResources(this@registerAsDynamicImageFromClientResources)
    }

inline fun Identifier.registerDynamicImage(image: InputStream) {
    this.registerDynamicImage(NativeImage.read(image))
}

inline fun Identifier.registerDynamicImage(image: NativeImage) {
    mc.textureManager.registerTexture(this, NativeImageBackedTexture(image))
}
