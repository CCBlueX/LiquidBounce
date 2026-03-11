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
package net.ccbluex.liquidbounce.integration.backend.backends.external

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.integration.backend.BrowserTexture
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserState
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import net.ccbluex.liquidbounce.utils.client.browseUrl

@Suppress("TooManyFunctions")
class ExternalSystemBrowser(
    private val backend: ExternalSystemBrowserBackend,
    url: String,
    viewport: BrowserViewport,
    val settings: BrowserSettings,
    override var priority: Short = 0
) : Browser, MinecraftShortcuts {

    override val isInitialized = true
    override val state: BrowserState = BrowserState.Stateless
    override var viewport: BrowserViewport = viewport
    override var visible = true

    init {
        browseUrl(url)
    }

    override var url: String = url
        set(value) {
            field = value
            browseUrl(value)
        }

    override val texture: BrowserTexture? = null

    @Suppress("EmptyFunctionBlock")
    override fun forceReload() {
    }

    @Suppress("EmptyFunctionBlock")
    override fun reload() {
    }

    @Suppress("EmptyFunctionBlock")
    override fun goForward() {
    }

    @Suppress("EmptyFunctionBlock")
    override fun goBack() {
    }

    override fun close() {
        backend.removeBrowser(this)
    }

    override fun update(width: Int, height: Int) {
        if (!viewport.fullScreen) {
            return
        }

        viewport = viewport.copy(width = width, height = height)
    }

    @Suppress("EmptyFunctionBlock")
    override fun invalidate() {
    }

    override fun toString() = "ExternalBrowser(url='$url', viewport=$viewport, visible=$visible, priority=$priority)"

}
