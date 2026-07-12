/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import com.google.gson.JsonObject
import com.mojang.blaze3d.platform.NativeImage
import io.netty.handler.codec.http.HttpHeaderNames
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.BaseApi
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ThemeColorChangeEvent
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.interop.middleware.AuthMiddleware
import net.ccbluex.liquidbounce.integration.theme.component.HudComponent
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentFactory
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentFactory.JsonHudComponentFactory
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.text.capitalize
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import okhttp3.Headers.Companion.headersOf
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.util.Locale

/**
 * A web-based theme loaded from the provided URL.
 *
 * Can be local from [ClientInteropServer] or remote from the internet.
 */
@Suppress("TooManyFunctions")
class Theme private constructor(val origin: Origin, url: String) :
    BaseApi(
        url.trimEnd('/'),
        defaultHeaders = headersOf(
            HttpHeaderNames.COOKIE.toString(),
            "${AuthMiddleware.AUTH_COOKIE_NAME}=${ClientInteropServer.AUTH_CODE}",
        )
    ), Closeable, ResourceManagerReloadListener {

    enum class Origin(override val tag: String, val external: Boolean) : Tagged {
        RESOURCE("resource", false),
        LOCAL("local", false),
        MARKETPLACE("marketplace", false),
        REMOTE("remote", true)
    }

    private var _metadata: ThemeMetadata? = null
    val metadata: ThemeMetadata
        get() = requireNotNull(_metadata) { "metadata not loaded" }

    private suspend fun loadMetadata() {
        try {
            _metadata = get<ThemeMetadata>("/metadata.json").apply { checkNotNull() }
        } catch (e: Exception) {
            logger.error("Failed to load theme metadata", e)
            throw IllegalStateException("Failed to load theme metadata", e)
        }
    }

    private var componentFactories: Map<String, HudComponentFactory>? = null
    private var componentSettings: ValueGroup? = null
    val components: List<HudComponent>
        get() = requireNotNull(componentSettings) { "components not loaded" }
            .inner.filterIsInstance<HudComponent>()

    private var _settings: ValueGroup? = null
    val settings: ValueGroup
        get() = requireNotNull(_settings) { "settings not loaded" }

    private var _colors: ValueGroup? = null
    val colors: ValueGroup
        get() = requireNotNull(_colors) { "colors not loaded" }

    private suspend fun loadComponents() {
        val componentFactoryList = metadata.components.mapNotNull { name ->
            runCatching {
                get<JsonHudComponentFactory>("/components/${name.lowercase(Locale.US)}.json")
            }.onFailure {
                logger.warn("Failed to load component $name", it)
            }.getOrNull()
        }

        componentFactories = buildMap {
            for (factory in componentFactoryList) {
                // Check for duplicated component names
                check(this.put(factory.name, factory) == null) {
                    "Found duplicated component name '${factory.name}'"
                }
            }
        }

        val initialComponents = requireNotNull(componentFactories).values.mapNotNull { factory ->
            createComponent(factory)
        }

        _settings = ValueGroup(metadata.id.capitalize()).apply {
            _colors = ValueGroup("Colors")
            metadata.colors?.let { values ->
                for ((name, value) in values) {
                    val color4b = Color4b.fromHex(value)
                    colors.color(name, color4b).apply {
                        onChanged { color ->
                            EventManager.callEvent(ThemeColorChangeEvent(metadata.id, name, color))
                        }
                    }
                }
            }
            tree(colors)

            metadata.values?.let { values ->
                for (value in values) {
                    json(value)
                }
            }

            componentSettings = ComponentSettings().also { settings ->
                initialComponents.forEach(settings::tree)
                tree(settings)
            }
        }
    }

    private fun createComponent(factory: HudComponentFactory): HudComponent? = runCatching {
        factory.createComponent()
    }.onFailure {
        logger.warn("Failed to create component ${factory.name}", it)
    }.getOrNull()

    private fun registerComponent(component: HudComponent) {
        val settings = requireNotNull(componentSettings)
        settings.tree(component)
        component.walkInit()
        settings.key?.let(component::walkKeyPath)
    }

    fun addComponent(sourceId: String): HudComponent? {
        val source = components.find { it.id.toString() == sourceId } ?: return null
        val factory = requireNotNull(componentFactories)[source.name] ?: return null

        if (factory.singleton && components.any { it.name == source.name && it.enabled }) {
            return null
        }

        val disabledComponent = components.find { it.name == source.name && !it.enabled }
        val component = disabledComponent
            ?: createComponent(factory)?.also(::registerComponent)
            ?: return null

        if (!factory.singleton && disabledComponent != null) {
            component.restore()
        }

        component.enabled = true
        return component
    }

    fun componentCatalog(): List<ComponentCatalogEntry> = requireNotNull(componentFactories).values
        .mapNotNull { factory ->
            val source = components.firstOrNull { it.name == factory.name } ?: return@mapNotNull null
            ComponentCatalogEntry(
                source.name,
                source.componentDescription,
                source.id.toString(),
                factory.singleton,
                !factory.singleton || components.none { it.name == factory.name && it.enabled },
            )
        }

    private inner class ComponentSettings : ValueGroup("Components") {
        override fun prepareDeserialize(jsonObject: JsonObject) {
            val existingCounts = Object2IntOpenHashMap<String>()
            components.forEach {
                existingCounts.addTo(it.name, 1)
            }

            for (storedComponent in jsonObject.getAsJsonArray("value")) {
                val name = storedComponent.asJsonObject["name"].asString
                val remaining = existingCounts.getOrDefault(name, 0)
                if (remaining > 0) {
                    existingCounts.put(name, remaining - 1)
                    continue
                }

                val factory = requireNotNull(componentFactories)[name] ?: continue
                if (factory.singleton) {
                    continue
                }
                createComponent(factory)?.let(::registerComponent)
            }
        }
    }

    data class ComponentCatalogEntry(
        val name: String,
        val description: String,
        val id: String,
        val singleton: Boolean,
        val canAdd: Boolean,
    )

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

    var backgroundShader: ThemeBackground? = null
        private set
    private val shaderMutex = Mutex()
    var backgroundImage: ThemeBackground? = null
        private set
    private val imageMutex = Mutex()

    suspend fun compileShader(): Boolean = shaderMutex.withLock {
        if (backgroundShader != null) {
            return true
        }

        // todo: allow multiple backgrounds later on
        val background = metadata.backgrounds.firstOrNull() ?: return false
        if ("frag" !in background.types) {
            // not supported
            return false
        }

        val fragmentShader = runCatching {
            get<String>("/backgrounds/${background.name.lowercase(Locale.US)}.frag")
        }.getOrNull() ?: return false

        withContext(Dispatchers.Minecraft) {
            backgroundShader = ThemeBackground.Shader.build(
                metadata,
                background,
                fragmentShader,
            ).also {
                it.onResourceReload()
            }
        }

        logger.info("Compiled shader background for theme ${metadata.name}")
        return true
    }

    suspend fun loadBackgroundImage(): Boolean = imageMutex.withLock {
        if (backgroundImage != null) {
            return true
        }

        // todo: allow multiple backgrounds later on
        val background = metadata.backgrounds.firstOrNull() ?: return false
        if ("png" !in background.types) {
            // not supported
            return false
        }

        val image = runCatching {
            get<NativeImage>("/backgrounds/${background.name}.png")
        }.getOrNull() ?: return false

        withContext(Dispatchers.Minecraft) {
            backgroundImage = ThemeBackground.Image(metadata, image).also {
                it.onResourceReload()
            }
        }
        logger.info("Loaded background image for theme ${metadata.name}")
        return true
    }

    /**
     * Get the URL to the given page name in the theme.
     */
    fun getUrl(name: String? = null, markAsStatic: Boolean = false): String {
        val baseUrlWithFragment = "$baseUrl/?${AuthMiddleware.AUTH_CODE_PARAM}=" +
            "${ClientInteropServer.AUTH_CODE}#/${name.orEmpty()}"
        val params = buildList {
            if (origin.external) add("port=${ClientInteropServer.PORT}")
            if (markAsStatic) add("static")
        }.joinToString("&")

        return if (params.isNotEmpty()) "$baseUrlWithFragment?$params" else baseUrlWithFragment
    }

    fun isSupported(name: String?) = isScreenSupported(name) || isOverlaySupported(name)

    fun isScreenSupported(name: String?) = name != null && metadata.screens.contains(name)

    fun isOverlaySupported(name: String?) = name != null && metadata.overlays.contains(name)

    override fun onResourceManagerReload(manager: ResourceManager) {
        backgroundShader?.onResourceReload()
        backgroundImage?.onResourceReload()
        logger.info("Reloaded theme '${metadata.name}'.")
    }

    override fun close() {
        backgroundShader?.close()
        backgroundImage?.close()
        componentSettings?.inner?.filterIsInstance<HudComponent>()?.forEach(EventManager::unregisterEventHandler)
    }

    override fun toString() = "Theme(name=${metadata.name}, origin=${origin.tag}, url=$baseUrl)"

    companion object {

        private val logger = clientLogger("Theme")

        @JvmStatic
        suspend fun load(url: String) = Theme(Origin.REMOTE, url).loadAll()

        @JvmStatic
        suspend fun load(origin: Origin, file: File) = Theme(
            origin,
            url = "${ClientInteropServer.url}/${origin.tag}/${file.invariantSeparatorsPath}/"
        ).loadAll()
    }

}
