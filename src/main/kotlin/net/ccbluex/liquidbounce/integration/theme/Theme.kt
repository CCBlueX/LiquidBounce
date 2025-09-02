/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.integration.theme

import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.api.core.BaseApi
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentFactory.JsonComponentFactory
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.io.resource
import java.io.Closeable
import java.io.File
import java.util.*

/**
 * A web-based theme loaded from the provided URL.
 *
 * Can be local from [ClientInteropServer] or remote from the internet.
 */
class Theme(url: String) : BaseApi(url.removeSuffix("/")), Closeable {

    val id: UUID = UUID.randomUUID()

    constructor(
        prefix: String,
        file: File
    ) : this("${ClientInteropServer.url}/$prefix/${file.invariantSeparatorsPath}/")

    val metadata: ThemeMetadata = runBlocking {
        try {
            get<ThemeMetadata>("/metadata.json")
        } catch (e: Exception) {
            logger.error("Failed to load theme metadata", e)
            throw IllegalStateException("Failed to load theme metadata", e)
        }
    }

    val components: List<Component> = runBlocking {
        metadata.components.mapNotNull { name ->
            val componentFactory = runCatching {
                get<JsonComponentFactory>("/components/${name.lowercase(Locale.US)}.json")
            }.onFailure {
                logger.warn("Failed to load component $name", it)
            }.getOrNull() ?: return@mapNotNull null

            runCatching {
                componentFactory.createComponent()
            }.onFailure {
                logger.warn("Failed to create component $name", it)
            }.getOrNull()
        }
    }

//
//    fun compileShader(): Boolean {
//        if (compiledShaderBackground != null) {
//            return true
//        }
//
//        readShaderBackground()?.let { shaderBackground ->
//            compiledShaderBackground = CanvasShader(resourceToString("/resources/liquidbounce/shaders/vertex.vert"),
//                shaderBackground)
//            logger.info("Compiled background shader for theme $name")
//            return true
//        }
//        return false
//    }
//
//    private fun readShaderBackground() = backgroundShader.takeIf { it.exists() }?.readText()
//    private fun readBackgroundImage() = backgroundImage.takeIf { it.exists() }
//        ?.inputStream()?.use { NativeImage.read(it) }
//
//    fun loadBackgroundImage(): Boolean {
//        if (loadedBackgroundImage != null) {
//            return true
//        }
//
//        val image = NativeImageBackedTexture(readBackgroundImage() ?: return false)
//        loadedBackgroundImage = Identifier.of("liquidbounce", "theme-bg-${name.lowercase()}")
//        mc.textureManager.registerTexture(loadedBackgroundImage, image)
//        logger.info("Loaded background image for theme $name")
//        return true
//    }

    /**
     * Get the URL to the given page name in the theme.
     */
    fun getUrl(name: String? = null, markAsStatic: Boolean = false) = "$baseUrl/#/${name.orEmpty()}".let {
        if (markAsStatic) {
            "$it?static"
        } else {
            it
        }
    }

    fun isSupported(name: String?) = isScreenSupported(name) || isOverlaySupported(name)

    fun isScreenSupported(name: String?) = name != null && metadata.screens.contains(name)

    fun isOverlaySupported(name: String?) = name != null && metadata.overlays.contains(name)

    override fun close() {
//        mc.textureManager.destroyTexture(loadedBackgroundImage)
    }

    companion object {

        fun extractFromResources(name: String) = ThemeManager.themesFolder.resolve(name).run {
            deleteOnExit()

            if (exists()) {
                deleteRecursively()
            }

            resource("/resources/liquidbounce/themes/$name.zip").use { stream ->
                extractZip(stream, this)
            }

            relativeTo(ThemeManager.themesFolder)
        }

    }

}

