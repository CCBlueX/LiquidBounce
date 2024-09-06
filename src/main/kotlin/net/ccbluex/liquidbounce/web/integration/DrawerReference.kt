package net.ccbluex.liquidbounce.web.integration

import net.ccbluex.liquidbounce.web.browser.supports.tab.ITab
import net.ccbluex.liquidbounce.web.theme.type.native.NativeDrawer

sealed class DrawerReference {
    data class Native(val drawer: NativeDrawer) : DrawerReference()
    data class Web(val browser: ITab) : DrawerReference()
}
