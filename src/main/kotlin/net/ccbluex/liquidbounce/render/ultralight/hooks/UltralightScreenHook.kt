package net.ccbluex.liquidbounce.render.ultralight.hooks

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.screen.EmptyScreen
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.render.ultralight.js.bindings.QueuedScreen
import net.ccbluex.liquidbounce.render.ultralight.js.bindings.UltralightJsUi
import net.ccbluex.liquidbounce.render.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.extensions.asText

object UltralightScreenHook : Listenable {

    var nextScreen: QueuedScreen? = null

    /**
     * Check queue every game tick
     */
    val tickHandler = handler<GameTickEvent> { event ->
        val (screen, parent) = nextScreen ?: return@handler
        // Making it null before opening is very important to make sure it doesn't repeat any further
        nextScreen = null

        // Open screen with parent
        screen.open(parent)
    }

    /**
     *
     */
    val screenHandler = handler<ScreenEvent> { event ->
        val name = UltralightJsUi.get(event.screen)?.name ?: return@handler
        val page = ThemeManager.page(name) ?: return@handler

        val view = UltralightEngine.newScreenView(event.screen ?: EmptyScreen("ultralight".asText()))
        view.loadPage(page)
    }

}
