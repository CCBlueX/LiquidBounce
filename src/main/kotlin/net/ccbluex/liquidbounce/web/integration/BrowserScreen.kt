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
package net.ccbluex.liquidbounce.web.integration

import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.refreshRate
import net.ccbluex.liquidbounce.web.browser.BrowserManager.browser
import net.ccbluex.liquidbounce.web.browser.supports.tab.ITab
import net.ccbluex.liquidbounce.web.browser.supports.tab.TabMargin
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

class BrowserScreen(var url: String, title: Text = "".asText()) : Screen(title) {

    private var jcef: ITab? = null

    private lateinit var inputField: TextFieldWidget

    override fun init() {
        inputField = addDrawableChild(TextFieldWidget(mc.textRenderer, 5, height - 22, width - 10,
            20, Text.literal(url)))
        inputField.setMaxLength(1337)
        inputField.setPlaceholder(Text.literal("URL"))

        if (jcef != null) {
            return
        }

        jcef = browser?.createInputAwareTab(url, refreshRate, TabMargin(bottom = 25)) { mc.currentScreen == this }
            ?.preferOnTop()
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        // render nothing
        if (!inputField.isFocused) {
            inputField.text = jcef?.getUrl()
        }
        super.render(context, mouseX, mouseY, delta)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (url != inputField.text) {
                url = inputField.text
                jcef?.loadUrl(url)
                inputField.isFocused = false
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun shouldPause() = false

    override fun close() {
        jcef?.closeTab()
        jcef = null
        super.close()
    }

    override fun shouldCloseOnEsc() = true

}
