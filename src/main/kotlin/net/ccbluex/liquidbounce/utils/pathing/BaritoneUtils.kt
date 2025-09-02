package net.ccbluex.liquidbounce.utils.pathing

import baritone.api.BaritoneAPI

object BaritoneUtils {
    var IS_AVAILABLE: Boolean = false

    val prefix: String?
        get() {
            if (IS_AVAILABLE) {
                return BaritoneAPI.getSettings().prefix.value
            }

            return ""
        }
}
