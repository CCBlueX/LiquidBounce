package net.ccbluex.liquidbounce.integration

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.VirtualScreenEvent
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.refreshRate
import net.ccbluex.liquidbounce.integration.browser.BrowserManager
import net.ccbluex.liquidbounce.integration.browser.supports.tab.ITab
import net.ccbluex.liquidbounce.integration.theme.component.ComponentOverlayEditor
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.integration.theme.type.native.NativeDrawer
import net.minecraft.client.gui.screen.ChatScreen

sealed class DrawerReference : AutoCloseable {
    data class Native(
        val drawer: NativeDrawer,
        var route: RouteType.Native,
    ) : DrawerReference()
    data class Web(
        val browser: ITab,
        var route: RouteType.Web
    ) : DrawerReference()

    companion object {

        private val takesInputHandler: () -> Boolean
            get() = { mc.currentScreen != null && mc.currentScreen !is ChatScreen }

        private val takesInputOnEditor: () -> Boolean
            get() = { mc.currentScreen is ComponentOverlayEditor  }

        fun newComponentRef(route: RouteType) =
            when (route) {
                is RouteType.Web -> Web(
                    BrowserManager.browser?.createTab(
                        route.url,
                        frameRate = 60,
                        takesInput = takesInputOnEditor
                    ) ?: error("Browser is not initialized"),
                    route
                )

                is RouteType.Native -> Native(NativeDrawer(route.drawableRoute, takesInputOnEditor), route)
            }

        fun newInputRef(route: RouteType) =
            when (route) {
                is RouteType.Web -> Web(
                    BrowserManager.browser?.createTab(
                        route.url,
                        frameRate = refreshRate,
                        takesInput = takesInputHandler
                    ) ?: error("Browser is not initialized"),
                    route
                )

                is RouteType.Native -> Native(NativeDrawer(route.drawableRoute, takesInputHandler), route)
            }

    }

    fun update(route: RouteType) {
        when (route) {
            is RouteType.Web -> when (this) {
                is Web -> {
                    // If the reference is already on the same route, we don't need to update it
                    if (this.route.theme == route.theme && this.route.type == route.type) {
                        return
                    }

                    if (this.route.theme != route.theme) {
                        browser.loadUrl(route.url)
                        this.route = route
                        return
                    }

                    when (val type = route.type) {
                        // If the new route type is null, we should trigger a close event,
                        // but if the current route type is null as well, we simply ignore it
                        null -> EventManager.callEvent(VirtualScreenEvent(
                            // Use the current route name
                            this.route.type?.routeName ?: return,
                            VirtualScreenEvent.Action.CLOSE
                        ))
                        // If the new route type is not null, we should trigger an open event
                        else -> EventManager.callEvent(VirtualScreenEvent(
                            type.routeName,
                            VirtualScreenEvent.Action.OPEN
                        ))
                    }
                    this.route = route
                }
                is Native -> error("Unable to update tab, drawer reference is not a web tab")
            }

            is RouteType.Native -> when (this) {
                is Native -> {
                    drawer.select(route.drawableRoute)
                    this.route = route
                }
                is Web -> error("Unable to update tab, drawer reference is not a native tab")
            }
        }
    }

    fun sync() {
        when (this) {
            is Web -> browser.loadUrl(route.url)
            is Native -> drawer.select(route.drawableRoute)
        }
    }

    fun isCompatible(route: RouteType): Boolean {
        return when (this) {
            is Web -> route is RouteType.Web
            is Native -> route is RouteType.Native
        }
    }

    fun matches(route: RouteType): Boolean {
        return when (this) {
            is Web -> route is RouteType.Web && route.url == this.route.url
            is Native -> route is RouteType.Native && route.drawableRoute == this.route.drawableRoute
        }
    }

    override fun close() = when (this) {
        is Web -> browser.closeTab()
        is Native -> drawer.close()
    }

}
