/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
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
    private val url: String,
    override val takesInput: () -> Boolean
) : ITab, InputAware {

    private val mcefBrowser: MCEFBrowser = MCEF.createBrowser(url, true,
        mc.window.width, mc.window.height)

    override var drawn = false
    override var preferOnTop = false

    override fun loadUrl(url: String) {
        mcefBrowser.loadURL(url)
    }

    override fun getUrl() = url
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
        mcefBrowser.sendMousePress(mouseX.toInt(), mouseY.toInt(), mouseButton)
        mcefBrowser.setFocus(true)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, mouseButton: Int) {
        mcefBrowser.sendMouseRelease(mouseX.toInt(), mouseY.toInt(), mouseButton)
        mcefBrowser.setFocus(true)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        mcefBrowser.sendMouseMove(mouseX.toInt(), mouseY.toInt())
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double) {
        mcefBrowser.sendMouseWheel(mouseX.toInt(), mouseY.toInt(), delta, 0)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) {
        mcefBrowser.sendKeyPress(keyCode, scanCode.toLong(), modifiers)
        mcefBrowser.setFocus(true)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int) {
        mcefBrowser.sendKeyRelease(keyCode, scanCode.toLong(), modifiers)
        mcefBrowser.setFocus(true)
    }

    override fun charTyped(codePoint: Char, modifiers: Int) {
        if (codePoint == 0.toChar()) {
            return
        }

        mcefBrowser.sendKeyTyped(codePoint, modifiers)
        mcefBrowser.setFocus(true)
    }

}
