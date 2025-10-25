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
 *
 */
package net.ccbluex.liquidbounce.integration.theme

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.renderScope
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.input.InputAcceptor
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import java.io.File

object ThemeManager : Configurable("theme") {

    internal val themesFolder = File(ConfigSystem.rootFolder, "themes")

    val themes = mutableListOf<Theme>()
    val themeIds get() = themes.map { theme -> theme.metadata.id }

    private var currentTheme by text("Theme", "liquidbounce").onChanged {
        // Update integration browser
        mc.execute {
            IntegrationListener.update()
            ModuleHud.reopen()
            ModuleClickGui.reload(true)
        }
    }

    internal lateinit var includedTheme: Theme
        private set
    /**
     * Used for development.
     */
    private var temporaryTheme: Theme? = null

    var theme: Theme
        get() = temporaryTheme
            ?: themes.find { theme -> theme.metadata.id.equals(currentTheme, true) }
            ?: includedTheme
        set(value) {
            // When external, set as a temporary theme.
            if (value.origin.external) {
                temporaryTheme = value
                currentTheme = includedTheme.metadata.id
            } else {
                temporaryTheme = null
                currentTheme = value.metadata.id
            }
        }

    private val takesInputHandler = InputAcceptor { mc.currentScreen != null && mc.currentScreen !is ChatScreen }

    var shaderEnabled by boolean("Shader", false)
        .onChange { enabled ->
            if (enabled) {
                renderScope.launch {
                    theme.compileShader()
                    includedTheme.compileShader()
                }
            }

            return@onChange enabled
        }

    init {
        ConfigSystem.root(this)
    }

    suspend fun init() {
        // Load default theme
        includedTheme = Theme.load(Theme.Origin.RESOURCE, File("liquidbounce"))
    }

    suspend fun load() {
        fun Theme.addIfUnloaded() {
            if (themes.none { it.metadata.id.equals(this.metadata.id, true) }) {
                themes += this
            } else {
                logger.warn("Theme with ID '${this.metadata.id}' is already loaded, skipping duplicate.")
            }
        }

        themes.clear()

        // 1st priority
        themesFolder.listFiles { it.isDirectory }
            ?.forEach { file ->
                if (file.name.equals("default", true)) {
                    return@forEach
                }

                runCatching {
                    Theme.load(Theme.Origin.LOCAL, file.relativeTo(themesFolder))
                        .addIfUnloaded()
                }.onFailure { err ->
                    logger.error("Failed to load theme '${file.name}'.", err)
                }
            }

        // 2nd priority
        MarketplaceManager.getSubscribedItemsOfType(MarketplaceItemType.THEME).forEach { item ->
            runCatching {
                val installationFolder = item.getInstallationFolder() ?: return@forEach
                val relativeFile = installationFolder.relativeTo(MarketplaceManager.marketplaceRoot)
                Theme.load(Theme.Origin.MARKETPLACE, relativeFile)
                    .addIfUnloaded()
            }.onFailure { err ->
                logger.error("Failed to load theme '${item.name}'.", err)
            }
        }

        themes.add(includedTheme)

        ModuleHud.updateThemes()
        if (LiquidBounce.isInitialized) {
            IntegrationListener.update()
            ModuleHud.reopen()
            ModuleClickGui.reload(true)
        }
    }

    /**
     * Open [Browser] with the given [VirtualScreenType] and mark as static if [markAsStatic] is true.
     * This tab will be locked to 60 FPS since it is not input aware.
     */
    fun openImmediate(
        virtualScreenType: VirtualScreenType? = null,
        markAsStatic: Boolean = false,
        settings: BrowserSettings
    ): Browser =
        BrowserBackendManager.browserBackend.createBrowser(
            getScreenLocation(virtualScreenType, markAsStatic).url,
            settings = settings
        )

    /**
     * Open [Browser] with the given [VirtualScreenType] and mark as static if [markAsStatic] is true.
     * This tab will be locked to the highest refresh rate since it is input aware.
     */
    fun openInputAwareImmediate(
        virtualScreenType: VirtualScreenType? = null,
        markAsStatic: Boolean = false,
        settings: BrowserSettings,
        priority: Short = 10,
        inputAcceptor: InputAcceptor = takesInputHandler
    ): Browser = BrowserBackendManager.browserBackend.createBrowser(
        getScreenLocation(virtualScreenType, markAsStatic).url,
        settings = settings,
        priority = priority,
        inputAcceptor = inputAcceptor
    )

    fun updateImmediate(
        browser: Browser?,
        virtualScreenType: VirtualScreenType? = null,
        markAsStatic: Boolean = false
    ) {
        browser?.url = getScreenLocation(virtualScreenType, markAsStatic).url
    }

    fun getScreenLocation(virtualScreenType: VirtualScreenType? = null, markAsStatic: Boolean = false): ScreenLocation {
        val theme = theme.takeIf { theme ->
            virtualScreenType == null || theme.isSupported(virtualScreenType.routeName)
        } ?: includedTheme.takeIf { theme ->
            virtualScreenType == null || theme.isSupported(virtualScreenType.routeName)
        } ?: error("No theme supports the route ${virtualScreenType?.routeName}")

        return ScreenLocation(
            theme,
            theme.getUrl(virtualScreenType?.routeName, markAsStatic)
        )
    }

    fun loadBackground() = runBlocking {
        theme.loadBackgroundImage()
        if (shaderEnabled) {
            theme.compileShader()
        }
    }

    @Suppress("LongParameterList")
    fun drawBackground(context: DrawContext, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float): Boolean {
        val background = if (shaderEnabled) {
            theme.themeBackgroundShader
        } else {
            theme.themeBackgroundTexture
        } ?: return false

        background.draw(context, width, height, mouseX, mouseY, delta)
        return true
    }

    data class ScreenLocation(val theme: Theme, val url: String)

}

