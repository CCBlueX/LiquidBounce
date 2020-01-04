package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

/**
 * CustomHUD Notification element
 */
@ElementInfo(name = "Notifications")
class Notifications(x: Double = 0.0, y: Double = 30.0, scale: Float = 1F,
                    side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)) : Element(x, y, scale, side) {

    /**
     * Example notification for CustomHUD designer
     */
    private val exampleNotification = Notification("Example Notification")

    /**
     * Draw element
     */
    override fun drawElement(): Border? {
        if (LiquidBounce.CLIENT.hud.notifications.size > 0)
            LiquidBounce.CLIENT.hud.notifications[0].drawNotification()

        if (mc.currentScreen is GuiHudDesigner) {
            if (!LiquidBounce.CLIENT.hud.notifications.contains(exampleNotification))
                LiquidBounce.CLIENT.hud.addNotification(exampleNotification)

            exampleNotification.fadeState = Notification.FadeState.STAY
            exampleNotification.x = exampleNotification.textLength + 8F

            return Border(-95F, -20F, 0F, 0F)
        }

        return null
    }

}

class Notification(private val message: String) {

    var x = 0F
    var textLength = 0

    private var stay = 0F
    var fadeState = FadeState.IN

    /**
     * Fade state for animation
     */
    enum class FadeState { IN, STAY, OUT, END }

    /**
     * Draw notification
     */
    fun drawNotification() {
        // Get text length
        textLength = Fonts.font35.getStringWidth(message)

        // Draw notification
        RenderUtils.drawRect(-x + 8 + textLength, 0F, -x, -20F, Color.BLACK.rgb)
        RenderUtils.drawRect(-x, 0F, -x - 5, -20F, Color(0, 160, 255).rgb)
        Fonts.font35.drawString(message, -x + 4, -14F, Int.MAX_VALUE)
        GlStateManager.resetColor()

        // Animation
        val delta = RenderUtils.deltaTime

        when (fadeState) {
            FadeState.IN -> {
                if (x < textLength + 8)
                    x += 0.2F * delta else fadeState = FadeState.STAY

                stay = 60F

                if (x > textLength + 8F)
                    x = textLength + 8F
            }

            FadeState.STAY -> if (stay > 0)
                stay -= 0.2F * delta
            else
                fadeState = FadeState.OUT

            FadeState.OUT -> if (x > 0)
                x -= 0.2F * delta
            else
                fadeState = FadeState.END

            FadeState.END -> LiquidBounce.CLIENT.hud.removeNotification(this)
        }
    }

}

