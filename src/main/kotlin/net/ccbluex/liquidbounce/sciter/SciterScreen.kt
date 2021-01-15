/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.sciter

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.sciter.natives.Sciter
import net.ccbluex.liquidbounce.utils.mc
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.LiteralText
import org.lwjgl.glfw.GLFWNativeWin32
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import java.io.File

class SciterScreen(name: String, val pausesGame: Boolean = true) : Screen(LiteralText(name)) {

    // todo: automatically download and load them
    private val NATIVES = File(LiquidBounce.configSystem.rootFolder, "natives")

    // compile from /src/native/
    val LIBRARY_WRAPPER_LOCATION = File(NATIVES, "sciter_wrapper.dll").absolutePath

    // download from https://sciter.com/download/
    val LIBRARY_SCITER_LOCATION = File(NATIVES, "sciter.dll").absolutePath

    init {
        System.load(LIBRARY_WRAPPER_LOCATION)
    }

    private val sciter: Sciter = Sciter(GLFWNativeWin32.glfwGetWin32Window(MinecraftClient.getInstance().window.handle))

    private var buttonStates = 0
    private var keyboardModifiers = 0

    init {
        sciter.init(LIBRARY_SCITER_LOCATION)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        sciter.mouseMoved(mouseX.toInt(), mouseY.toInt(), this.buttonStates)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        sciter.mouseButtonEvent(mouseX.toInt(), mouseY.toInt(), button + 1, false, 0)

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        sciter.mouseButtonEvent(mouseX.toInt(), mouseY.toInt(), button + 1, true, 0)

        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        sciter.mouseMoved(mouseX.toInt(), mouseY.toInt(), this.buttonStates)

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        this.keyboardModifiers = modifiers

        sciter.keyEvent(scanCode, modifiers, 0)

        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        this.keyboardModifiers = modifiers

        sciter.keyEvent(scanCode, modifiers, 1)

        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun charTyped(chr: Char, keyCode: Int): Boolean {
        sciter.keyEvent(chr.toInt(), keyboardModifiers, 2)

        return super.charTyped(chr, keyCode)
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        try {
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

//            sciter.draw();

//            sciter.draw();
            GL11.glMatrixMode(GL11.GL_PROJECTION)
            GL11.glPushMatrix()
            sciter.heartBit()
            sciter.render()
            GL11.glPopMatrix()

            GL13.glActiveTexture(GL13.GL_TEXTURE0) // Hey OpenGL, we're about to give commands for texture sampler 0.

            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sciter.textureID)
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
            GL11.glEnable(GL11.GL_ALPHA_TEST)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

            GL11.glBegin(GL11.GL_QUADS)

            GL11.glTexCoord2f(0.0f, 0.0f)
            GL11.glVertex2f(0f, 0f)
            GL11.glTexCoord2f(0.0f, 1.0f)
            GL11.glVertex2f(0f, this.height.toFloat())
            GL11.glTexCoord2f(1.0f, 1.0f)
            GL11.glVertex2f(this.width.toFloat(), this.height.toFloat())
            GL11.glTexCoord2f(1.0f, 0.0f)
            GL11.glVertex2f(this.width.toFloat(), 0f)

            GL11.glEnd()
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
//            GL11.glDisable(GL11.GL_TEXTURE_2D)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        super.render(matrices, mouseX, mouseY, delta)
    }

    override fun insertText(text: String?, override: Boolean) {
        super.insertText(text, override)
    }

    override fun init() {
        super.init()

        sciter.setSize(this.width, this.height)
        sciter.setResolution(mc.options.guiScale * 92 / 2)


        sciter.loadHtml(
            """<html lang="en"><head>
                        <meta charset="UTF-8">
                        <title>Hello</title>
                        <style>html, body { background: transparent; } </style>
                    </head>
                    <body>
                        <h1>Sciter Test</h1>

                        <ul>
                            <li>A</li>
                            <li>B</li>
                            <li>Lorem ipsum dolor sit amet</li>
                        </ul>

                        <button>Helo</button>

                        <input type="checkbox">
                        <input type="date">
                        <input type="email">
                        <input type="file">
                        <input type="number">
                        <input type="password">
                        <input type="text">
                    </body></html>""", null)
    }

    override fun isPauseScreen(): Boolean {
        return pausesGame
    }

    override fun resize(client: MinecraftClient?, width: Int, height: Int) {
        super.resize(client, width, height)

        sciter.setSize(this.width, this.height)
        sciter.setResolution(mc.options.guiScale * 92 / 2)
    }
}
