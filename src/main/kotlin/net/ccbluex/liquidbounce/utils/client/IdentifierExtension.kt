@file:Suppress("NOTHING_TO_INLINE")
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import java.util.*

/**
 * @param path prefix /resources/liquidbounce/$path
 */
internal fun Identifier.registerDynamicImageFromResources(path: String) {
    val resourceStream = LiquidBounce::class.java.getResourceAsStream("/resources/liquidbounce/$path")!!

    NativeImage.read(resourceStream).registerTexture(this@registerDynamicImageFromResources)
}

internal inline fun String.registerAsDynamicImageFromClientResources(): Identifier =
    Identifier.of(LiquidBounce.CLIENT_NAME.lowercase(), "dynamic-texture-" + UUID.randomUUID()).apply {
        registerDynamicImageFromResources(this@registerAsDynamicImageFromClientResources)
    }

fun NativeImage.registerTexture(identifier: Identifier) {
    mc.textureManager.registerTexture(identifier, NativeImageBackedTexture(this))
}

/**
 * Converts an [Identifier] to a human-readable name without localization.
 */
inline fun Identifier.toName() = toString()
    .split(':')
    .last()
    .replace('.', ' ')
    .replace('_', ' ')
    .split(' ')
    .joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
    }
