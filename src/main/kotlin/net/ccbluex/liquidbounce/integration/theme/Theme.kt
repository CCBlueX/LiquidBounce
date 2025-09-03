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
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentFactory.JsonComponentFactory
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.shader.CanvasShader
import net.ccbluex.liquidbounce.utils.client.capitalize
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.resourceToString
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.util.*

/**
 * A web-based theme loaded from the provided URL.
 *
 * Can be local from [ClientInteropServer] or remote from the internet.
 */
class Theme(val origin: Origin, url: String) : BaseApi(url.removeSuffix("/")), Closeable {

    enum class Origin(override val choiceName: String) : NamedChoice {
        RESOURCE("resource"),
        LOCAL("local"),
        MARKETPLACE("marketplace"),
        REMOTE("remote")
    }

    constructor(url: String) : this(Origin.REMOTE, url)

    constructor(
        origin: Origin,
        file: File
    ) : this(origin, "${ClientInteropServer.url}/${origin.choiceName}/${file.invariantSeparatorsPath}/")

    val metadata: ThemeMetadata = runBlocking {
        try {
            get<ThemeMetadata>("/metadata.json")
        } catch (e: Exception) {
            logger.error("Failed to load theme metadata", e)
            throw IllegalStateException("Failed to load theme metadata", e)
        }
    }

    val components: MutableList<Component> = runBlocking {
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
        }.toMutableList()
    }

    val settings = Configurable(metadata.id.capitalize()).apply {
        metadata.values?.let { values ->
            for (value in values) {
                json(value)
            }
        }

        val componentSettings = Configurable("Components", components as MutableList<Value<*>>)
        tree(componentSettings)
    }

    init {
        metadata.checkNotNull()

        // Check for duplicated component names
        components.groupBy { component -> component.name }.forEach { (name, components) ->
            check(components.size == 1) { "Found duplicated component name '$name'" }
        }

        // Load fonts
        for (font in metadata.fonts) {
            runCatching {
                runBlocking {
                    get<InputStream>("/fonts/$font").use { stream ->
                        FontManager.queueFontFromStream(stream)
                    }

                    logger.info("Loaded font $font for theme ${metadata.name}")
                }
            }.onFailure {
                logger.warn("Failed to load font $font for theme ${metadata.name}", it)
            }
        }
    }

    var themeBackgroundShader: ThemeBackground? = null
        private set
    var themeBackgroundTexture: ThemeBackground? = null
        private set

    suspend fun compileShader(): Boolean {
        if (themeBackgroundShader != null) {
            return true
        }

        // todo: allow multiple backgrounds later on
        val background = metadata.backgrounds.firstOrNull() ?: return false
        if ("frag" !in background.types) {
            // not supported
            return false
        }

        val vertexShader = resourceToString("/resources/liquidbounce/shaders/vertex.vert")
        val fragmentShader = runCatching {
            get<String>("/backgrounds/${background.name.lowercase(Locale.US)}.frag")
        }.getOrNull() ?: return false

        themeBackgroundShader = ThemeBackground.shader(CanvasShader(
            vertexShader,
            fragmentShader,
        ))
        logger.info("Compiled shader background for theme ${metadata.name}")
        return true
    }

    suspend fun loadBackgroundImage(): Boolean {
        if (themeBackgroundTexture != null) {
            return true
        }

        // todo: allow multiple backgrounds later on
        val background = metadata.backgrounds.firstOrNull() ?: return false
        if ("png" !in background.types) {
            // not supported
            return false
        }

        val image = runCatching {
            get<NativeImageBackedTexture>("/backgrounds/${background.name}.png")
        }.getOrNull() ?: return false

        val id = Identifier.of("liquidbounce",
            "theme-bg-${metadata.name.lowercase(Locale.US)}")
        themeBackgroundTexture = ThemeBackground.image(id)
        mc.textureManager.registerTexture(id, image)
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
        themeBackgroundShader?.close()
        themeBackgroundTexture?.close()
    }

    override fun toString() = "Theme(name=${metadata.name}, origin=${origin.choiceName}, url=$baseUrl)"

}

