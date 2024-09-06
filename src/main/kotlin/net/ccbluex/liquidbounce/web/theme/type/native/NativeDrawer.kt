package net.ccbluex.liquidbounce.web.theme.type.native

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.ccbluex.liquidbounce.web.theme.type.RouteType
import net.ccbluex.liquidbounce.web.theme.type.native.routes.EmptyDrawableRoute

object NativeDrawer : Listenable {

    var hasDrawn = false
    var activeRoute: NativeDrawableRoute? = null

    @Suppress
    val gameTickHandler = handler<GameTickEvent> {
        hasDrawn = false
    }

    @Suppress("unused")
    val onScreenRender = handler<ScreenRenderEvent> {
        if (!hasDrawn) {
            hasDrawn = true
        }
        activeRoute?.render(it.context, it.partialTicks)
    }

    @Suppress("unused")
    val onOverlayRender = handler<OverlayRenderEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) {
        if (!hasDrawn) {
            hasDrawn = true
        }
        activeRoute?.render(it.context, it.tickDelta)
    }

    fun select(route: NativeDrawableRoute?) {
        activeRoute = route
    }

    @Suppress("unused")
    val onVirtualEvent = handler<VirtualScreenEvent> { event ->
        val route = when (event.action) {
            VirtualScreenEvent.Action.OPEN -> ThemeManager.activeTheme.route(VirtualScreenType.byName(event.screenName))
            VirtualScreenEvent.Action.CLOSE -> ThemeManager.activeTheme.route(null)
        }

        if (route is RouteType.Native) {
            select(route.drawableRoute)
        } else {
            select(null)
        }
    }

}
