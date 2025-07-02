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
package net.ccbluex.liquidbounce.integration.browser.impl.cef

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.integration.browser.BrowserManager
import net.ccbluex.liquidbounce.integration.browser.BrowserTexture
import net.ccbluex.liquidbounce.integration.browser.tab.InputAware
import net.ccbluex.liquidbounce.integration.browser.tab.Tab
import net.ccbluex.liquidbounce.integration.browser.tab.TabPosition
import net.ccbluex.liquidbounce.mcef.MCEF
import net.ccbluex.liquidbounce.mcef.cef.MCEFBrowser
import net.ccbluex.liquidbounce.mcef.cef.MCEFBrowserSettings
import net.minecraft.client.texture.AbstractTexture
import net.minecraft.util.Identifier

@Suppress("TooManyFunctions")
class JcefTab(
    private val jcefBrowser: JcefBrowser,
    url: String,
    position: TabPosition,
    frameRate: Int = 60,
    override val takesInput: () -> Boolean
) : Tab, InputAware, MinecraftShortcuts {

    override var position: TabPosition = position
        set(value) {
            field = value

            mcefBrowser.resize(
                value.width.coerceAtLeast(1),
                value.height.coerceAtLeast(1)
            )
        }
    override var visible = true

    private val mcefBrowser: MCEFBrowser = MCEF.INSTANCE.createBrowser(
        url,
        true,
        position.width.coerceAtLeast(1),
        position.height.coerceAtLeast(1),
        MCEFBrowserSettings(
            frameRate,
            BrowserManager.accelerated
        )
    ).apply {
        // Force zoom level to 1.0 to prevent users from adjusting the zoom level
        // this was possible in earlier versions of MCEF
        zoomLevel = 1.0
    }

    private val texture = Identifier.of("liquidbounce", "browser/tab/${mcefBrowser.hashCode()}")

    override var drawn = false
    override var preferOnTop = false

    init {
        mc.textureManager.registerTexture(texture, object : AbstractTexture() {
            override fun getGlId() = mcefBrowser.renderer.textureID
        })
    }

    override fun forceReload() {
        mcefBrowser.reloadIgnoreCache()
    }

    override fun reload() {
        mcefBrowser.reload()
    }

    override fun goForward() {
        mcefBrowser.goForward()
    }

    override fun goBack() {
        mcefBrowser.goBack()
    }

    override fun loadUrl(url: String) {
        mcefBrowser.loadURL(url)
    }

    override fun getUrl(): String = mcefBrowser.url

    override fun closeTab() {
        jcefBrowser.removeTab(this)
        mcefBrowser.close()
        mc.textureManager.destroyTexture(texture)
    }

    override fun getTexture(): BrowserTexture? {
        if (mcefBrowser.renderer.isUnpainted) {
            return null
        }

        return BrowserTexture(
            mcefBrowser.renderer.textureID,
            texture,
            position.height,
            position.width,
            mcefBrowser.renderer.isBGRA
        )
    }

    override fun resize(width: Int, height: Int) {
        if (!position.fullScreen) {
            return
        }

        position = position.copy(width = width, height = height)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        mcefBrowser.setFocus(true)
        mcefBrowser.sendMousePress(mouseX.toInt(), mouseY.toInt(), mouseButton)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, mouseButton: Int) {
        mcefBrowser.setFocus(true)
        mcefBrowser.sendMouseRelease(mouseX.toInt(), mouseY.toInt(), mouseButton)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        mcefBrowser.sendMouseMove(mouseX.toInt(), mouseY.toInt())
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double) {
        mcefBrowser.sendMouseWheel(mouseX.toInt(), mouseY.toInt(), delta)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) {
        mcefBrowser.setFocus(true)
        mcefBrowser.sendKeyPress(keyCode, scanCode.toLong(), modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int) {
        mcefBrowser.setFocus(true)
        mcefBrowser.sendKeyRelease(keyCode, scanCode.toLong(), modifiers)
    }

    override fun charTyped(char: Char, modifiers: Int) {
        mcefBrowser.setFocus(true)
        mcefBrowser.sendKeyTyped(char, modifiers)
    }

}
