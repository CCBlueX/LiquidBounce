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
 */
package net.ccbluex.liquidbounce.web.browser.supports.tab

import net.ccbluex.liquidbounce.mcef.MCEF
import net.ccbluex.liquidbounce.mcef.MCEFBrowser
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.browser.supports.JcefBrowser
import net.minecraft.client.render.GameRenderer

@Suppress("TooManyFunctions")
class JcefTab(
    private val jcefBrowser: JcefBrowser,
    url: String,
    frameRate: Int = 60,
    override val takesInput: () -> Boolean
) : ITab, InputAware {

    private val mcefBrowser: MCEFBrowser = MCEF.INSTANCE.createBrowser(
        url, true, mc.window.width, mc.window.height, frameRate
    ).apply {
        // Force zoom level to 1.0 to prevent users from adjusting the zoom level
        // this was possible in earlier versions of MCEF
        zoomLevel = 1.0
    }

    override var drawn = false
    override var preferOnTop = false

    override fun forceReload() {
        mcefBrowser.reloadIgnoreCache()
    }

    override fun loadUrl(url: String) {
        mcefBrowser.loadURL(url)
    }

    override fun getUrl() = mcefBrowser.getURL()

    override fun closeTab() {
        mcefBrowser.close()
        jcefBrowser.removeTab(this)
    }

    override fun getTexture() = mcefBrowser.renderer.textureID
    override fun getShader() = GameRenderer.getPositionTexColorProgram()
    override fun resize(width: Int, height: Int) {
        if (width <= 100 || height <= 100) {
            return
        }

        mcefBrowser.resize(width, height)
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

    override fun charTyped(codePoint: Char, modifiers: Int) {
        mcefBrowser.setFocus(true)
        mcefBrowser.sendKeyTyped(codePoint, modifiers)
    }

}
