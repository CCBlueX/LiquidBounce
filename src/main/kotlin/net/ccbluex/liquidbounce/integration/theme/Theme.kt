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
import net.ccbluex.liquidbounce.render.shader.CanvasShader
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.io.resource
import net.ccbluex.liquidbounce.utils.io.resourceToString
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
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

    var backgroundShader: CanvasShader? = null
        private set
    var backgroundTexture: Identifier? = null
        private set

    suspend fun compileShader(): Boolean {
        if (backgroundShader != null) {
            return true
        }

        val vertexShader = resourceToString("/resources/liquidbounce/shaders/vertex.vert")
        val fragmentShader = runCatching {
            get<String>("/background.frag")
        }.getOrNull() ?: return false

        backgroundShader = CanvasShader(
            vertexShader,
            fragmentShader,
        )
        logger.info("Compiled shader background for theme ${metadata.name}")
        return true
    }

    suspend fun loadBackgroundImage(): Boolean {
        if (backgroundTexture != null) {
            return true
        }

        val image = runCatching {
            get<NativeImageBackedTexture>("/background.png")
        }.getOrNull() ?: return false

        backgroundTexture = Identifier.of("liquidbounce",
            "theme-bg-${metadata.name.lowercase(Locale.US)}")
        mc.textureManager.registerTexture(backgroundTexture, image)
        logger.info("Loaded background image for theme ${metadata.name}")
        return true
    }

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
        mc.textureManager.destroyTexture(backgroundTexture)
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

