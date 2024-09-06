package net.ccbluex.liquidbounce.web.theme.type.native

import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.theme.component.Component
import net.ccbluex.liquidbounce.web.theme.type.RouteType
import net.ccbluex.liquidbounce.web.theme.type.native.components.minimap.MinimapComponent
import net.ccbluex.liquidbounce.web.theme.type.Theme
import net.ccbluex.liquidbounce.web.theme.type.native.routes.EmptyDrawableRoute
import net.ccbluex.liquidbounce.web.theme.type.native.routes.TitleDrawableRoute

/**
 * A Theme based on native GL rendering.
 */
object NativeTheme : Theme {

    override val name = "Native"
    override val components: List<Component>
        get() = listOf(MinimapComponent)

    private val routes = mutableMapOf(
        null to EmptyDrawableRoute(),
//        VirtualScreenType.TITLE to TitleDrawableRoute(),
    )

    private val overlayRoutes = mutableMapOf(
        null to EmptyDrawableRoute(),
        VirtualScreenType.TITLE to TitleDrawableRoute(),
    )

    override fun route(screenType: VirtualScreenType?) =
        RouteType.Native(screenType, this, routes[screenType] ?: overlayRoutes[screenType] ?: EmptyDrawableRoute())

    override fun doesSupport(type: VirtualScreenType?) = routes.containsKey(type)
    override fun doesOverlay(type: VirtualScreenType?) = overlayRoutes.containsKey(type)

}
