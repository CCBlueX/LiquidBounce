package net.ccbluex.liquidbounce.integration.theme.type.native.routes

import net.ccbluex.liquidbounce.integration.IntegrationHandler
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.ThemeManager.activeComponents
import net.ccbluex.liquidbounce.integration.theme.type.native.NativeDrawableRoute
import net.ccbluex.liquidbounce.integration.theme.type.native.components.NativeComponent
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.minecraft.client.gui.DrawContext

const val BORDER_GAP = 4

class HudDrawableRoute : NativeDrawableRoute() {

    private var draggingComponent: NativeComponent? = null

    private var lastX = 0.0
    private var lastY = 0.0

    override fun render(context: DrawContext, delta: Float) {
        val editorIsOpen = IntegrationHandler.route.type == VirtualScreenType.EDITOR

        // Reset dragging component if editor is closed
        if (!editorIsOpen && draggingComponent != null) {
            draggingComponent = null
        }

        // Draw native components
        activeComponents
            .filterIsInstance<NativeComponent>()
            .forEach { component ->
                val (width, height) = component.size()
                val box = component.alignment.getBounds(width.toFloat(), height.toFloat())

                context.matrices.push()
                context.matrices.translate(box.xMin.toDouble(), box.yMin.toDouble(), 0.0)
                component.render(context, delta)
                context.matrices.pop()

                if (editorIsOpen) {
                    context.drawBorder(
                        box.xMin.toInt() - BORDER_GAP,
                        box.yMin.toInt() - BORDER_GAP,
                        (box.xMax - box.xMin).toInt() + BORDER_GAP,
                        (box.yMax - box.yMin).toInt() + BORDER_GAP,
                        Color4b.WHITE.toRGBA()
                    )
                }
            }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        activeComponents
            .filterIsInstance<NativeComponent>()
            .forEach { component ->
                val (width, height) = component.size()

                if (component.alignment.contains(mouseX.toFloat(), mouseY.toFloat(), width.toFloat(), height.toFloat())) {
                    draggingComponent = component
                    return true
                }
            }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        draggingComponent = null
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val diffX = mouseX - this.lastX
        val diffY = mouseY - this.lastY
        // todo: fix awful dragging
        draggingComponent?.alignment?.move(diffX.toInt(), diffY.toInt())
        this.lastX = mouseX
        this.lastY = mouseY

        super.mouseMoved(mouseX, mouseY)
    }

}
