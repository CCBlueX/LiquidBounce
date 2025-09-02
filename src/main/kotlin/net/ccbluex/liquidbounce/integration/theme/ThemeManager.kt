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
import net.ccbluex.liquidbounce.integration.theme.component.components.minimap.MinimapComponent
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import java.io.File

object ThemeManager : Configurable("theme") {

    internal val themesFolder = File(ConfigSystem.rootFolder, "themes")
    internal lateinit var liquidBounceTheme: Theme
        private set

    var theme: Theme? = null
        set(value) {
            val previousTheme = field
            field = value

            ModuleHud.components.inner.clear()
            ModuleHud.components.inner.add(MinimapComponent)
            ModuleHud.components.inner.addAll(value?.components.orEmpty())
            ModuleHud.components.initConfigurable()

            if (previousTheme == null) {
                return
            }

            // Update integration browser
            IntegrationListener.update()
            ModuleHud.reopen()
            ModuleClickGui.reload(true)
        }

    private val takesInputHandler = InputAcceptor { mc.currentScreen != null && mc.currentScreen !is ChatScreen }

    val themes: List<String>
        get() {
            val folderList = themesFolder.listFiles()
                ?.filter(File::isDirectory)
                ?.mapNotNull { it.name }
                ?: emptyList()

            val marketplaceItemList = MarketplaceManager.getSubscribedItemsOfType(MarketplaceItemType.THEME)
                .map { item -> "${item.id}_${item.name}" }

            return folderList + marketplaceItemList
        }

    var shaderEnabled by boolean("Shader", false)
        .onChange { enabled ->
            if (enabled) {
//                RenderSystem.recordRenderCall {
//                    activeTheme.compileShader()
//                    defaultTheme.compileShader()
//                }
            }

            return@onChange enabled
        }

    init {
        ConfigSystem.root(this)
    }

    fun load() {
        Theme("theme", Theme.extractFromResources("liquidbounce")).apply {
            liquidBounceTheme = this
            theme = this
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
        val theme = theme?.takeIf { theme ->
            virtualScreenType == null || theme.isSupported(virtualScreenType.routeName)
        } ?: liquidBounceTheme.takeIf { theme ->
            virtualScreenType == null || theme.isSupported(virtualScreenType.routeName)
        } ?: error("No theme supports the route ${virtualScreenType?.routeName}")

        return ScreenLocation(
            theme,
            theme.getUrl(virtualScreenType?.routeName, markAsStatic)
        )
    }

    fun initializeBackground() {
//        // Load background image of active theme and fallback to default theme if not available
//        if (!activeTheme.loadBackgroundImage()) {
//            defaultTheme.loadBackgroundImage()
//        }
//
//        // Compile shader of active theme and fallback to default theme if not available
//        if (shaderEnabled && !activeTheme.compileShader()) {
//            defaultTheme.compileShader()
//        }
    }

    @Suppress("LongParameterList")
    fun drawBackground(context: DrawContext, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float): Boolean {
//        if (shaderEnabled) {
//            val shader = activeTheme.compiledShaderBackground ?: defaultTheme.compiledShaderBackground
//
//            if (shader != null) {
//                shader.draw(mouseX, mouseY, delta)
//                return true
//            }
//        }
//
//        val image = activeTheme.loadedBackgroundImage ?: defaultTheme.loadedBackgroundImage
//        if (image != null) {
//            context.drawTexture(
//                RenderLayer::getGuiTextured,
//                image,
//                0,
//                0,
//                0f,
//                0f,
//                width,
//                height,
//                width,
//                height
//            )
//            return true
//        }

        return false
    }

    data class ScreenLocation(val theme: Theme, val url: String)

}

