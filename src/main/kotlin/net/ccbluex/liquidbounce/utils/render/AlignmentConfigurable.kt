package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.utils.client.mc

class AlignmentConfigurable(
    horizontalAlignment: ScreenAxisX,
    horizontalPadding: Int,
    verticalAlignment: ScreenAxisY,
    verticalPadding: Int,
) : Configurable("Alignment") {
    private val horizontalAlignment by enumChoice("HorizontalAlignment", horizontalAlignment, ScreenAxisX.values())
    private val horizontalPadding by int("HorizontalPadding", horizontalPadding, 1..256)
    private val verticalAlignment by enumChoice("VerticalAlignment", verticalAlignment, ScreenAxisY.values())
    private val verticalPadding by int("VerticalPadding", verticalPadding, 1..256)

    fun getBounds(
        width: Float,
        height: Float,
    ): BoundingBox2f {
        val screenWidth = mc.window.scaledWidth.toFloat()
        val screenHeight = mc.window.scaledHeight.toFloat()

        val x =
            when (horizontalAlignment) {
                ScreenAxisX.LEFT -> horizontalPadding.toFloat()
                ScreenAxisX.RIGHT -> screenWidth - width - horizontalPadding.toFloat()
            }

        val y =
            when (verticalAlignment) {
                ScreenAxisY.TOP -> verticalPadding.toFloat()
                ScreenAxisY.BOTTOM -> screenHeight - height - verticalPadding.toFloat()
            }

        return BoundingBox2f(x, y, x + width, y + height)
    }

    enum class ScreenAxisX(override val choiceName: String) : NamedChoice {
        LEFT("Left"),
        RIGHT("Right"),
    }

    enum class ScreenAxisY(override val choiceName: String) : NamedChoice {
        TOP("Top"),
        BOTTOM("Bottom"),
    }
}
