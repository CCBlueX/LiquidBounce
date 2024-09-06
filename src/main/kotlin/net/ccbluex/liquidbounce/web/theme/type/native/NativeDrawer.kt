package net.ccbluex.liquidbounce.web.theme.type.native

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.ccbluex.liquidbounce.web.theme.type.RouteType

class NativeDrawer(var route: NativeDrawableRoute? = null) : Listenable, AutoCloseable {

    private var hasDrawn = false

    @Suppress
    val gameTickHandler = handler<GameTickEvent> {
        hasDrawn = false
    }

    @Suppress("unused")
    val onScreenRender = handler<ScreenRenderEvent> {
        if (!hasDrawn) {
            hasDrawn = true
        }
        route?.render(it.context, it.partialTicks)
    }

    @Suppress("unused")
    val onOverlayRender = handler<OverlayRenderEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) {
        if (!hasDrawn) {
            hasDrawn = true
        }
        route?.render(it.context, it.tickDelta)
    }

    fun select(route: NativeDrawableRoute?) {
        this.route = route
    }

    override fun close() {
        EventManager.unregisterEventHandler(this)
    }

}
