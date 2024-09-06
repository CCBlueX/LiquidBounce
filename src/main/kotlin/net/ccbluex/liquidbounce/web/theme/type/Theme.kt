package net.ccbluex.liquidbounce.web.theme.type

import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.theme.component.Component
import net.ccbluex.liquidbounce.web.theme.type.native.NativeDrawableRoute

interface Theme {
    val name: String
    val components: List<Component>

    fun route(screenType: VirtualScreenType? = null): RouteType
    fun doesAccept(type: VirtualScreenType?): Boolean
    fun doesSupport(type: VirtualScreenType?): Boolean
    fun doesOverlay(type: VirtualScreenType?): Boolean

}

sealed class RouteType(open val type: VirtualScreenType?, open val theme: Theme) {
    data class Native(override val type: VirtualScreenType?, override val theme: Theme, val drawableRoute: NativeDrawableRoute)
        : RouteType(type, theme)
    data class Web(override val type: VirtualScreenType?, override val theme: Theme, val url: String)
        : RouteType(type, theme)
}
