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
package net.ccbluex.liquidbounce.integration.backend.impl.cef

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.BrowserTexture
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserRenderer
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import net.ccbluex.liquidbounce.integration.backend.input.InputAcceptor
import net.ccbluex.liquidbounce.integration.backend.input.InputHandler
import net.ccbluex.liquidbounce.integration.backend.input.InputListener
import net.ccbluex.liquidbounce.mcef.MCEF
import net.ccbluex.liquidbounce.mcef.cef.MCEFBrowser
import net.ccbluex.liquidbounce.mcef.cef.MCEFBrowserSettings
import net.minecraft.client.texture.AbstractTexture
import net.minecraft.util.Identifier

@Suppress("TooManyFunctions")
class JcefBrowser(
    private val backend: JcefBrowserBackend,
    url: String,
    viewport: BrowserViewport,
    val settings: BrowserSettings,
    inputAcceptor: InputAcceptor? = null
) : Browser, InputHandler, MinecraftShortcuts {

    override var viewport: BrowserViewport = viewport
        set(value) {
            field = value

            val quality = settings.quality
            val (scaledWidth, scaledHeight) = value.getScaledDimensions(quality)

            mcefBrowser.resize(scaledWidth, scaledHeight)
            mcefBrowser.zoomLevel = value.getZoomLevel(quality)
        }
    override var visible = true

    private val mcefBrowser: MCEFBrowser = MCEF.INSTANCE.createBrowser(
        url,
        true,
        viewport.getScaledDimensions(settings.quality).first,
        viewport.getScaledDimensions(settings.quality).second,
        MCEFBrowserSettings(
            settings.currentFps,
            settings.accelerated?.get() == true
        )
    )

    private val renderer = BrowserRenderer(this)
    private val inputListener: InputListener? = inputAcceptor?.let { inputChecker ->
        InputListener(this, this, inputAcceptor)
    }

    init {
        mcefBrowser.zoomLevel = viewport.getZoomLevel(settings.quality)
    }

    private val textureId = Identifier.of("liquidbounce", "browser/tab/${mcefBrowser.hashCode()}")

    override var rendered = false
    override var renderOnTop = false

    override var url: String
        get() = mcefBrowser.url
        set(value) {
            mcefBrowser.loadURL(value)
        }

    override val texture: BrowserTexture?
        get() {
            if (mcefBrowser.renderer.isUnpainted) {
                return null
            }

            return BrowserTexture(
                mcefBrowser.renderer.textureID,
                textureId,
                viewport.height,
                viewport.width,
                mcefBrowser.renderer.isBGRA
            )
        }

    init {
        mc.textureManager.registerTexture(textureId, object : AbstractTexture() {
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

    override fun close() {
        renderer.close()
        inputListener?.close()
        backend.removeBrowser(this)
        mcefBrowser.close()
        mc.textureManager.destroyTexture(textureId)
    }

    override fun update(width: Int, height: Int) {
        if (!viewport.fullScreen) {
            return
        }

        viewport = viewport.copy(width = width, height = height)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        mcefBrowser.setFocus(true)
        val (scaledX, scaledY) = viewport.transformMouse(mouseX, mouseY, settings.quality)
        mcefBrowser.sendMousePress(scaledX, scaledY, mouseButton)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, mouseButton: Int) {
        mcefBrowser.setFocus(true)
        val (scaledX, scaledY) = viewport.transformMouse(mouseX, mouseY, settings.quality)
        mcefBrowser.sendMouseRelease(scaledX, scaledY, mouseButton)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val (scaledX, scaledY) = viewport.transformMouse(mouseX, mouseY, settings.quality)
        mcefBrowser.sendMouseMove(scaledX, scaledY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double) {
        val (scaledX, scaledY) = viewport.transformMouse(mouseX, mouseY, settings.quality)
        mcefBrowser.sendMouseWheel(scaledX, scaledY, delta)
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
