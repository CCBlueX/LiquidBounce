package net.ccbluex.liquidbounce.ui.client.clickgui.elements.rice

import net.ccbluex.liquidbounce.ui.client.clickgui.RiceGui
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.value.BoolValue
import net.vitox.particle.util.RenderUtils
import java.awt.Color

class BoolValueElement(
    var boolValue: BoolValue,
    override var startX: Float,
    override var startY: Float = 0f,
    override var previousValue: ValueElement? = null
) : ValueElement() {

    override var margin: Float = 5f
    override var height: Float = Fonts.font35.fontHeight.toFloat() + margin
    override var width: Float = Fonts.font35.getStringWidth(boolValue.name).toFloat()

    private var hitboxX = 0f..0f
    private var hitboxY = 0f..0f

    init {
        if (previousValue != null) {
            startY = previousValue!!.startY + previousValue!!.height
        }
        this.hitboxX = startX .. (startX + width + 14f)
        this.hitboxY = startY .. (startY + height-margin)
    }

    override fun drawElement() {
        updateElement()
        Fonts.font35.drawString(
            boolValue.name,
            startX,
            startY,
            Color.WHITE.rgb
        )
        var circleY = startY + Fonts.font35.fontHeight / 2f - 1.5f
        var circleX = startX + width + 10f
        if (boolValue.isActive()) {
            RenderUtils.drawCircle(circleX, circleY, 4f, RiceGui.highlightColorAlpha.rgb)
            RenderUtils.drawCircle(
                circleX,
                circleY,
                2f,
                 RiceGui.highlightColor
            )
        }else{
            RenderUtils.drawCircle(circleX, circleY, 3f, RiceGui.referenceColor)
        }
    }

    override fun handleClick(mouseX: Float, mouseY: Float, button: Int) {
        if (button == 0 && hitboxX.contains(mouseX) && hitboxY.contains(mouseY)) {
            boolValue.toggle()
        }
    }
    private fun updateElement(){

        if (previousValue != null) {
            this.startY = previousValue!!.startY + previousValue!!.height
        }
        this.hitboxX = startX .. (startX + width+14f)
        this.hitboxY = startY .. (startY + height-margin)
    }
}