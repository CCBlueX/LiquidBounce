package net.ccbluex.liquidbounce.integration.theme.type

import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentFactory
import net.ccbluex.liquidbounce.integration.theme.type.native.NativeDrawableRoute
import net.ccbluex.liquidbounce.integration.theme.wallpaper.Wallpaper

interface Theme {
    val name: String
    val components: List<ComponentFactory>
    val wallpapers: List<Wallpaper>

    fun route(screenType: VirtualScreenType? = null): RouteType
    fun doesAccept(type: VirtualScreenType?): Boolean = doesOverlay(type) || doesSupport(type)
    fun doesSupport(type: VirtualScreenType?): Boolean
    fun doesOverlay(type: VirtualScreenType?): Boolean
    fun canSplash(): Boolean

}

sealed class RouteType(open val type: VirtualScreenType?, open val theme: Theme) {
    data class Native(override val type: VirtualScreenType?, override val theme: Theme, val drawableRoute: NativeDrawableRoute)
        : RouteType(type, theme)
    data class Web(override val type: VirtualScreenType?, override val theme: Theme, val url: String)
        : RouteType(type, theme)
}
