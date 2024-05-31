/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements


import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.AnimationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import org.lwjgl.opengl.GL11.glColor4f
import java.awt.Color

/**
 * CustomHUD Notification element
 */
@ElementInfo(name = "Notifications", single = true)
class Notifications(x: Double = 0.0, y: Double = 30.0, scale: Float = 1F,
                    side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.DOWN)) : Element(x, y, scale, side) {

    /**
     * Example notification for CustomHUD designer
     */
    private val exampleNotification = Notification("Example Notification")

    /**
     * Draw element
     */
    override fun drawElement(): Border? {
        HUD.notifications.firstOrNull()?.drawNotification()

        if (mc.currentScreen is GuiHudDesigner) {
            if (exampleNotification !in HUD.notifications)
                addNotification(exampleNotification)

            exampleNotification.fadeState = Notification.FadeState.STAY
            exampleNotification.x = exampleNotification.textLength + 8F

            return Border(-95F, -20F, 0F, 0F)
        }

        return null
    }

}

// Default delay set to 60F
class Notification(private val message: String, private val delay: Float = 60F) {
    var x = 0F
    var textLength = 0

    private var stay = 0F
    private var fadeStep = 0F
    var fadeState = FadeState.IN

    /**
     * Fade state for animation
     */
    enum class FadeState { IN, STAY, OUT, END }

    init {
        textLength = Fonts.font35.getStringWidth(message)
    }

    /**
     * Draw notification
     */
    fun drawNotification() {
        // Draw notification
        drawRect(-x + 8 + textLength, 0F, -x, -20F, Color.BLACK.rgb)
        drawRect(-x, 0F, -x - 5, -20F, Color(0, 160, 255).rgb)
        Fonts.font35.drawString(message, -x + 4, -14F, Int.MAX_VALUE)
        glColor4f(1f, 1f, 1f, 1f)

        // Animation
        val delta = deltaTime
        val width = textLength + 8F

        when (fadeState) {
            FadeState.IN -> {
                if (x < width) {
                    x = AnimationUtils.easeOut(fadeStep, width) * width
                    fadeStep += delta / 4F
                }
                if (x >= width) {
                    fadeState = FadeState.STAY
                    x = width
                    fadeStep = width
                }

                stay = delay
            }

            FadeState.STAY -> if (stay > 0)
                stay -= delta
            else
                fadeState = FadeState.OUT

            FadeState.OUT -> if (x > 0) {
                x = AnimationUtils.easeOut(fadeStep, width) * width
                fadeStep -= delta / 4F
            } else
                fadeState = FadeState.END

            FadeState.END -> HUD.removeNotification(this)
        }
    }
}

