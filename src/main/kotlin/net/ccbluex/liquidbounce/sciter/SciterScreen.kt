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
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.LiteralText
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWNativeWin32
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import kotlin.concurrent.thread


object SciterWindow {

    // todo: automatically download and load them
    private val NATIVES = File(LiquidBounce.configSystem.rootFolder, "natives")
    // compile from /src/native/
    val LIBRARY_WRAPPER_LOCATION = File(NATIVES,"sciter_wrapper.dll").absolutePath
    // download from https://sciter.com/download/
    val LIBRARY_SCITER_LOCATION = File(NATIVES, "sciter.dll").absolutePath

    init {
        System.load(LIBRARY_WRAPPER_LOCATION)
    }

    lateinit var sciter: Sciter
    private var window = 0L

    val sciterThread = thread {
        setupWindow()

        sciter = Sciter(GLFWNativeWin32.glfwGetWin32Window(window))
        sciter.init(LIBRARY_SCITER_LOCATION)

        sciter.setSize(1920, 1080)
        sciter.setResolution(mc.options.guiScale * 92)

        sciter.loadHtml("<html><body><h1>Hi.</h1> What the fuck is this?</body></html>", null)

        while (!glfwWindowShouldClose(window)) {
            sciter.heartBit()

            // Clear the screen and depth buffer
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
            getWindowSize { width, height ->
                GL11.glLoadIdentity()
                GL11.glViewport(0, 0, width, height)
                GL11.glMatrixMode(GL11.GL_PROJECTION)
                GL11.glLoadIdentity()
                GL11.glOrtho(0.0, 1.0, 1.0, 0.0, 1.0, -1.0)
                sciter.setSize(width, height)
            }
            GL11.glMatrixMode(GL11.GL_MODELVIEW)
            GL11.glColor4f(1.0f, 0.0f, 0.0f, 1.0f)

            GL11.glMatrixMode(GL11.GL_PROJECTION)
            GL11.glPushMatrix()
            sciter.render()
            GL11.glPopMatrix()
            GL13.glActiveTexture(GL13.GL_TEXTURE0)
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
            GL11.glVertex2f(0f, 1f)
            GL11.glTexCoord2f(1.0f, 1.0f)
            GL11.glVertex2f(1f, 1f)
            GL11.glTexCoord2f(1.0f, 0.0f)
            GL11.glVertex2f(1f, 0f)
            GL11.glEnd()
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
            GL11.glDisable(GL11.GL_TEXTURE_2D)

            glfwSwapBuffers(window)

            glfwPollEvents()
        }

        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        glfwTerminate()
        glfwSetErrorCallback(null)!!.free()
    }

    private fun setupWindow() {
        GLFWErrorCallback.createPrint(System.err).set()

        check(glfwInit()) { "Unable to initialize GLFW" }

        glfwDefaultWindowHints()

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

        // Create the window
        window = glfwCreateWindow(mc.window.width, mc.window.height, "Client layer", NULL, NULL)
        if (window == NULL)
            throw RuntimeException("Failed to create the GLFW window")

        val currentMods = AtomicInteger(0)

        glfwSetKeyCallback(window) { window: Long, key: Int, scancode: Int, action: Int, mods: Int ->
            sciter.keyEvent(key, mods, if (action == GLFW_RELEASE) 1 else 0)
            currentMods.set(mods)
        }

        val currentButton = AtomicInteger(0)

        val mousePosX = AtomicInteger(0)
        val mousePosY = AtomicInteger(0)

        glfwSetMouseButtonCallback(window) { window: Long, button: Int, action: Int, mods: Int ->
            if (action == GLFW_RELEASE) {
                currentButton.set(0)
            } else if (action == GLFW_PRESS) {
                currentButton.set(button + 1)
            }

            sciter.mouseButtonEvent(mousePosX.get(), mousePosY.get(), button + 1, action == GLFW_RELEASE, mods)
        }

        glfwSetCharCallback(window) { window: Long, codepoint: Int ->
            if (currentMods.get() and 0x2 == 0)
                sciter.keyEvent(codepoint, currentMods.get(), 2)
        }

        glfwSetCursorPosCallback(window) { window: Long, x: Double, y: Double ->
            sciter.mouseMoved(x.toInt(), y.toInt(), currentButton.get())
            mousePosX.set(x.toInt())
            mousePosY.set(y.toInt())
        }

        getWindowSize { width1, height1 ->
            // Get the resolution of the primary monitor
            val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!

            // Center the window
            glfwSetWindowPos(window, (videoMode.width() - width1) / 2, (videoMode.height() - height1) / 2)
        }

        glfwMakeContextCurrent(window)
        glfwSwapInterval(1)
        glfwShowWindow(window)
        GL.createCapabilities()
    }

    private fun getWindowSize(outputHandler: BiConsumer<Int, Int>) {
        // Get the thread stack and push a new frame
        stackPush().use { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight)
            outputHandler.accept(pWidth[0], pHeight[0])
        }
    }

    fun resize() {
        // sciter.setSize(this.width, this.height)
        // sciter.setResolution(mc.options.guiScale * 92)
    }

}

class SciterScreen(name: String, val pausesGame: Boolean = true) : Screen(LiteralText(name)) {

    private var buttonStates = 0
    private var keyboardModifiers = 0

    // Action handlers
    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        SciterWindow.sciter.mouseMoved(mouseX.toInt(), mouseY.toInt(), this.buttonStates)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        SciterWindow.sciter.mouseButtonEvent(mouseX.toInt(), mouseY.toInt(), button + 1, false, 0)
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        SciterWindow.sciter.mouseButtonEvent(mouseX.toInt(), mouseY.toInt(), button + 1, true, 0)
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        SciterWindow.sciter.mouseMoved(mouseX.toInt(), mouseY.toInt(), this.buttonStates)
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        this.keyboardModifiers = modifiers
        SciterWindow.sciter.keyEvent(scanCode, modifiers, 0)
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        this.keyboardModifiers = modifiers
        SciterWindow.sciter.keyEvent(scanCode, modifiers, 1)
        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun charTyped(chr: Char, keyCode: Int): Boolean {
        SciterWindow.sciter.keyEvent(chr.toInt(), keyboardModifiers, 2)
        return super.charTyped(chr, keyCode)
    }

    override fun isPauseScreen() = false

}
