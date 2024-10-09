package net.ccbluex.liquidbounce.integration.theme.type.native

import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentFactory
import net.ccbluex.liquidbounce.integration.theme.component.ComponentFactory.NativeComponentFactory
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.integration.theme.type.native.components.minimap.MinimapComponent
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.type.native.components.ArrayListNativeComponent
import net.ccbluex.liquidbounce.integration.theme.type.native.routes.EmptyDrawableRoute
import net.ccbluex.liquidbounce.integration.theme.type.native.routes.HudDrawableRoute
import net.ccbluex.liquidbounce.integration.theme.type.native.routes.TitleDrawableRoute
import net.ccbluex.liquidbounce.integration.theme.wallpaper.Wallpaper

/**
 * A Theme based on native GL rendering.
 */
object NativeTheme : Theme {

    override val name = "Native"
    override val components: List<ComponentFactory>
        get() = listOf(
            NativeComponentFactory { ArrayListNativeComponent(this) },
            NativeComponentFactory { MinimapComponent(this) }
        )
    override val wallpapers: List<Wallpaper> = emptyList()

    private val routes = mutableMapOf(
        VirtualScreenType.TITLE to TitleDrawableRoute()
    )

    private val overlayRoutes = mutableMapOf(
        VirtualScreenType.HUD to HudDrawableRoute()
    )

    override fun route(screenType: VirtualScreenType?) =
        RouteType.Native(
            screenType,
            this,
            routes[screenType] ?: overlayRoutes[screenType] ?: EmptyDrawableRoute()
        )

    override fun doesSupport(type: VirtualScreenType?) = routes.containsKey(type)
    override fun doesOverlay(type: VirtualScreenType?) = overlayRoutes.containsKey(type)
    override fun canSplash() = false

}
