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
@Suppress("TooManyFunctions")
class Theme private constructor(val origin: Origin, url: String): BaseApi(url.removeSuffix("/")), Closeable {

    enum class Origin(override val choiceName: String) : NamedChoice {
        RESOURCE("resource"),
        LOCAL("local"),
        MARKETPLACE("marketplace"),
        REMOTE("remote")
    }

    var metadata: ThemeMetadata
        field: ThemeMetadata? = null
        private set
        get() = requireNotNull(field) { "metadata not loaded" }

    private suspend fun loadMetadata() {
        try {
            metadata = get<ThemeMetadata>("/metadata.json").apply { checkNotNull() }
        } catch (e: Exception) {
            logger.error("Failed to load theme metadata", e)
            throw IllegalStateException("Failed to load theme metadata", e)
        }
    }

    var components: MutableList<Component>
        field: MutableList<Component>? = null
        private set
        get() = requireNotNull(field) { "components not loaded" }

    var settings: Configurable
        field: Configurable? = null
        private set
        get() = requireNotNull(field) { "settings not loaded" }

    private suspend fun loadComponents() {
        components = metadata.components.mapNotNullTo(mutableListOf()) { name ->
            val componentFactory = runCatching {
                get<JsonComponentFactory>("/components/${name.lowercase(Locale.US)}.json")
            }.onFailure {
                logger.warn("Failed to load component $name", it)
            }.getOrNull() ?: return@mapNotNullTo null

            runCatching {
                componentFactory.createComponent()
            }.onFailure {
                logger.warn("Failed to create component $name", it)
            }.getOrNull()
        }

        // Check for duplicated component names
        components.groupingBy { component -> component.name }.eachCount().forEach { (name, count) ->
            check(count == 1) { "Found duplicated component name '$name'" }
        }

        settings = Configurable(metadata.id.capitalize()).apply {
            metadata.values?.let { values ->
                for (value in values) {
                    json(value)
                }
            }

            @Suppress("UNCHECKED_CAST")
            val componentSettings = Configurable("Components", components as MutableList<Value<*>>)
            tree(componentSettings)
        }
    }

    private suspend fun loadFonts() {
        for (font in metadata.fonts) {
            runCatching {
                get<InputStream>("/fonts/$font").use { stream ->
                    FontManager.queueFontFromStream(stream)
                }

                logger.info("Loaded font $font for theme ${metadata.name}")
            }.onFailure {
                logger.warn("Failed to load font $font for theme ${metadata.name}", it)
            }
        }
    }

    private suspend fun loadAll() = apply {
        loadMetadata()
        loadComponents()
        loadFonts()
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

    companion object {
        @JvmStatic
        suspend fun load(url: String) = Theme(Origin.REMOTE, url).loadAll()

        @JvmStatic
        suspend fun load(origin: Origin, file: File) = Theme(
            origin,
            url = "${ClientInteropServer.url}/${origin.choiceName}/${file.invariantSeparatorsPath}/"
        ).loadAll()
    }

}

