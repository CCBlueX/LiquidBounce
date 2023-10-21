package net.ccbluex.liquidbounce.web

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.screen.WebScreen
import net.janrupf.ujr.api.UltralightKeyEventBuilder
import net.janrupf.ujr.api.UltralightMouseEventBuilder
import net.janrupf.ujr.api.UltralightScrollEventBuilder
import net.janrupf.ujr.api.event.UlKeyEvent
import net.janrupf.ujr.api.event.UlKeyEventType
import net.janrupf.ujr.api.event.UlMouseButton
import net.janrupf.ujr.api.event.UlScrollEventType
import net.janrupf.ujr.example.glfw.web.KeyboardTranslator
import net.janrupf.ujr.example.glfw.web.WebController
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.TitleScreen
import org.lwjgl.glfw.GLFW


object GameWebView : Listenable {

    var webController = WebController()

    init {
        // Immediately create a new splash
        LayerDistribution().newSplash()
    }

    val screenHandler = handler<ScreenEvent> {
        if (it.screen is TitleScreen) {
            it.cancelEvent()

            val window = LayerDistribution().newScreen(it.screen)
            mc.setScreen(WebScreen(window))
        }
    }

    val windowResizeWHandler = handler<WindowResizeEvent> {

    }

    val windowFocusHandler = handler<WindowFocusEvent> {

    }

    var mouseX = 0.0
    var mouseY = 0.0

    val mouseButtonHandler = handler<MouseButtonEvent> {
        val button = it.button
        val action = it.action
        val ulButton: UlMouseButton

        ulButton = when (button) {
            GLFW.GLFW_MOUSE_BUTTON_LEFT -> UlMouseButton.LEFT
            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> UlMouseButton.RIGHT
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> UlMouseButton.MIDDLE
            else -> return@handler
        }

        val builder: UltralightMouseEventBuilder

        builder = if (action == GLFW.GLFW_PRESS) {
            UltralightMouseEventBuilder.down(ulButton)
        } else if (action == GLFW.GLFW_RELEASE) {
            UltralightMouseEventBuilder.up(ulButton)
        } else {
            return@handler
        }

        val event = builder
            .x(this.mouseX.toInt())
            .y(this.mouseY.toInt())
            .build()

        webController.windows.forEach {
            it.view.fireMouseEvent(event)
        }
    }

    val mouseScrollHandler = handler<MouseScrollEvent> { event ->
        webController.windows.forEach {
            it.view.fireScrollEvent(
                UltralightScrollEventBuilder(UlScrollEventType.BY_PIXEL)
                    .deltaY((event.horizontal * 50).toInt())
                    .deltaY((event.vertical * 50).toInt())
                    .build()
            );
        }
    }

    val mouseCursorHandler = handler<MouseCursorEvent> {
        this.mouseX = it.x
        this.mouseY = it.y

        webController.windows.forEach {
            it.view.fireMouseEvent(
                UltralightMouseEventBuilder.moved()
                    .x(this.mouseX.toInt())
                    .y(this.mouseY.toInt())
                    .build()
            )
        }
    }

    val keyboardKeyHandler = handler<KeyboardKeyEvent> { event ->
        val builder: UltralightKeyEventBuilder
        val action = event.action
        val key = event.keyCode
        val mods = event.mods
        val scancode = event.scancode

        builder = when (action) {
            GLFW.GLFW_PRESS -> UltralightKeyEventBuilder.rawDown()
            GLFW.GLFW_RELEASE -> UltralightKeyEventBuilder.up()
            else -> {
                return@handler
            }
        }

        builder.nativeKeyCode(scancode)
            .virtualKeyCode(KeyboardTranslator.glfwKeyToUltralight(key))
            .keyIdentifier(UlKeyEvent.keyIdentifierFromVirtualKeyCode(builder.virtualKeyCode))
            .modifiers(KeyboardTranslator.glfwModifiersToUltralight(mods))

        val event = builder.build()
        webController.windows.forEach {
            it.view.fireKeyEvent(event)
        }

        if (builder.type == UlKeyEventType.RAW_DOWN && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_TAB)) {
            webController.windows.forEach {
                it.view.fireKeyEvent(UltralightKeyEventBuilder.character()
                    .unmodifiedText(if (key == GLFW.GLFW_KEY_ENTER) "\n" else "\t")
                    .text(if (key == GLFW.GLFW_KEY_ENTER) "\n" else "\t"))
            }
        }
    }

    val keyboardCharHandler = handler<KeyboardCharEvent> {

    }

    fun renderTick() {
        webController.update()
        webController.render()
    }

    fun renderInGame(context: DrawContext?) = webController.renderToFramebuffer(context, Layer.IN_GAME_LAYER)
    fun renderOverlay(context: DrawContext?) = webController.renderToFramebuffer(context, Layer.SPLASH_LAYER)
    fun renderScreen(context: DrawContext?, webScreen: WebScreen) = webController.renderToFramebuffer(context, webScreen.window)



}
