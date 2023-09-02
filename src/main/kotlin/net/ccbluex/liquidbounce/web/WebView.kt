package net.ccbluex.liquidbounce.web

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.ScreenRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.janrupf.ujr.example.glfw.web.WebController
import net.minecraft.client.gui.DrawContext


object WebView : Listenable {

    var webController = WebController()
    private var hasSetup = false

    fun render(context: DrawContext?, partialTicks: Float) {
        if (!hasSetup) {
            val window = webController.createWindow({ mc.window.framebufferWidth }) { mc.window.framebufferHeight }
            window.view.loadURL("https://duckduckgo.com")
            hasSetup = true
        }

        webController.update()
        webController.render(context)
    }

}
