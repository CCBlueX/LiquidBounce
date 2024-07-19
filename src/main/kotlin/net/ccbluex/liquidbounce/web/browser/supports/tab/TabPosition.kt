package net.ccbluex.liquidbounce.web.browser.supports.tab

/**
 * Tab position
 */
data class TabPosition(
    val x: () -> Int = { 40 },
    val y: () -> Int = { 200 },
    val width: () -> Int = { 400 },
    val height: () -> Int = { 600 }
) {

    fun x(x: Double): Double {
        return x - this.x()
    }

    fun y(y: Double): Double {
        return y - this.y()
    }

    companion object {
        val DEFAULT = TabPosition()
    }
}
