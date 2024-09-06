/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.web.theme

import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.io.resource
import net.ccbluex.liquidbounce.utils.render.refreshRate
import net.ccbluex.liquidbounce.web.browser.BrowserManager
import net.ccbluex.liquidbounce.web.integration.DrawerReference
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.theme.type.RouteType
import net.ccbluex.liquidbounce.web.theme.type.Theme
import net.ccbluex.liquidbounce.web.theme.type.native.NativeDrawer
import net.ccbluex.liquidbounce.web.theme.type.native.NativeTheme
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import java.io.File

object ThemeManager : Configurable("theme") {

    val themesFolder = File(ConfigSystem.rootFolder, "themes")

    init {
//        extractDefault()
    }

    val availableThemes = arrayOf(
        NativeTheme,
//        *themesFolder.listFiles()
//            ?.filter(File::isDirectory)
//            ?.map(::WebTheme)
//            ?.toTypedArray()
//            ?: emptyArray()
    )

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

    var activeTheme: Theme = availableThemes.firstOrNull { it.name == "default" } ?: NativeTheme
        set(value) {
//            if (!value.exists) {
//                logger.warn("Unable to set theme to ${value.name}, theme does not exist")
//                return
//            }

            field = value

            // Update integration browser
            IntegrationHandler.updateIntegrationBrowser()
            ModuleHud.refresh()
        }

    private val takesInputHandler: () -> Boolean
        get() = { mc.currentScreen != null && mc.currentScreen !is ChatScreen }

    init {
        ConfigSystem.root(this)
    }

    /**
     * Open [DrawerReference] with the given [VirtualScreenType]
     * This tab will be locked to 60 FPS since it is not input aware.
     */
    fun openImmediate(virtualScreenType: VirtualScreenType? = null) =
        when (val route = route(virtualScreenType)) {
            is RouteType.Web -> DrawerReference.Web(
                BrowserManager.browser?.createTab(
                    route.url,
                    frameRate = 60
                ) ?: error("Browser is not initialized")
            )

            is RouteType.Native -> {
                NativeDrawer.select(route.drawableRoute)
                DrawerReference.Native(NativeDrawer)
            }
        }

    /**
     * Open [DrawerReference] with the given [VirtualScreenType]
     * This tab will be locked to the highest refresh rate since it is input aware.
     */
    fun openInputAwareImmediate(virtualScreenType: VirtualScreenType? = null) =
        when (val route = route(virtualScreenType)) {
            is RouteType.Web -> DrawerReference.Web(
                BrowserManager.browser?.createInputAwareTab(
                    route.url,
                    frameRate = refreshRate,
                    takesInput = takesInputHandler
                ) ?: error("Browser is not initialized")
            )
            is RouteType.Native -> {
                NativeDrawer.select(route.drawableRoute)
                DrawerReference.Native(NativeDrawer)
            }
        }

    fun updateImmediate(ref: DrawerReference, virtualScreenType: VirtualScreenType? = null) =
        when (val route = route(virtualScreenType)) {
            is RouteType.Web -> when (ref) {
                is DrawerReference.Web -> ref.browser.loadUrl(route.url)
                is DrawerReference.Native -> error("Unable to update tab, drawer reference is not a web tab")
            }
            is RouteType.Native -> when (ref) {
                is DrawerReference.Native -> NativeDrawer.select(route.drawableRoute)
                is DrawerReference.Web -> error("Unable to update tab, drawer reference is not a native tab")
            }
        }

    fun route(virtualScreenType: VirtualScreenType? = null): RouteType {
        val theme = if (virtualScreenType == null || activeTheme.doesAccept(virtualScreenType)) {
            activeTheme
        } else {
            availableThemes.firstOrNull { theme -> theme.doesAccept(virtualScreenType) }
                ?: error("No theme supports the route ${virtualScreenType.routeName}")
        }

        return theme.route(virtualScreenType)
    }

    fun initialiseBackground() {
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

    fun drawBackground(context: DrawContext, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float): Boolean {
//        if (shaderEnabled) {
//            val shader = activeTheme.compiledShaderBackground ?: defaultTheme.compiledShaderBackground
//
//            if (shader != null) {
//                shader.draw(mouseX, mouseY, width, height, delta)
//                return true
//            }
//        }
//
//        val image = activeTheme.loadedBackgroundImage ?: defaultTheme.loadedBackgroundImage
//        if (image != null) {
//            context.drawTexture(image, 0, 0, 0f, 0f, width, height, width, height)
//            return true
//        }

        return false
    }

    fun chooseTheme(name: String) {
        activeTheme = availableThemes.firstOrNull { it.name == name }
            ?: error("Theme $name does not exist")
    }

    fun themes() = themesFolder.listFiles()?.filter { it.isDirectory }?.mapNotNull { it.name } ?: emptyList()

    /**
     * Extract the default theme from the resources.
     */
    private fun extractDefault() {
        runCatching {
            val folder = themesFolder.resolve("default")
            val stream = resource("/assets/liquidbounce/default_theme.zip")

            if (folder.exists()) {
                folder.deleteRecursively()
            }

            extractZip(stream, folder)
            folder.deleteOnExit()

            logger.info("Extracted default theme")
        }.onFailure {
            logger.error("Unable to extract default theme", it)
        }.onSuccess {
            logger.info("Successfully extracted default theme")
        }.getOrThrow()
    }

}



data class ThemeMetadata(
    val name: String,
    val author: String,
    val version: String,
    val supports: List<String>,
    val overlays: List<String>,
    @SerializedName("components")
    val rawComponents: JsonArray
)
