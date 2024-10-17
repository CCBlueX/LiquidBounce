package net.ccbluex.liquidbounce.ui.client.clickgui.elements.rice

import net.ccbluex.liquidbounce.value.Value

abstract class ValueElement {

    abstract var previousValue: ValueElement?
    abstract var startX: Float
    abstract var startY: Float
    abstract var width: Float
    abstract var height: Float
    abstract var margin: Float
    abstract fun drawElement()
    abstract fun handleClick(mouseX: Float, mouseY: Float, button: Int)
}