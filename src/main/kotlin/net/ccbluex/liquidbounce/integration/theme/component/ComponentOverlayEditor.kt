package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import java.awt.Color

class ComponentOverlayEditor : Screen("Component Editor".asText()) {

    var draggingComponent: Component? = null

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        for (component in components) {
            val alignment = component.alignment
            val bounds = alignment.getBounds(200f, 200f)

            // Draw the component

            context.drawBorder(bounds.xMin.toInt(), bounds.yMin.toInt(), 200, 200, Color(0, 0, 0, 100).rgb)
        }

//        super.render(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Check if the mouse is over a component

        for (component in components) {
            if (component.isMouseOver(mouseX, mouseY)) {
                // Start dragging the component
                draggingComponent = component
                chat("Dragging component ${component.name}")
                return true
            }
        }


        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Stop dragging the component
        draggingComponent = null
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {

        super.mouseMoved(mouseX, mouseY)
    }

}

fun Component.isMouseOver(mouseX: Double, mouseY: Double): Boolean {
    return alignment.getBounds(200f, 200f).contains(mouseX.toFloat(), mouseY.toFloat())
}
